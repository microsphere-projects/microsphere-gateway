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


import io.microsphere.spring.boot.test.AutoConfigurationTest;
import io.microsphere.spring.boot.test.WebAutoConfigurationTest;
import io.microsphere.spring.cloud.gateway.server.webflux.event.DisabledHeartbeatEventRouteRefreshListenerInterceptor;
import io.microsphere.spring.cloud.gateway.server.webflux.event.PropagatingRefreshRoutesEventApplicationListener;
import io.microsphere.spring.cloud.gateway.server.webflux.handler.FilteringWebHandlerBeanDefinitionRegistryPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.GatewayProperties;

import java.util.Set;

/**
 * {@link GatewayAutoConfiguration} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see GatewayAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                GatewayAutoConfigurationTest.class
        }
)
class GatewayAutoConfigurationTest extends WebAutoConfigurationTest<GatewayAutoConfiguration> {

    @Override
    protected void configureAutoConfiguredClasses(Set<Class<?>> autoConfiguredClasses) {
        autoConfiguredClasses.add(DisabledHeartbeatEventRouteRefreshListenerInterceptor.class);
        autoConfiguredClasses.add(PropagatingRefreshRoutesEventApplicationListener.class);
        autoConfiguredClasses.add(FilteringWebHandlerBeanDefinitionRegistryPostProcessor.class);
    }

    @Override
    protected void configureGlobalDisabledPropertyValues(Set<String> globalDisabledPropertyValues) {
        globalDisabledPropertyValues.add("spring.cloud.gateway.server.webflux.enabled=false");
        globalDisabledPropertyValues.add("microsphere.spring.cloud.gateway.enabled=false");
    }

    @Override
    protected void configureGlobalMissingClasses(Set<Class<?>> globalMissingClasses) {
        globalMissingClasses.add(GatewayProperties.class);
    }
}