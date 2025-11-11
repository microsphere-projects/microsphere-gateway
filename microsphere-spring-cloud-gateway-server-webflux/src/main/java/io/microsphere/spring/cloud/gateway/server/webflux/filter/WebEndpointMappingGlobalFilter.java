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
package io.microsphere.spring.cloud.gateway.server.webflux.filter;

import io.microsphere.logging.Logger;
import io.microsphere.spring.cloud.client.event.ServiceInstancesChangedEvent;
import io.microsphere.spring.cloud.gateway.commons.config.WebEndpointConfig;
import io.microsphere.spring.cloud.gateway.commons.config.WebEndpointConfig.Mapping;
import io.microsphere.spring.web.metadata.WebEndpointMapping;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.event.RefreshRoutesResultEvent;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.microsphere.collection.ListUtils.first;
import static io.microsphere.collection.ListUtils.newArrayList;
import static io.microsphere.constants.PathConstants.SLASH_CHAR;
import static io.microsphere.lang.function.Streams.filterFirst;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.net.URLUtils.buildURI;
import static io.microsphere.spring.cloud.client.service.registry.constants.InstanceConstants.WEB_CONTEXT_PATH_METADATA_NAME;
import static io.microsphere.spring.cloud.client.service.util.ServiceInstanceUtils.getUriString;
import static io.microsphere.spring.cloud.client.service.util.ServiceInstanceUtils.getWebEndpointMappings;
import static io.microsphere.spring.cloud.gateway.server.webflux.autoconfigure.WebEndpointMappingGatewayAutoConfiguration.ROUTE_METADATA_WEB_ENDPOINT_KEY;
import static io.microsphere.spring.cloud.gateway.server.webflux.constants.GatewayPropertyConstants.GATEWAY_ROUTES_PROPERTY_NAME_PREFIX;
import static io.microsphere.spring.cloud.gateway.server.webflux.util.GatewayUtils.isSuccessRouteLocatorEvent;
import static io.microsphere.spring.web.metadata.WebEndpointMapping.ID_HEADER_NAME;
import static io.microsphere.spring.web.util.MonoUtils.getValue;
import static io.microsphere.util.StringUtils.isBlank;
import static io.microsphere.util.StringUtils.substringAfter;
import static java.lang.String.valueOf;
import static java.net.URI.create;
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
public class WebEndpointMappingGlobalFilter implements GlobalFilter, SmartApplicationListener, ApplicationContextAware,
        EnvironmentAware, DisposableBean, Ordered {

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
     * The URI template variable name for application name
     */
    public static final String APPLICATION_NAME_URI_TEMPLATE_VARIABLE_NAME = "application";

    static final String NEW_PATH_ATTRIBUTE_NAME = "msg-new-path";

    private final DiscoveryClient discoveryClient;

    private final LoadBalancerClientFactory clientFactory;

    private final GatewayProperties gatewayProperties;

    private ApplicationContext context;

    private Environment environment;

    volatile Map<String, Collection<RequestMappingContext>> routedRequestMappingContextsCache = null;

    volatile Map<String, Collection<RequestMappingInfo>> routedExcludedRequestMappingInfoCache = null;

    public WebEndpointMappingGlobalFilter(DiscoveryClient discoveryClient,
                                          LoadBalancerClientFactory clientFactory, GatewayProperties gatewayProperties) {
        this.discoveryClient = discoveryClient;
        this.clientFactory = clientFactory;
        this.gatewayProperties = gatewayProperties;
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

    public boolean supportsAsyncExecution() {
        return false;
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return ContextRefreshedEvent.class.equals(eventType)
                || RefreshRoutesResultEvent.class.equals(eventType)
                || EnvironmentChangeEvent.class.equals(eventType)
                || ServiceInstancesChangedEvent.class.equals(eventType);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent contextRefreshedEvent) {
            onContextRefreshedEvent(contextRefreshedEvent);
        } else if (event instanceof RefreshRoutesResultEvent refreshRoutesResultEvent) {
            onRefreshRoutesResultEvent(refreshRoutesResultEvent);
        } else if (event instanceof EnvironmentChangeEvent environmentChangeEvent) {
            onEnvironmentChangeEvent(environmentChangeEvent);
        } else {
            onServiceInstancesChangedEvent();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void destroy() {
        clear(this.routedRequestMappingContextsCache);
        clear(this.routedExcludedRequestMappingInfoCache);
    }

    @Override
    public int getOrder() {
        return LOAD_BALANCER_CLIENT_FILTER_ORDER - 1;
    }

    private void onContextRefreshedEvent(ContextRefreshedEvent event) {
        if (this.context == event.getApplicationContext()) {
            refresh();
        }
    }

    private void onRefreshRoutesResultEvent(RefreshRoutesResultEvent event) {
        if (matchesEvent(event)) {
            refresh();
        }
    }

    private void onEnvironmentChangeEvent(EnvironmentChangeEvent event) {
        if (matchesEvent(event)) {
            refresh();
        }
    }

    private void onServiceInstancesChangedEvent() {
        refresh();
    }

    private void refresh() {
        Map<String, Collection<RequestMappingContext>> routedRequestMappingContextsCache = new ConcurrentHashMap<>();
        Map<String, Collection<RequestMappingInfo>> routedExcludedRequestMappingInfoCache = new ConcurrentHashMap<>();
        List<RouteDefinition> routes = this.gatewayProperties.getRoutes();

        for (RouteDefinition route : routes) {
            URI routeUri = route.getUri();
            if (isWebEndpointRoute(routeUri)) {
                String routeId = route.getId();
                Collection<RequestMappingContext> requestMappingContexts = buildRequestMappingContexts(routeUri);
                routedRequestMappingContextsCache.put(routeId, requestMappingContexts);

                Set<RequestMappingInfo> requestMappingInfoSet = buildExcludedRequestMappingInfoSet(routes, routeId);
                routedExcludedRequestMappingInfoCache.put(routeId, requestMappingInfoSet);
            }
        }

        // exchange
        synchronized (this) {
            this.routedRequestMappingContextsCache = routedRequestMappingContextsCache;
            this.routedExcludedRequestMappingInfoCache = routedExcludedRequestMappingInfoCache;
        }
    }

    private Set<RequestMappingInfo> buildExcludedRequestMappingInfoSet(List<RouteDefinition> routes, String routeId) {
        WebEndpointConfig webEndpointConfig = findWebEndpointConfig(routes, routeId);

        List<Mapping> excludes = webEndpointConfig.getExcludes();
        Set<RequestMappingInfo> requestMappingInfoSet = new HashSet<>(excludes.size());
        for (Mapping exclude : excludes) {
            requestMappingInfoSet.add(buildRequestMappingInfo(exclude));
        }
        return requestMappingInfoSet;
    }

    private Collection<RequestMappingContext> buildRequestMappingContexts(URI routeUri) {
        Collection<String> subscribedServices = getSubscribedServices(routeUri);
        Collection<RequestMappingContext> requestMappingContexts = new LinkedList<>();
        // TODO support ZonePreferenceFilter
        for (String subscribedService : subscribedServices) {
            ServiceInstance sampleServiceInstance = choose(subscribedService);
            Collection<WebEndpointMapping> webEndpointMappings = getWebEndpointMappings(sampleServiceInstance);
            for (WebEndpointMapping webEndpointMapping : webEndpointMappings) {
                RequestMappingContext requestMappingContext = new RequestMappingContext(webEndpointMapping);
                requestMappingContexts.add(requestMappingContext);
            }
        }
        return requestMappingContexts;
    }

    private WebEndpointConfig findWebEndpointConfig(List<RouteDefinition> routes, String routeId) {
        RouteDefinition routeDefinition = filterFirst(routes, def -> routeId.equals(def.getId()));
        Map<String, Object> metadata = routeDefinition.getMetadata();
        return (WebEndpointConfig) metadata.get(ROUTE_METADATA_WEB_ENDPOINT_KEY);
    }

    private ServiceInstance choose(String applicationName) {
        ReactorLoadBalancer<ServiceInstance> loadBalancer = this.clientFactory.getInstance(applicationName, ReactorServiceInstanceLoadBalancer.class);
        Mono<Response<ServiceInstance>> mono = loadBalancer.choose();
        Response<ServiceInstance> response = getValue(mono);
        return response.getServer();
    }

    private RequestMappingContext getMatchingRequestMappingContext(String applicationName, ServerWebExchange exchange) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        String routeId = route.getId();
        return getMatchingRequestMappingContext(applicationName, routeId, exchange);
    }

    RequestMappingContext getMatchingRequestMappingContext(String applicationName, String routeId, ServerWebExchange exchange) {
        if (isExcludedRequest(routeId, exchange)) {
            // The request is excluded
            logger.trace("The request is excluded");
            return null;
        }

        Map<String, Collection<RequestMappingContext>> routedRequestMappingContexts = this.routedRequestMappingContextsCache;

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

    private boolean isInvalidScheme(URI url) {
        return !isWebEndpointRoute(url);
    }

    private boolean matchesRequestMapping(ServerWebExchange exchange, RequestMappingContext requestMappingContext) {
        RequestMappingInfo requestMappingInfo = requestMappingContext.requestMappingInfo;
        return requestMappingInfo.getMatchingCondition(exchange) != null;
    }

    private boolean isExcludedRequest(String routeId, ServerWebExchange exchange) {
        Collection<RequestMappingInfo> excludedRequestMappingInfoSet = routedExcludedRequestMappingInfoCache.getOrDefault(routeId, emptySet());
        for (RequestMappingInfo excludedRequestMappingInfo : excludedRequestMappingInfoSet) {
            if (excludedRequestMappingInfo.getMatchingCondition(exchange) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesEvent(RefreshRoutesResultEvent event) {
        return isSuccessRouteLocatorEvent(event);
    }

    private boolean matchesEvent(EnvironmentChangeEvent event) {
        Set<String> keys = event.getKeys();
        for (String key : keys) {
            if (key.startsWith(GATEWAY_ROUTES_PROPERTY_NAME_PREFIX)) {
                int lastIndex = key.lastIndexOf(".id");
                if (lastIndex > -1) {
                    List<RouteDefinition> routes = this.gatewayProperties.getRoutes();
                    String propertyValue = this.environment.getProperty(key);
                    for (RouteDefinition route : routes) {
                        if (route.getId().equals(propertyValue)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    Collection<String> getSubscribedServices(URI routeUri) {
        String host = routeUri.getHost();
        if (ALL_SERVICES.equals(host)) {
            return discoveryClient.getServices();
        }
        return commaDelimitedListToSet(host);
    }

    private boolean isWebEndpointRoute(URI routeUri) {
        return routeUri != null && SCHEME.equals(routeUri.getScheme());
    }

    /**
     * Clear for testing
     */
    void clear() {
        this.routedRequestMappingContextsCache = null;
    }

    static class RequestMappingContext {

        private final RequestMappingInfo requestMappingInfo;

        private int id;

        RequestMappingContext(WebEndpointMapping webEndpointMapping) {
            this.requestMappingInfo = buildRequestMappingInfo(webEndpointMapping);
            this.id = webEndpointMapping.getId();
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

    private static RequestMappingInfo buildRequestMappingInfo(Mapping mapping) {
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

    static void clear(Map<?, ?> map) {
        if (map != null) {
            map.clear();
        }
    }
}