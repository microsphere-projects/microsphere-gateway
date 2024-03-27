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
package io.microsphere.spring.cloud.gateway.autoconfigure;

import io.microsphere.spring.cloud.gateway.filter.WebEndpointMappingGlobalFilter;
import io.microsphere.spring.cloud.gateway.handler.ServiceInstancePredicate;
import io.microsphere.spring.web.metadata.WebEndpointMapping;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.config.conditional.ConditionalOnEnabledGlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * Gateway Auto-Configuration for {@link WebEndpointMapping}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebEndpointMappingGlobalFilter
 * @see GatewayAutoConfiguration
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
@ConditionalOnProperty(name = "spring.cloud.gateway.enabled", matchIfMissing = true)
@AutoConfigureAfter(GatewayAutoConfiguration.class)
public class WebEndpointMappingGatewayAutoConfiguration {

    @Bean
    @ConditionalOnEnabledGlobalFilter
    @ConditionalOnMissingBean(value = ServiceInstancePredicate.class)
    public ServiceInstancePredicate serviceInstancePredicate() {
        return (serverWebExchange, serviceInstance) -> {
            String[] paths = StringUtils.tokenizeToStringArray(serverWebExchange.getRequest().getURI().getRawPath(), "/");
            if (ObjectUtils.isEmpty(paths)) {
                return false;
            }
            return Objects.equals(paths[0], serviceInstance.getServiceId().toLowerCase());
        };
    }

    @Bean
    @ConditionalOnEnabledGlobalFilter
    @ConditionalOnBean(value = DiscoveryClient.class, search = SearchStrategy.CURRENT)
    public WebEndpointMappingGlobalFilter webEndpointMappingGlobalFilter(DiscoveryClient discoveryClient,
                                                                         ObjectProvider<ServiceInstancePredicate> webEndpointServiceInstanceChooseHandler) {
        WebEndpointMappingGlobalFilter webEndpointMappingGlobalFilter = new WebEndpointMappingGlobalFilter(discoveryClient);
        webEndpointMappingGlobalFilter.setWebEndpointServiceInstanceChooseHandler(webEndpointServiceInstanceChooseHandler.getIfAvailable());
        return webEndpointMappingGlobalFilter;
    }

}
