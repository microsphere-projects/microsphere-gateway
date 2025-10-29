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

import io.microsphere.spring.boot.context.config.BindableConfigurationBeanBinder;
import io.microsphere.spring.cloud.gateway.handler.ServiceInstancePredicate;
import io.microsphere.spring.context.config.ConfigurationBeanBinder;
import io.microsphere.spring.web.metadata.WebEndpointMapping;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.event.RefreshRoutesResultEvent;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static io.microsphere.collection.CollectionUtils.isNotEmpty;
import static io.microsphere.collection.PropertiesUtils.flatProperties;
import static io.microsphere.constants.PathConstants.SLASH_CHAR;
import static io.microsphere.constants.SymbolConstants.COLON_CHAR;
import static io.microsphere.net.URLUtils.buildURI;
import static io.microsphere.spring.cloud.client.service.registry.constants.InstanceConstants.WEB_CONTEXT_PATH_METADATA_NAME;
import static io.microsphere.spring.cloud.client.service.util.ServiceInstanceUtils.getWebEndpointMappings;
import static io.microsphere.spring.web.metadata.WebEndpointMapping.ID_HEADER_NAME;
import static io.microsphere.util.ArrayUtils.isNotEmpty;
import static java.lang.Math.abs;
import static java.lang.String.valueOf;
import static java.net.URI.create;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
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

    private final DiscoveryClient discoveryClient;

    private ServiceInstancePredicate serviceInstancePredicate;

    volatile Map<String, Collection<RequestMappingContext>> routedRequestMappingContexts = null;

    volatile Map<String, Config> routedConfigs = null;

    public WebEndpointMappingGlobalFilter(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    public void setWebEndpointServiceInstanceChooseHandler(ServiceInstancePredicate serviceInstancePredicate) {
        this.serviceInstancePredicate = serviceInstancePredicate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);

        if (isInvalidRequest(url)) {
            // NO Web-Endpoint scheme
            return chain.filter(exchange);
        }

        RequestMappingContext requestMappingContext = getMatchingRequestMappingContext(exchange);

        if (requestMappingContext != null) {
            // The RequestMappingContext found
            ServiceInstance serviceInstance = requestMappingContext.choose(exchange);
            if (serviceInstance != null) {
                String basePath = buildBasePath(serviceInstance);
                String path = buildPath(serviceInstance, url);
                URI targetURI = create(basePath + path);
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

    private RequestMappingContext getMatchingRequestMappingContext(ServerWebExchange exchange) {
        String routeId = getRouteId(exchange);

        if (isExcludedRequest(routeId, exchange)) {
            // The request is excluded
            return null;
        }

        Map<String, Collection<RequestMappingContext>> routedRequestMappingContexts = this.routedRequestMappingContexts;

        if (routedRequestMappingContexts == null) {
            // No RequestMappingContexts for routing
            return null;
        }

        Collection<RequestMappingContext> requestMappingContexts = routedRequestMappingContexts.get(routeId);

        if (isEmpty(requestMappingContexts)) {
            // No RequestMappingContext found
            return null;
        }

        RequestMappingContext target = null;
        PathContainer pathWithinApplication = exchange.getRequest().getPath().pathWithinApplication();
        ServerWebExchange newExchange;
        if (serviceInstancePredicate != null && pathWithinApplication.elements().size() >= 2) {
            // remove applicationName
            String applicationName = pathWithinApplication.subPath(0, 2).value();
            RequestPath requestPath = exchange.getRequest().getPath().modifyContextPath(applicationName);
            ServerHttpRequest request = exchange.getRequest().mutate().path(requestPath.pathWithinApplication().value()).build();
            newExchange = exchange.mutate().request(request).build();
        } else {
            newExchange = exchange;
        }
        List<RequestMappingContext> matchesRequestMappings = new ArrayList<>();
        for (RequestMappingContext requestMappingContext : requestMappingContexts) {
            if (matchesRequestMapping(newExchange, requestMappingContext)) {
                // matches the request mappings
                matchesRequestMappings.add(requestMappingContext);
            }
        }
        matchesRequestMappings.sort((v1, v2) -> v1.compareTo(v2, newExchange));
        if (isNotEmpty(matchesRequestMappings)) {
            // matches the request mapping
            target = matchesRequestMappings.get(0);
        }
        return target;
    }

    private String getRouteId(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        return route == null ? null : route.getId();
    }

    private boolean isInvalidRequest(URI url) {
        return url == null || !SCHEME.equals(url.getScheme());
    }

    private boolean matchesRequestMapping(ServerWebExchange exchange, RequestMappingContext requestMappingContext) {
        RequestMappingInfo requestMappingInfo = requestMappingContext.requestMappingInfo;
        return requestMappingInfo.getMatchingCondition(exchange) != null;
    }

    private boolean isExcludedRequest(String routeId, ServerWebExchange exchange) {
        if (routeId == null) {
            return true;
        }
        Map<String, Config> routedConfigs = this.routedConfigs;
        if (routedConfigs == null) {
            return false;
        }
        Config config = routedConfigs.get(routeId);
        if (config == null) {
            return false;
        }
        return config.isExcludedRequest(exchange);
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
                                        requestMappingContext.setWebEndpointServiceInstanceChooseHandler(serviceInstancePredicate);
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

        Exclude exclude = new Exclude();

        String loadBalancer;

        RequestMappingInfo excludeRequestMappingInfo;

        public void setExclude(Exclude exclude) {
            this.exclude = exclude;
        }

        public void setLoadBalancer(String loadBalancer) {
            this.loadBalancer = loadBalancer;
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

        private ServiceInstancePredicate serviceInstancePredicate;

        private int id;

        private List<ServiceInstance> serviceInstances = new LinkedList<>();

        private final AtomicInteger position = new AtomicInteger(0);

        RequestMappingContext(WebEndpointMapping webEndpointMapping) {
            this.requestMappingInfo = buildRequestMappingInfo(webEndpointMapping);
            this.id = webEndpointMapping.getId();
        }

        void setWebEndpointServiceInstanceChooseHandler(ServiceInstancePredicate serviceInstancePredicate) {
            this.serviceInstancePredicate = serviceInstancePredicate;
        }

        void addServiceInstance(ServiceInstance serviceInstance) {
            this.serviceInstances.add(serviceInstance);
        }

        ServiceInstance choose(ServerWebExchange exchange) {
            List<ServiceInstance> serviceInstances = this.serviceInstances.stream()
                    .filter(serviceInstance -> serviceInstancePredicate(exchange, serviceInstance))
                    .collect(toList());
            int size = serviceInstances.size();
            if (size == 0) {
                return null;
            }

            int offset = size == 1 ? 0 : abs(this.position.incrementAndGet()) % size;
            return serviceInstances.get(offset);
        }

        boolean serviceInstancePredicate(ServerWebExchange exchange, ServiceInstance serviceInstance) {
            ServiceInstancePredicate serviceInstancePredicate = this.serviceInstancePredicate;
            if (serviceInstancePredicate == null) {
                return true;
            }
            return serviceInstancePredicate.test(exchange, serviceInstance);
        }

        public int compareTo(RequestMappingContext other, ServerWebExchange exchange) {
            return this.requestMappingInfo.compareTo(other.requestMappingInfo, exchange);
        }
    }

    static String buildBasePath(ServiceInstance serviceInstance) {
        // TODO Refactor this to microsphere-spring-cloud-commons
        boolean isSecure = serviceInstance.isSecure();
        String prefix = isSecure ? "https://" : "http://";
        String host = serviceInstance.getHost();
        String port = valueOf(serviceInstance.getPort());
        StringBuilder basePathBuilder = new StringBuilder((isSecure ? 9 : 8) + host.length() + port.length());
        basePathBuilder.append(prefix)
                .append(host)
                .append(COLON_CHAR)
                .append(port);
        // TODO append the context path
        return basePathBuilder.toString();
    }

    static String buildPath(ServiceInstance serviceInstance, URI url) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        String path = url.getPath();
        if (isEmpty(metadata)) {
            return path;
        }
        String servicePath = SLASH_CHAR + serviceInstance.getServiceId().toLowerCase() + SLASH_CHAR;
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
