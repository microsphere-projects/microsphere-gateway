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
package io.microsphere.spring.cloud.gateway.filter;

import io.microsphere.annotation.Nonnull;
import io.microsphere.logging.Logger;
import io.microsphere.spring.boot.context.config.BindableConfigurationBeanBinder;
import io.microsphere.spring.context.config.ConfigurationBeanBinder;
import io.microsphere.spring.web.metadata.WebEndpointMapping;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.gateway.event.RefreshRoutesResultEvent;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.microsphere.collection.ListUtils.first;
import static io.microsphere.collection.ListUtils.newArrayList;
import static io.microsphere.collection.PropertiesUtils.flatProperties;
import static io.microsphere.constants.PathConstants.SLASH_CHAR;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.net.URLUtils.buildURI;
import static io.microsphere.spring.cloud.client.service.registry.constants.InstanceConstants.WEB_CONTEXT_PATH_METADATA_NAME;
import static io.microsphere.spring.cloud.client.service.util.ServiceInstanceUtils.getUriString;
import static io.microsphere.spring.cloud.client.service.util.ServiceInstanceUtils.getWebEndpointMappings;
import static io.microsphere.spring.cloud.gateway.filter.WebEndpointMappingGlobalFilter.Config.DEFAULT_CONFIG;
import static io.microsphere.spring.web.metadata.WebEndpointMapping.ID_HEADER_NAME;
import static io.microsphere.spring.web.util.MonoUtils.getValue;
import static io.microsphere.util.ArrayUtils.isNotEmpty;
import static io.microsphere.util.StringUtils.isBlank;
import static io.microsphere.util.StringUtils.substringAfter;
import static java.lang.String.valueOf;
import static java.net.URI.create;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Stream.of;
import static org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.getUriTemplateVariables;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.commaDelimitedListToSet;
import static org.springframework.web.reactive.result.method.RequestMappingInfo.paths;

/**
 * {@link WebEndpointMapping}  {@link GlobalFilter}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see ReactiveLoadBalancerClientFilter
 * @since 1.0.0
 */
public class WebEndpointMappingGlobalFilter implements GlobalFilter, ApplicationListener<RefreshRoutesResultEvent>,
        DisposableBean, Ordered {

    private static final Logger logger = getLogger(WebEndpointMappingGlobalFilter.class);

    /**
     * The Web Endpoint scheme of the {@link Route#getUri() Routes' URI}
     */
    public static final String SCHEME = "we";

    /**
     * The all services for mapping
     */
    public static final String ALL_SERVICES = "all";

    /**
     * The key of the {@link Route#getMetadata() Routes' Metadata}
     */
    public static final String METADATA_KEY = "web-endpoint";

    /**
     * The URI template variable name for application name
     */
    public static final String APPLICATION_NAME_URI_TEMPLATE_VARIABLE_NAME = "application";

    static final String NEW_PATH_ATTRIBUTE_NAME = "msg-new-path";

    private final DiscoveryClient discoveryClient;

    private final LoadBalancerClientFactory clientFactory;

    volatile Map<String, Collection<RequestMappingContext>> routedRequestMappingContexts = null;

    volatile Map<String, Config> routedConfigs = null;

    public WebEndpointMappingGlobalFilter(DiscoveryClient discoveryClient,
                                          LoadBalancerClientFactory clientFactory) {
        this.discoveryClient = discoveryClient;
        this.clientFactory = clientFactory;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        if (isInvalidScheme(url)) {
            // NO Web-Endpoint scheme
            logger.trace("The scheme of Gateway Request URL['{}'] is invalid ", url);
            return chain.filter(exchange);
        }

        Map<String, String> uriTemplateVariables = getUriTemplateVariables(exchange);
        String applicationName = uriTemplateVariables.get(APPLICATION_NAME_URI_TEMPLATE_VARIABLE_NAME);
        if (isBlank(applicationName)) {
            logger.trace("No application name was found by the request URL['{}'] with uriTemplateVariables : {}", url, uriTemplateVariables);
            return chain.filter(exchange);
        }

        RequestMappingContext requestMappingContext = getMatchingRequestMappingContext(applicationName, exchange);

        if (requestMappingContext != null) {
            // The RequestMappingContext found
            ServiceInstance serviceInstance = choose(applicationName);
            if (serviceInstance != null) {
                String uri = getUriString(serviceInstance);
                String path = (String) exchange.getAttributes().remove(NEW_PATH_ATTRIBUTE_NAME);
                URI targetURI = create(uri + path);
                int id = requestMappingContext.id;
                ServerHttpRequest request = exchange.getRequest()
                        .mutate()
                        .header(ID_HEADER_NAME, valueOf(id)).build();
                exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, targetURI);
                return chain.filter(exchange.mutate().request(request).build());
            }
        }

        return chain.filter(exchange);
    }

    private ServiceInstance choose(String applicationName) {
        ReactorLoadBalancer<ServiceInstance> loadBalancer = this.clientFactory.getInstance(applicationName, ReactorServiceInstanceLoadBalancer.class);
        Mono<Response<ServiceInstance>> mono = loadBalancer.choose();
        Response<ServiceInstance> response = getValue(mono);
        return response.getServer();
    }

    private RequestMappingContext getMatchingRequestMappingContext(String applicationName, ServerWebExchange exchange) {
        String routeId = getRouteId(exchange);

        if (isExcludedRequest(routeId, exchange)) {
            // The request is excluded
            logger.trace("The request is excluded");
            return null;
        }

        Map<String, Collection<RequestMappingContext>> routedRequestMappingContexts = this.routedRequestMappingContexts;

        if (routedRequestMappingContexts == null) {
            // No RequestMappingContexts for routing
            logger.trace("The 'routedRequestMappingContexts' was not initialized");
            return null;
        }

        Collection<RequestMappingContext> requestMappingContexts = routedRequestMappingContexts.get(routeId);

        if (isEmpty(requestMappingContexts)) {
            // No RequestMappingContext found
            logger.trace("No RequestMappingContext was not found by route id['{}'] : {}", routeId, requestMappingContexts);
            return null;
        }

        ServerHttpRequest request = exchange.getRequest();
        RequestPath requestPath = request.getPath();
        String path = requestPath.value();


        String newPath = substringAfter(path, SLASH_CHAR + applicationName);
        exchange.getAttributes().put(NEW_PATH_ATTRIBUTE_NAME, newPath);
        ServerHttpRequest newRequest = request.mutate().path(newPath).build();
        ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();

        List<RequestMappingContext> matchesRequestMappings = newArrayList(requestMappingContexts.size());
        for (RequestMappingContext requestMappingContext : requestMappingContexts) {
            if (matchesRequestMapping(newExchange, requestMappingContext)) {
                // matches the request mappings
                matchesRequestMappings.add(requestMappingContext);
            }
        }
        matchesRequestMappings.sort((v1, v2) -> v1.compareTo(v2, newExchange));

        return first(matchesRequestMappings);
    }

    private String getRouteId(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        return route == null ? null : route.getId();
    }

    private boolean isInvalidScheme(URI url) {
        return url == null || !SCHEME.equals(url.getScheme());
    }

    private boolean matchesRequestMapping(ServerWebExchange exchange, RequestMappingContext requestMappingContext) {
        RequestMappingInfo requestMappingInfo = requestMappingContext.requestMappingInfo;
        return requestMappingInfo.getMatchingCondition(exchange) != null;
    }

    private boolean isExcludedRequest(String routeId, ServerWebExchange exchange) {
        Config config = getConfig(routeId);
        return config.isExcludedRequest(exchange);
    }

    @Nonnull
    Config getConfig(String routeId) {
        if (routeId == null) {
            logger.trace("No id of Route was found");
            return DEFAULT_CONFIG;
        }

        Map<String, Config> routedConfigs = this.routedConfigs;
        if (routedConfigs == null) {
            logger.trace("The routed configs was not initialized");
            return DEFAULT_CONFIG;
        }

        Config config = routedConfigs.get(routeId);
        if (config == null) {
            logger.trace("No routed config was found by id : '{}'", routeId);
            return DEFAULT_CONFIG;
        }
        return config;
    }

    @Override
    public int getOrder() {
        return LOAD_BALANCER_CLIENT_FILTER_ORDER - 1;
    }

    @Override
    public void onApplicationEvent(RefreshRoutesResultEvent event) {
        if (matchesEvent(event)) {
            Map<String, Collection<RequestMappingContext>> routedContexts = new HashMap<>();
            Map<String, Config> routedConfigs = new HashMap<>();
            RouteLocator routeLocator = (RouteLocator) event.getSource();
            routeLocator.getRoutes().filter(this::isWebEndpointRoute).subscribe((route -> {
                String routeId = route.getId();
                Config config = createConfig(route);
                routedConfigs.put(routeId, config);
                Map<WebEndpointMapping, RequestMappingContext> mappedContexts = new HashMap<>();
                getSubscribedServices(route, config)
                        .stream()
                        .map(discoveryClient::getInstances)
                        // TODO support ZonePreferenceFilter
                        .flatMap(List::stream)
                        .forEach(serviceInstance -> {
                            getWebEndpointMappings(serviceInstance)
                                    .stream()
                                    .forEach(webEndpointMapping -> {
                                        RequestMappingContext requestMappingContext = mappedContexts.computeIfAbsent(webEndpointMapping, RequestMappingContext::new);
                                        requestMappingContext.addServiceInstance(serviceInstance);
                                    });
                        });
                routedContexts.put(routeId, mappedContexts.values());
            })).dispose();

            // exchange
            synchronized (this) {
                this.routedRequestMappingContexts = routedContexts;
                this.routedConfigs = routedConfigs;
            }
        }
    }

    Config createConfig(Route route) {
        Map<String, Object> metadata = route.getMetadata();
        Config config = new Config();
        Map<String, Object> properties = (Map) metadata.getOrDefault(METADATA_KEY, emptyMap());
        Map<String, Object> flatProperties = flatProperties(properties);
        ConfigurationBeanBinder beanBinder = new BindableConfigurationBeanBinder();
        beanBinder.bind(flatProperties, true, true, config);
        config.init();
        return config;
    }

    private boolean matchesEvent(RefreshRoutesResultEvent event) {
        return event.isSuccess() && (event.getSource() instanceof RouteLocator);
    }

    Collection<String> getSubscribedServices(Route route, Config config) {
        Set<String> excludedServices = config.exclude.getServices();
        URI uri = route.getUri();
        String host = uri.getHost();
        final Collection<String> services = new LinkedList<>();
        if (ALL_SERVICES.equals(host)) {
            services.addAll(discoveryClient.getServices());
        } else {
            services.addAll(commaDelimitedListToSet(host));
        }
        services.removeAll(excludedServices);
        return services;
    }

    private boolean isWebEndpointRoute(Route route) {
        URI uri = route.getUri();
        return SCHEME.equals(uri.getScheme());
    }

    @Override
    public void destroy() {
        if (this.routedRequestMappingContexts != null) {
            this.routedRequestMappingContexts.clear();
        }
        if (this.routedConfigs != null) {
            this.routedConfigs.clear();
        }
    }

    /**
     * Clear for testing
     */
    void clear() {
        this.routedRequestMappingContexts = null;
        this.routedConfigs = null;
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

        boolean isExcludedRequest(ServerWebExchange exchange) {
            return excludeRequestMappingInfo == null ? false :
                    excludeRequestMappingInfo.getMatchingCondition(exchange) != null;
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

        public int compareTo(RequestMappingContext other, ServerWebExchange exchange) {
            return this.requestMappingInfo.compareTo(other.requestMappingInfo, exchange);
        }
    }

    static String buildPath(ServiceInstance serviceInstance, URI url) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        String path = url.getPath();
        if (isEmpty(metadata)) {
            return path;
        }
        String serviceId = serviceInstance.getServiceId();
        String servicePath = SLASH_CHAR + serviceId + SLASH_CHAR;
        int index = path.indexOf(servicePath, 0);
        if (index != 0) {
            return path;
        }
        String contextPath = metadata.get(WEB_CONTEXT_PATH_METADATA_NAME);
        return buildURI(contextPath, path.substring(servicePath.length()));
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