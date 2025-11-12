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
package io.microsphere.spring.cloud.gateway.server.webflux.autoconfigure;

import io.microsphere.spring.cloud.client.discovery.ReactiveDiscoveryClientAdapter;
import io.microsphere.spring.cloud.client.discovery.autoconfigure.ReactiveDiscoveryClientAutoConfiguration;
import io.microsphere.spring.cloud.gateway.commons.annotation.ConditionalOnMicrosphereWebEndpointMappingEnabled;
import io.microsphere.spring.cloud.gateway.commons.config.WebEndpointConfig;
import io.microsphere.spring.cloud.gateway.server.webflux.annotation.ConditionalOnGatewayEnabled;
import io.microsphere.spring.cloud.gateway.server.webflux.filter.WebEndpointMappingGlobalFilter;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindHandlerAdvisor;
import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnReactiveDiscoveryEnabled;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.config.conditional.ConditionalOnEnabledGlobalFilter;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Map;

import static io.microsphere.spring.cloud.gateway.commons.config.ConfigUtils.getWebEndpointConfig;
import static io.microsphere.spring.cloud.gateway.commons.constants.RouteConstants.METADATA_KEY;
import static io.microsphere.spring.cloud.gateway.commons.constants.RouteConstants.WEB_ENDPOINT_KEY;
import static io.microsphere.spring.cloud.gateway.server.webflux.constants.GatewayPropertyConstants.GATEWAY_ROUTES_PROPERTY_NAME_PREFIX;
import static org.springframework.boot.autoconfigure.condition.SearchStrategy.CURRENT;

/**
 * Gateway Auto-Configuration for {@link io.microsphere.spring.web.metadata.WebEndpointMapping}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebEndpointMappingGlobalFilter
 * @see GatewayAutoConfiguration
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
@ConditionalOnReactiveDiscoveryEnabled
@ConditionalOnGatewayEnabled
@ConditionalOnMicrosphereWebEndpointMappingEnabled
@AutoConfigureAfter(
        value = {
                GatewayAutoConfiguration.class,
                ReactiveDiscoveryClientAutoConfiguration.class
        },
        name = {
                "org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration",
                "org.springframework.cloud.client.discovery.composite.reactive.ReactiveCompositeDiscoveryClientAutoConfiguration"
        }
)
public class WebEndpointMappingGatewayAutoConfiguration implements ConfigurationPropertiesBindHandlerAdvisor, EnvironmentAware {

    private Environment environment;

    @Bean
    @ConditionalOnEnabledGlobalFilter
    @ConditionalOnBean(value = {ReactiveDiscoveryClientAdapter.class, LoadBalancerClientFactory.class}, search = CURRENT)
    public WebEndpointMappingGlobalFilter webEndpointMappingGlobalFilter(ReactiveDiscoveryClientAdapter reactiveDiscoveryClient,
                                                                         LoadBalancerClientFactory loadBalancerClientFactory,
                                                                         GatewayProperties gatewayProperties) {
        return new WebEndpointMappingGlobalFilter(reactiveDiscoveryClient, loadBalancerClientFactory, gatewayProperties);
    }

    @Override
    public BindHandler apply(BindHandler bindHandler) {
        return new AbstractBindHandler(bindHandler) {
            @Override
            public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) throws Exception {
                String propertyName = name.toString();
                if (propertyName.startsWith(GATEWAY_ROUTES_PROPERTY_NAME_PREFIX) && propertyName.endsWith(METADATA_KEY) && result != null) {
                    ConfigurationPropertyName webEndpointName = name.append(WEB_ENDPOINT_KEY);
                    WebEndpointConfig webEndpointConfig = getWebEndpointConfig(environment, webEndpointName.toString());
                    if (webEndpointConfig != null) {
                        Map<String, Object> metadata = (Map<String, Object>) result;
                        metadata.put(WEB_ENDPOINT_KEY, webEndpointConfig);
                    }
                }
            }
        };
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}