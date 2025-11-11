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
package io.microsphere.spring.cloud.gateway.handler;

import io.microsphere.spring.cloud.gateway.filter.DefaultGatewayFilterChain;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cloud.gateway.event.RefreshRoutesResultEvent;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.handler.FilteringWebHandler;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.event.EventListener;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.microsphere.reflect.FieldUtils.getFieldValue;
import static io.microsphere.spring.cloud.gateway.util.GatewayUtils.isSuccessRouteLocatorEvent;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
import static org.springframework.core.annotation.AnnotationAwareOrderComparator.sort;

/**
 * {@link FilteringWebHandler} extension class caches the {@link GlobalFilter GlobalFilters} and
 * the {@link GatewayFilter GatewayFilters} from the matched {@link Route Routes} when
 * {@link #handle(ServerWebExchange) handle} the request
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see FilteringWebHandler
 * @see GlobalFilter
 * @see GatewayFilter
 * @see Route
 * @see RouteLocator
 * @see RefreshRoutesResultEvent
 * @since 1.0.0
 */
public class CachingFilteringWebHandler extends FilteringWebHandler implements DisposableBean {

    static final GatewayFilter[] EMPTY_FILTER_ARRAY = new GatewayFilter[0];

    private volatile Map<String, GatewayFilter[]> routedGatewayFiltersCache = null;

    public CachingFilteringWebHandler(List<GlobalFilter> globalFilters) {
        super(globalFilters);
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange) {
        Route route = exchange.getRequiredAttribute(GATEWAY_ROUTE_ATTR);
        GatewayFilter[] routedGatewayFilters = getRoutedGatewayFilters(route);
        return new DefaultGatewayFilterChain(routedGatewayFilters).filter(exchange);
    }

    @Override
    public void destroy() {
        if (routedGatewayFiltersCache != null) {
            routedGatewayFiltersCache.clear();
        }
    }

    @EventListener(RefreshRoutesResultEvent.class)
    public void onRefreshRoutesResultEvent(RefreshRoutesResultEvent event) {
        if (matchesEvent(event)) {
            RouteLocator routeLocator = (RouteLocator) event.getSource();
            this.routedGatewayFiltersCache = buildRoutedGatewayFiltersCache(routeLocator);
        }
    }

    private Map<String, GatewayFilter[]> buildRoutedGatewayFiltersCache(RouteLocator routeLocator) {
        Map<String, GatewayFilter[]> routedGatewayFiltersCache = new HashMap<>();
        routeLocator.getRoutes().subscribe(route -> {
            String routeId = route.getId();
            GatewayFilter[] combinedGatewayFilters = combineGatewayFilters(route);
            routedGatewayFiltersCache.put(routeId, combinedGatewayFilters);
        }).dispose();
        return routedGatewayFiltersCache;
    }

    GatewayFilter[] getRoutedGatewayFilters(Route route) {
        Map<String, GatewayFilter[]> routedGatewayFiltersCache = this.routedGatewayFiltersCache;
        if (routedGatewayFiltersCache == null) {
            return EMPTY_FILTER_ARRAY;
        } else {
            String id = route.getId();
            return routedGatewayFiltersCache.getOrDefault(id, EMPTY_FILTER_ARRAY);
        }
    }

    private GatewayFilter[] combineGatewayFilters(Route route) {
        List<GatewayFilter> globalFilters = globalFilters();
        List<GatewayFilter> gatewayFilters = route.getFilters();
        List<GatewayFilter> allFilters = new ArrayList<>(globalFilters.size() + gatewayFilters.size());
        allFilters.addAll(globalFilters);
        allFilters.addAll(gatewayFilters);
        sort(allFilters);
        return allFilters.toArray(EMPTY_FILTER_ARRAY);
    }

    private boolean matchesEvent(RefreshRoutesResultEvent event) {
        return isSuccessRouteLocatorEvent(event);
    }

    private List<GatewayFilter> globalFilters() {
        return getFieldValue(this, "globalFilters");
    }
}