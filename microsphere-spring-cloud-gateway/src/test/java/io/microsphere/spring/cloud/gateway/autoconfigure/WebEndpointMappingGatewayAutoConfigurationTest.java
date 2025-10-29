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


import io.microsphere.spring.cloud.gateway.filter.DefaultGatewayFilterChain;
import io.microsphere.spring.cloud.gateway.filter.NoOpGatewayFilter;
import io.microsphere.spring.cloud.gateway.filter.WebEndpointMappingGlobalFilter;
import io.microsphere.spring.cloud.gateway.handler.ServiceInstancePredicate;
import io.microsphere.spring.webflux.annotation.EnableWebFluxExtension;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.get;
import static org.springframework.mock.web.server.MockServerWebExchange.from;

/**
 * {@link WebEndpointMappingGatewayAutoConfiguration} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebEndpointMappingGatewayAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                WebEndpointMappingGatewayAutoConfigurationTest.class,
        },
        webEnvironment = RANDOM_PORT
)
@EnableAutoConfiguration
@EnableWebFluxExtension
class WebEndpointMappingGatewayAutoConfigurationTest {

    @Autowired
    private ServiceInstancePredicate serviceInstancePredicate;

    @Autowired
    private WebEndpointMappingGlobalFilter webEndpointMappingGlobalFilter;

    @Test
    void testServiceInstancePredicate() {
        testServiceInstancePredicate("/test/abc", "test", true);
        testServiceInstancePredicate("/test/abc", "TEST", true);
        testServiceInstancePredicate("/test/abc", "Test", true);
        testServiceInstancePredicate("/test/abc", "t", false);
        testServiceInstancePredicate("/", "t", false);
        testServiceInstancePredicate("", "t", false);
        testServiceInstancePredicate("/a", "t", false);
    }

    @Test
    void testRequestWebEndpointMappingGlobalFilter() {
        testRequestWebEndpointMappingGlobalFilter("/test", false);
        testRequestWebEndpointMappingGlobalFilter("/test", true);
        testRequestWebEndpointMappingGlobalFilter("we:/all/test", true);
    }

    void testServiceInstancePredicate(String path, String serviceId, boolean expected) {
        MockServerWebExchange serverWebExchange = createServerWebbExchange(path);
        DefaultServiceInstance instance = new DefaultServiceInstance();
        instance.setServiceId(serviceId);
        assertEquals(expected, serviceInstancePredicate.test(serverWebExchange, instance));
    }

    void testRequestWebEndpointMappingGlobalFilter(String path, boolean withAttribute) {
        MockServerWebExchange serverWebExchange = createServerWebbExchange(path);
        if (withAttribute) {
            serverWebExchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, serverWebExchange.getRequest().getURI());
        }
        DefaultGatewayFilterChain chain = new DefaultGatewayFilterChain(new NoOpGatewayFilter());
        assertNotNull(this.webEndpointMappingGlobalFilter.filter(serverWebExchange, chain));
    }

    MockServerWebExchange createServerWebbExchange(String path) {
        MockServerHttpRequest request = get(path).build();
        return from(request);
    }
}