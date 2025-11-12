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

package io.microsphere.spring.cloud.gateway.server.webmvc.filter;

import io.microsphere.annotation.Nonnull;
import io.microsphere.annotation.Nullable;
import io.microsphere.logging.Logger;
import io.microsphere.spring.cloud.gateway.commons.config.WebEndpointConfig;
import io.microsphere.spring.web.metadata.WebEndpointMapping;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.http.server.RequestPath;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static io.microsphere.collection.ListUtils.first;
import static io.microsphere.collection.ListUtils.newArrayList;
import static io.microsphere.constants.PathConstants.SLASH_CHAR;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.spring.cloud.client.service.util.ServiceInstanceUtils.getWebEndpointMappings;
import static io.microsphere.spring.cloud.gateway.commons.config.ConfigUtils.getWebEndpointConfig;
import static io.microsphere.spring.cloud.gateway.commons.constants.CommonConstants.APPLICATION_NAME_URI_TEMPLATE_VARIABLE_NAME;
import static io.microsphere.spring.cloud.gateway.commons.constants.RouteConstants.ALL_SERVICES;
import static io.microsphere.spring.cloud.gateway.commons.constants.RouteConstants.WEB_ENDPOINT_REWRITE_PATH_ATTRIBUTE_NAME;
import static io.microsphere.spring.web.metadata.WebEndpointMapping.ID_HEADER_NAME;
import static io.microsphere.util.StringUtils.isBlank;
import static io.microsphere.util.StringUtils.substringAfter;
import static java.lang.String.valueOf;
import static java.net.URI.create;
import static java.util.Collections.emptySet;
import static java.util.stream.Stream.of;
import static org.springframework.cloud.gateway.server.mvc.common.MvcUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.server.mvc.common.MvcUtils.GATEWAY_ROUTE_ID_ATTR;
import static org.springframework.cloud.gateway.server.mvc.common.MvcUtils.getAttribute;
import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.http.server.RequestPath.parse;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.commaDelimitedListToSet;
import static org.springframework.web.servlet.function.ServerRequest.from;
import static org.springframework.web.servlet.mvc.method.RequestMappingInfo.paths;
import static org.springframework.web.util.ServletRequestPathUtils.setParsedRequestPath;

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

    private final String routeId;

    @Nonnull
    private DiscoveryClient discoveryClient;

    @Nonnull
    private ApplicationContext context;

    volatile Collection<RequestMappingContext> requestMappingContexts = null;

    volatile Collection<RequestMappingInfo> excludedRequestMappingInfoSet = null;

    public WebEndpointMappingHandlerFilterFunction(final String routeId) {
        this.routeId = routeId;
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
        String rewritePath = (String) attributes.remove(WEB_ENDPOINT_REWRITE_PATH_ATTRIBUTE_NAME);
        int id = requestMappingContext.id;
        ServerRequest newRequest = from(request)
                .uri(create(rewritePath))
                .header(ID_HEADER_NAME, valueOf(id))
                .build();

        attributes.put(GATEWAY_REQUEST_URL_ATTR, newRequest.uri());

        return lbHandlerFunctionDefinition.filter(newRequest, next);
    }

    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
        this.discoveryClient = context.getBean(DiscoveryClient.class);
    }

    public void refresh(RouteProperties routeProperties, ApplicationContext context) {
        if (context != this.context) {
            return;
        }

        if (!Objects.equals(this.routeId, routeProperties.getId())) {
            return;
        }

        Collection<RequestMappingContext> requestMappingContexts = buildRequestMappingContexts(routeProperties);

        Set<RequestMappingInfo> excludedRequestMappingInfoSet = buildExcludedRequestMappingInfoSet(routeProperties);

        synchronized (this) {
            this.requestMappingContexts = requestMappingContexts;
            this.excludedRequestMappingInfoSet = excludedRequestMappingInfoSet;
        }

        logger.trace("The 'requestMappingContexts' and 'excludedRequestMappingInfoSet' were refreshed!");
    }

    Collection<RequestMappingContext> buildRequestMappingContexts(RouteProperties routeProperties) {
        URI routeUri = routeProperties.getUri();
        Collection<String> subscribedServices = getSubscribedServices(routeUri);
        Collection<RequestMappingContext> requestMappingContexts = new LinkedList<>();
        // TODO support ZonePreferenceFilter
        for (String subscribedService : subscribedServices) {
            ServiceInstance sampleServiceInstance = choose(subscribedService);
            if (sampleServiceInstance != null) {
                Collection<WebEndpointMapping> webEndpointMappings = getWebEndpointMappings(sampleServiceInstance);
                for (WebEndpointMapping webEndpointMapping : webEndpointMappings) {
                    RequestMappingContext requestMappingContext = new RequestMappingContext(webEndpointMapping);
                    requestMappingContexts.add(requestMappingContext);
                }
            }
        }
        return requestMappingContexts;
    }

    Set<RequestMappingInfo> buildExcludedRequestMappingInfoSet(RouteProperties routeProperties) {
        WebEndpointConfig webEndpointConfig = getWebEndpointConfig(routeProperties.getMetadata());
        if (webEndpointConfig == null) {
            return emptySet();
        }

        List<WebEndpointConfig.Mapping> excludes = webEndpointConfig.getExcludes();
        Set<RequestMappingInfo> requestMappingInfoSet = new HashSet<>(excludes.size());
        for (WebEndpointConfig.Mapping exclude : excludes) {
            requestMappingInfoSet.add(buildRequestMappingInfo(exclude));
        }
        return requestMappingInfoSet;
    }

    @Nullable
    private ServiceInstance choose(String applicationName) {
        List<ServiceInstance> serviceInstances = this.discoveryClient.getInstances(applicationName);
        return serviceInstances.stream().findAny().orElse(null);
    }

    private RequestMappingContext getMatchingRequestMappingContext(String applicationName, ServerRequest request) {
        String routeId = getAttribute(request, GATEWAY_ROUTE_ID_ATTR);
        return getMatchingRequestMappingContext(applicationName, routeId, request);
    }

    Collection<String> getSubscribedServices(URI uri) {
        String host = uri.getHost();
        if (ALL_SERVICES.equals(host)) {
            return discoveryClient.getServices();
        }
        return commaDelimitedListToSet(host);
    }

    RequestMappingContext getMatchingRequestMappingContext(String applicationName, String routeId, ServerRequest request) {
        HttpServletRequest servletRequest = request.servletRequest();
        if (isExcludedRequest(servletRequest)) {
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

        String rewritePath = substringAfter(path, SLASH_CHAR + applicationName);
        request.attributes().put(WEB_ENDPOINT_REWRITE_PATH_ATTRIBUTE_NAME, rewritePath);

        RequestPath rewriteRequestPath = parse(rewritePath, null);
        setParsedRequestPath(rewriteRequestPath, servletRequest);

        List<RequestMappingContext> matchesRequestMappings = newArrayList(requestMappingContexts.size());
        for (RequestMappingContext requestMappingContext : requestMappingContexts) {
            if (matchesRequestMapping(requestMappingContext, servletRequest)) {
                // matches the request mappings
                matchesRequestMappings.add(requestMappingContext);
            }
        }
        matchesRequestMappings.sort((v1, v2) -> v1.compareTo(v2, servletRequest));

        return first(matchesRequestMappings);
    }

    private boolean matchesRequestMapping(RequestMappingContext requestMappingContext, HttpServletRequest servletRequest) {
        RequestMappingInfo requestMappingInfo = requestMappingContext.requestMappingInfo;
        return matches(requestMappingInfo, servletRequest);
    }

    boolean isExcludedRequest(HttpServletRequest request) {
        Collection<RequestMappingInfo> excludedRequestMappingInfoSet = this.excludedRequestMappingInfoSet;
        if (excludedRequestMappingInfoSet != null) {
            for (RequestMappingInfo excludedRequestMappingInfo : excludedRequestMappingInfoSet) {
                if (matches(excludedRequestMappingInfo, request)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matches(RequestMappingInfo requestMappingInfo, HttpServletRequest servletRequest) {
        return requestMappingInfo.getMatchingCondition(servletRequest) != null;
    }

    static class RequestMappingContext {

        private final RequestMappingInfo requestMappingInfo;

        private int id;

        RequestMappingContext(WebEndpointMapping webEndpointMapping) {
            this.requestMappingInfo = buildRequestMappingInfo(webEndpointMapping);
            this.id = webEndpointMapping.getId();
        }

        int compareTo(RequestMappingContext other, HttpServletRequest request) {
            return this.requestMappingInfo.compareTo(other.requestMappingInfo, request);
        }
    }

    private static RequestMappingInfo buildRequestMappingInfo(WebEndpointConfig.Mapping mapping) {
        return paths(mapping.getPatterns())
                .methods(mapping.getMethods())
                .params(mapping.getParams())
                .headers(mapping.getHeaders())
                .consumes(mapping.getConsumes())
                .produces(mapping.getProduces())
                .build();
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