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

import io.microsphere.spring.cloud.client.discovery.autoconfigure.ReactiveDiscoveryClientAutoConfiguration;
import io.microsphere.spring.cloud.gateway.mvc.annotation.ConditionalOnGatewayServerMvcEnabled;
import io.microsphere.spring.cloud.gateway.mvc.filter.WebEndpointMappingHandlerFilterFunction;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.gateway.server.mvc.GatewayServerMvcAutoConfiguration;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctionDefinition;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static io.microsphere.collection.Lists.ofList;
import static io.microsphere.spring.cloud.gateway.mvc.constants.GatewayPropertyConstants.GATEWAY_ROUTES_PROPERTY_PREFIX;
import static io.microsphere.util.StringUtils.startsWith;
import static java.util.Collections.emptyList;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

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
@AutoConfigureAfter(
        value = {
                GatewayServerMvcAutoConfiguration.class,
                ReactiveDiscoveryClientAutoConfiguration.class,
                ConfigurationPropertiesRebinderAutoConfiguration.class
        },
        name = {
                "org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration",
                "org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientAutoConfiguration"
        }
)
public class WebEndpointMappingGatewayServerMvcAutoConfiguration {

    @Bean
    @ConditionalOnBean(value = {GatewayMvcProperties.class, DiscoveryClient.class})
    @Scope(SCOPE_PROTOTYPE)
    public WebEndpointMappingHandlerFilterFunction webEndpointMappingHandlerFilterFunction(GatewayMvcProperties gatewayMvcProperties,
                                                                                           DiscoveryClient discoveryClient) {
        return new WebEndpointMappingHandlerFilterFunction(gatewayMvcProperties, discoveryClient);
    }

    @Bean
    @ConditionalOnBean(value = {LoadBalancerClientFactory.class, WebEndpointMappingHandlerFilterFunction.class})
    public Function<RouteProperties, HandlerFunctionDefinition> weHandlerFunctionDefinition(ConfigurableApplicationContext context) {
        return routeProperties -> new WebEndpointMappingHandlerFunctionDefinition(routeProperties, context);
    }

    static class WebEndpointMappingHandlerFunctionDefinition implements HandlerFunctionDefinition, SmartApplicationListener {

        private final RouteProperties routeProperties;

        private final ConfigurableApplicationContext context;

        private final ConfigurableEnvironment environment;

        private final WebEndpointMappingHandlerFilterFunction function;

        WebEndpointMappingHandlerFunctionDefinition(RouteProperties routeProperties, ConfigurableApplicationContext context) {
            this.routeProperties = routeProperties;
            this.context = context;
            this.environment = context.getEnvironment();
            this.function = context.getBean(WebEndpointMappingHandlerFilterFunction.class);
            this.function.setRouteProperties(routeProperties);
            this.function.setApplicationContext(context);
            context.addApplicationListener(this);
        }

        @Override
        public HandlerFunction<ServerResponse> handlerFunction() {
            return http();
        }

        @Override
        public List<HandlerFilterFunction<ServerResponse, ServerResponse>> lowerPrecedenceFilters() {
            return ofList(function);
        }

        @Override
        public List<HandlerFilterFunction<ServerResponse, ServerResponse>> higherPrecedenceFilters() {
            return emptyList();
        }

        @Override
        public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
            return EnvironmentChangeEvent.class.isAssignableFrom(eventType)
                    || ContextRefreshedEvent.class.isAssignableFrom(eventType);
        }

        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            if (event instanceof ContextRefreshedEvent contextRefreshedEvent) {
                this.function.refresh(this.routeProperties, contextRefreshedEvent.getApplicationContext());
            } else if (event instanceof EnvironmentChangeEvent environmentChangeEvent) {
                onEnvironmentChangeEvent(environmentChangeEvent);
            }
        }

        public void onEnvironmentChangeEvent(EnvironmentChangeEvent event) {
            String prefix = findRoutePropertyPrefix(event.getKeys());
            if (prefix == null) {
                return;
            }
            this.function.refresh(this.routeProperties, this.context);
        }

        private String findRoutePropertyPrefix(Set<String> keys) {
            for (String key : keys) {
                if (startsWith(key, GATEWAY_ROUTES_PROPERTY_PREFIX)) {
                    int lastIndex = key.lastIndexOf(".id");
                    if (lastIndex == key.length() - 4) {
                        String propertyValue = this.environment.getProperty(key);
                        if (this.routeProperties.getId().equals(propertyValue)) {
                            return propertyValue.substring(0, lastIndex);
                        }
                    }
                }
            }
            return null;
        }
    }
}