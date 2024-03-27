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

import io.microsphere.spring.cloud.gateway.event.DisabledHeartbeatEventRouteRefreshListenerInterceptor;
import io.microsphere.spring.cloud.gateway.event.PropagatingRefreshRoutesEventApplicationListener;
import io.microsphere.spring.cloud.gateway.filter.WebEndpointMappingGlobalFilter;
import io.microsphere.spring.cloud.gateway.handler.FilteringWebHandlerBeanDefinitionRegistryPostProcessor;
import io.microsphere.spring.context.annotation.EnableEventExtension;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Gateway Auto-Configuration
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebEndpointMappingGlobalFilter
 * @see org.springframework.cloud.gateway.config.GatewayAutoConfiguration
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.cloud.gateway.enabled", matchIfMissing = true)
@AutoConfigureAfter(org.springframework.cloud.gateway.config.GatewayAutoConfiguration.class)
@EnableEventExtension(intercepted = true)
@Import(value = {
        DisabledHeartbeatEventRouteRefreshListenerInterceptor.class,
        PropagatingRefreshRoutesEventApplicationListener.class,
        FilteringWebHandlerBeanDefinitionRegistryPostProcessor.class
})
public class GatewayAutoConfiguration {
}
