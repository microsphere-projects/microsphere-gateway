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

package io.microsphere.spring.cloud.gateway.server.webmvc.autoconfigure;


import io.microsphere.spring.cloud.gateway.server.webmvc.autoconfigure.WebEndpointMappingGatewayServerMvcAutoConfiguration.WebEndpointMappingHandlerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link WebEndpointMappingGatewayServerMvcAutoConfiguration} Test on disabled
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebEndpointMappingGatewayServerMvcAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                WebEndpointMappingGatewayServerMvcAutoConfigurationDisabledTest.class
        },
        properties = {
                "microsphere.autoconfigure.exclude=org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration," +
                        "org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientAutoConfiguration," +
                        "org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration",
        }
)
@EnableAutoConfiguration
@DirtiesContext
class WebEndpointMappingGatewayServerMvcAutoConfigurationDisabledTest {

    @Autowired(required = false)
    private WebEndpointMappingHandlerConfig webEndpointMappingHandlerConfig;

    @Test
    void test() {
        assertNull(webEndpointMappingHandlerConfig);
    }
}