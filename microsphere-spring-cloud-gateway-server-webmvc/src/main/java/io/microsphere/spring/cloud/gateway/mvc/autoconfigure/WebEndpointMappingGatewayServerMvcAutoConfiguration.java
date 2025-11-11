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
package io.microsphere.spring.cloud.gateway.mvc.autoconfigure;

import io.microsphere.spring.cloud.client.event.ServiceInstancesChangedEvent;
import io.microsphere.spring.cloud.gateway.commons.annotation.ConditionalOnMicrosphereWebEndpointMappingEnabled;
import io.microsphere.spring.cloud.gateway.mvc.annotation.ConditionalOnGatewayServerMvcEnabled;
import io.microsphere.spring.cloud.gateway.mvc.filter.WebEndpointMappingHandlerFilterFunction;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientAutoConfiguration;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.gateway.server.mvc.GatewayServerMvcAutoConfiguration;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.microsphere.spring.cloud.gateway.mvc.constants.GatewayPropertyConstants.GATEWAY_ROUTES_PROPERTY_NAME_PREFIX;
import static io.microsphere.spring.cloud.gateway.mvc.filter.WebEndpointMappingHandlerFilterFunction.SCHEME;
import static io.microsphere.spring.cloud.gateway.mvc.filter.WebEndpointMappingHandlerSupplier.getWebEndpointMappingHandlerFilterFunction;
import static io.microsphere.util.StringUtils.startsWith;
import static java.util.stream.Collectors.toUnmodifiableList;

/**
 * Gateway Server MVC Auto-Configuration for {@link io.microsphere.spring.web.metadata.WebEndpointMapping}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see ConditionalOnGatewayServerMvcEnabled
 * @see GatewayServerMvcAutoConfiguration
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
@ConditionalOnBlockingDiscoveryEnabled
@ConditionalOnGatewayServerMvcEnabled
@ConditionalOnMicrosphereWebEndpointMappingEnabled
@AutoConfigureAfter(
        value = {
                GatewayServerMvcAutoConfiguration.class,
                CompositeDiscoveryClientAutoConfiguration.class,
                ConfigurationPropertiesRebinderAutoConfiguration.class
        },
        name = {
                "org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration",
                "io.microsphere.spring.cloud.client.discovery.autoconfigure.ReactiveDiscoveryClientAutoConfiguration"
        }
)
@Import(WebEndpointMappingGatewayServerMvcAutoConfiguration.WebEndpointMappingHandlerConfig.class)
public class WebEndpointMappingGatewayServerMvcAutoConfiguration {

    @ConditionalOnBean(value = {GatewayMvcProperties.class, DiscoveryClient.class, LoadBalancerClientFactory.class})
    static class WebEndpointMappingHandlerConfig implements SmartApplicationListener {

        private final GatewayMvcProperties gatewayMvcProperties;

        private final ConfigurableApplicationContext context;

        private final ConfigurableEnvironment environment;

        WebEndpointMappingHandlerConfig(GatewayMvcProperties gatewayMvcProperties, ConfigurableApplicationContext context) {
            this.gatewayMvcProperties = gatewayMvcProperties;
            this.context = context;
            this.environment = context.getEnvironment();
        }

        @Override
        public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
            return EnvironmentChangeEvent.class.isAssignableFrom(eventType)
                    || ContextRefreshedEvent.class.isAssignableFrom(eventType)
                    || ServiceInstancesChangedEvent.class.isAssignableFrom(eventType);
        }

        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            if (event instanceof ContextRefreshedEvent contextRefreshedEvent) {
                onContextRefreshedEvent(contextRefreshedEvent);
            } else if (event instanceof EnvironmentChangeEvent environmentChangeEvent) {
                onEnvironmentChangeEvent(environmentChangeEvent);
            } else if (event instanceof ServiceInstancesChangedEvent) {
                onServiceInstancesChangedEvent();
            }
        }

        void onContextRefreshedEvent(ContextRefreshedEvent event) {
            ApplicationContext context = event.getApplicationContext();
            List<RouteProperties> routes = getWebEndpointMappingRouteProperties();
            refresh(routes, context, handlerFilterFunction -> {
                handlerFilterFunction.setApplicationContext(context);
            });
        }

        void onEnvironmentChangeEvent(EnvironmentChangeEvent event) {
            refresh(() -> findWebEndpointMappingRouteProperties(event.getKeys()));
        }

        void onServiceInstancesChangedEvent() {
            refresh(this::getWebEndpointMappingRouteProperties);
        }

        private void refresh(Supplier<List<RouteProperties>> routesSupplier) {
            List<RouteProperties> routes = routesSupplier.get();
            refresh(routes, this.context);
        }

        private void refresh(List<RouteProperties> routes, ApplicationContext context) {
            refresh(routes, context, handlerFilterFunction -> {
            });
        }

        private void refresh(List<RouteProperties> routes, ApplicationContext context,
                             Consumer<WebEndpointMappingHandlerFilterFunction> handlerFilterFunctionInitializer) {
            for (RouteProperties routeProperties : routes) {
                String routeID = routeProperties.getId();
                WebEndpointMappingHandlerFilterFunction handlerFilterFunction = getWebEndpointMappingHandlerFilterFunction(routeID);
                if (handlerFilterFunction != null) {
                    handlerFilterFunctionInitializer.accept(handlerFilterFunction);
                    handlerFilterFunction.refresh(routeProperties, context);
                }
            }
        }

        private List<RouteProperties> findWebEndpointMappingRouteProperties(Set<String> keys) {
            List<RouteProperties> routes = getWebEndpointMappingRouteProperties();
            List<RouteProperties> foundRoutes = new LinkedList<>();
            for (String key : keys) {
                if (startsWith(key, GATEWAY_ROUTES_PROPERTY_NAME_PREFIX)) {
                    int lastIndex = key.lastIndexOf(".id");
                    if (lastIndex > -1) {
                        String propertyValue = this.environment.getProperty(key);
                        for (RouteProperties routeProperties : routes) {
                            if (routeProperties.getId().equals(propertyValue)) {
                                foundRoutes.add(routeProperties);
                            }
                        }
                    }
                }
            }
            return foundRoutes;
        }

        private List<RouteProperties> getWebEndpointMappingRouteProperties() {
            return this.gatewayMvcProperties.getRoutes()
                    .stream()
                    .filter(routeProperties -> SCHEME.equals(routeProperties.getUri().getScheme()))
                    .collect(toUnmodifiableList());
        }
    }
}