/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.microsphere.spring.cloud.gateway.mvc.filter;

import io.microsphere.annotation.Nonnull;
import io.microsphere.logging.Logger;
import io.microsphere.spring.boot.context.config.BindableConfigurationBeanBinder;
import io.microsphere.spring.context.config.ConfigurationBeanBinder;
import io.microsphere.spring.web.metadata.WebEndpointMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static io.microsphere.collection.ListUtils.first;
import static io.microsphere.collection.ListUtils.newArrayList;
import static io.microsphere.collection.PropertiesUtils.flatProperties;
import static io.microsphere.constants.PathConstants.SLASH_CHAR;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.spring.cloud.client.service.util.ServiceInstanceUtils.getWebEndpointMappings;
import static io.microsphere.spring.cloud.gateway.mvc.filter.WebEndpointMappingHandlerFilterFunction.Config.DEFAULT_CONFIG;
import static io.microsphere.spring.web.metadata.WebEndpointMapping.ID_HEADER_NAME;
import static io.microsphere.util.ArrayUtils.isNotEmpty;
import static io.microsphere.util.StringUtils.isBlank;
import static io.microsphere.util.StringUtils.substringAfter;
import static java.lang.String.valueOf;
import static java.net.URI.create;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Stream.of;
import static org.springframework.cloud.gateway.server.mvc.common.MvcUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.server.mvc.common.MvcUtils.GATEWAY_ROUTE_ID_ATTR;
import static org.springframework.cloud.gateway.server.mvc.common.MvcUtils.getAttribute;
import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.commaDelimitedListToSet;
import static org.springframework.web.servlet.function.ServerRequest.from;
import static org.springframework.web.servlet.mvc.method.RequestMappingInfo.paths;

/**
 * The Before-Filter of {@link WebEndpointMapping}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see Function
 * @see WebEndpointMapping
 * @since 1.0.0
 */
public class WebEndpointMappingHandlerFilterFunction implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private static final Logger logger = getLogger(WebEndpointMappingHandlerFilterFunction.class);

    /**
     * The all services for mapping
     */
    public static final String ALL_SERVICES = "all";

    /**
     * The key of {@link Config} under {@link RouteProperties#getMetadata() Routes' Metadata}
     */
    public static final String METADATA_CONFIG_KEY = "web-endpoint";

    /**
     * The URI template variable name for application name
     */
    public static final String APPLICATION_NAME_URI_TEMPLATE_VARIABLE_NAME = "application";

    static final String NEW_PATH_ATTRIBUTE_NAME = "msg-new-path";

    private final DiscoveryClient discoveryClient;


    private ApplicationContext context;

    private RouteProperties routeProperties;

    private Collection<RequestMappingContext> requestMappingContexts = new LinkedList<>();

    private Config config;

    public WebEndpointMappingHandlerFilterFunction(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        Map<String, String> uriTemplateVariables = request.pathVariables();
        String applicationName = uriTemplateVariables.get(APPLICATION_NAME_URI_TEMPLATE_VARIABLE_NAME);
        request.attributes().put(GATEWAY_REQUEST_URL_ATTR, request.uri());

        if (isBlank(applicationName)) {
            logger.trace("No application name was found by the request URL['{}'] with uriTemplateVariables : {}", request.path(), uriTemplateVariables);
            return next.handle(request);
        }

        RequestMappingContext requestMappingContext = getMatchingRequestMappingContext(applicationName, request);

        if (requestMappingContext == null) {
            return next.handle(request);
        }

        HandlerFilterFunction<ServerResponse, ServerResponse> lbHandlerFunctionDefinition = lb(applicationName);
        Map<String, Object> attributes = request.attributes();
        String newPath = (String) attributes.remove(NEW_PATH_ATTRIBUTE_NAME);
        int id = requestMappingContext.id;
        ServerRequest newRequest = from(request)
                .uri(create(newPath))
                .header(ID_HEADER_NAME, valueOf(id))
                .build();

        request.attributes().put(GATEWAY_REQUEST_URL_ATTR, newRequest.uri());

        return lbHandlerFunctionDefinition.filter(newRequest, next);
    }

    public void setRouteProperties(RouteProperties routeProperties) {
        this.routeProperties = routeProperties;
    }

    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    public void refresh(RouteProperties routeProperties, ApplicationContext context) {
        if (routeProperties == null) {
            return;
        }

        if (routeProperties != this.routeProperties) {
            return;
        }

        if (context != this.context) {
            return;
        }

        Config config = createConfig(routeProperties.getMetadata());

        Map<WebEndpointMapping, RequestMappingContext> mappedContexts = new HashMap<>();
        getSubscribedServices(routeProperties.getUri(), config)
                .stream()
                .map(discoveryClient::getInstances)
                // TODO support ZonePreferenceFilter
                .flatMap(List::stream)
                .forEach(serviceInstance -> {
                    getWebEndpointMappings(serviceInstance)
                            .stream()
                            .forEach(webEndpointMapping -> {
                                mappedContexts.computeIfAbsent(webEndpointMapping, RequestMappingContext::new);
                            });
                });

        Collection<RequestMappingContext> requestMappingContexts = mappedContexts.values();

        synchronized (routeProperties) {
            this.routeProperties = routeProperties;
            this.config = config;
            this.requestMappingContexts = requestMappingContexts;
            logger.trace("The route properties, configs and request mapping contexts were refreshed!");
        }
    }

    private RequestMappingContext getMatchingRequestMappingContext(String applicationName, ServerRequest request) {
        String routeId = getAttribute(request, GATEWAY_ROUTE_ID_ATTR);
        return getMatchingRequestMappingContext(applicationName, routeId, request);
    }

    List<String> getSubscribedServices(URI uri, Config config) {
        Set<String> excludedServices = config.exclude.getServices();
        String host = uri.getHost();
        final List<String> services = newArrayList();
        if (ALL_SERVICES.equals(host)) {
            services.addAll(discoveryClient.getServices());
        } else {
            services.addAll(commaDelimitedListToSet(host));
        }
        services.removeAll(excludedServices);
        return services;
    }

    RequestMappingContext getMatchingRequestMappingContext(String applicationName, String routeId, ServerRequest request) {
        if (isExcludedRequest(request)) {
            // The request is excluded
            logger.trace("The request is excluded");
            return null;
        }

        Collection<RequestMappingContext> requestMappingContexts = this.requestMappingContexts;

        if (isEmpty(requestMappingContexts)) {
            // No RequestMappingContext found
            logger.trace("No RequestMappingContext was not found by route id['{}'] : {}", routeId, requestMappingContexts);
            return null;
        }

        String path = request.path();

        String newPath = substringAfter(path, SLASH_CHAR + applicationName);
        request.attributes().put(NEW_PATH_ATTRIBUTE_NAME, newPath);

        HttpServletRequest newServletRequest = new HttpServletRequestWrapper(request.servletRequest()) {

            @Override
            public String getRequestURI() {
                return newPath;
            }
        };

        List<RequestMappingContext> matchesRequestMappings = newArrayList(requestMappingContexts.size());
        for (RequestMappingContext requestMappingContext : requestMappingContexts) {
            if (matchesRequestMapping(requestMappingContext, newServletRequest)) {
                // matches the request mappings
                matchesRequestMappings.add(requestMappingContext);
            }
        }
        matchesRequestMappings.sort((v1, v2) -> v1.compareTo(v2, newServletRequest));

        return first(matchesRequestMappings);
    }

    private boolean matchesRequestMapping(RequestMappingContext requestMappingContext, HttpServletRequest servletRequest) {
        RequestMappingInfo requestMappingInfo = requestMappingContext.requestMappingInfo;
        return requestMappingInfo.getMatchingCondition(servletRequest) != null;
    }

    private boolean isExcludedRequest(ServerRequest request) {
        Config config = getConfig();
        return config.isExcludedRequest(request);
    }

    @Nonnull
    private Config getConfig() {
        Config config = this.config;
        if (config == null) {
            logger.trace("The routed config was not initialized");
            config = DEFAULT_CONFIG;
            this.config = config;
        }
        return config;
    }

    static Config createConfig(Map<String, Object> metadata) {
        Config config = new Config();
        Map<String, Object> properties = (Map) metadata.getOrDefault(METADATA_CONFIG_KEY, emptyMap());
        Map<String, Object> flatProperties = flatProperties(properties);
        ConfigurationBeanBinder beanBinder = new BindableConfigurationBeanBinder();
        beanBinder.bind(flatProperties, true, true, config);
        config.init();
        logger.trace("The routed config was initialized from the properties : {}", flatProperties);
        return config;
    }

    static class RequestMappingContext {

        private final RequestMappingInfo requestMappingInfo;

        private int id;

        private List<ServiceInstance> serviceInstances = new LinkedList<>();

        RequestMappingContext(WebEndpointMapping webEndpointMapping) {
            this.requestMappingInfo = buildRequestMappingInfo(webEndpointMapping);
            this.id = webEndpointMapping.getId();
        }

        void addServiceInstance(ServiceInstance serviceInstance) {
            this.serviceInstances.add(serviceInstance);
        }

        public int compareTo(RequestMappingContext other, HttpServletRequest request) {
            return this.requestMappingInfo.compareTo(other.requestMappingInfo, request);
        }
    }

    static class Config {

        static final Config DEFAULT_CONFIG = new Config();

        Exclude exclude = new Exclude();

        RequestMappingInfo excludeRequestMappingInfo;

        public void setExclude(Exclude exclude) {
            this.exclude = exclude;
        }

        public void init() {
            Exclude exclude = this.exclude;
            String[] patterns = exclude.patterns;
            if (isNotEmpty(patterns)) {
                this.excludeRequestMappingInfo = paths(patterns).methods(exclude.methods).build();
            }
        }

        boolean isExcludedRequest(ServerRequest request) {
            return excludeRequestMappingInfo == null ? false :
                    excludeRequestMappingInfo.getMatchingCondition(request.servletRequest()) != null;
        }

        static class Exclude {

            Set<String> services;

            String[] patterns;

            RequestMethod[] methods;

            public Set<String> getServices() {
                return services == null ? emptySet() : services;
            }

            public void setServices(Set<String> services) {
                this.services = services;
            }

            public void setPatterns(String[] patterns) {
                this.patterns = patterns;
            }

            public void setMethods(RequestMethod[] methods) {
                this.methods = methods;
            }
        }
    }

    private static RequestMappingInfo buildRequestMappingInfo(WebEndpointMapping webEndpointMapping) {
        RequestMethod[] methods = buildRequestMethods(webEndpointMapping);
        return paths(webEndpointMapping.getPatterns())
                .methods(methods)
                .params(webEndpointMapping.getParams())
                .headers(webEndpointMapping.getHeaders())
                .consumes(webEndpointMapping.getConsumes())
                .produces(webEndpointMapping.getProduces())
                .build();
    }

    private static RequestMethod[] buildRequestMethods(WebEndpointMapping webEndpointMapping) {
        return of(webEndpointMapping.getMethods())
                .map(RequestMethod::valueOf)
                .toArray(RequestMethod[]::new);
    }
}