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


import io.microsphere.spring.cloud.gateway.mvc.autoconfigure.WebEndpointMappingGatewayServerMvcAutoConfiguration.WebEndpointMappingHandlerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Set;

import static io.microsphere.collection.Lists.ofList;
import static io.microsphere.collection.Sets.ofSet;
import static io.microsphere.spring.cloud.gateway.mvc.constants.GatewayPropertyConstants.GATEWAY_ROUTES_PROPERTY_NAME_PREFIX;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link WebEndpointMappingGatewayServerMvcAutoConfiguration} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebEndpointMappingGatewayServerMvcAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                WebEndpointMappingGatewayServerMvcAutoConfigurationTest.class
        },
        properties = {
                "spring.profiles.active=gateway"
        }
)
@EnableAutoConfiguration
@DirtiesContext
class WebEndpointMappingGatewayServerMvcAutoConfigurationTest {

    @Autowired
    private WebEndpointMappingHandlerConfig webEndpointMappingHandlerConfig;

    @Autowired
    private ApplicationContext context;

    @Test
    void test() {
        assertNotNull(webEndpointMappingHandlerConfig);

        RouteProperties routeProperties = new RouteProperties();
        routeProperties.setId("unknown-route-id");
        this.webEndpointMappingHandlerConfig.refresh(ofList(routeProperties), this.context, handlerFilterFunction -> {
        });


        Set<String> keys = ofSet("test-property-name");
        assertTrue(this.webEndpointMappingHandlerConfig.findWebEndpointMappingRouteProperties(keys).isEmpty());

        keys = ofSet(GATEWAY_ROUTES_PROPERTY_NAME_PREFIX + "[0].id",
                GATEWAY_ROUTES_PROPERTY_NAME_PREFIX + "[0].metadata",
                GATEWAY_ROUTES_PROPERTY_NAME_PREFIX + "[1].id");
        assertFalse(this.webEndpointMappingHandlerConfig.findWebEndpointMappingRouteProperties(keys).isEmpty());
    }
}