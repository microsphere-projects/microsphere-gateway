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

package io.microsphere.spring.cloud.gateway.filter;

import io.microsphere.spring.cloud.client.service.registry.DefaultRegistration;
import io.microsphere.spring.cloud.gateway.filter.WebEndpointMappingGlobalFilter.Config;
import io.microsphere.spring.cloud.gateway.filter.WebEndpointMappingGlobalFilter.RequestMappingContext;
import io.microsphere.spring.web.metadata.WebEndpointMapping;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static io.microsphere.spring.web.metadata.WebEndpointMapping.Kind.WEB_FLUX;
import static io.microsphere.spring.web.metadata.WebEndpointMapping.of;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.get;
import static org.springframework.mock.web.server.MockServerWebExchange.from;

/**
 * {@link WebEndpointMappingGlobalFilter} Static staff Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebEndpointMappingGlobalFilter
 * @see WebEndpointMappingGlobalFilter.Config
 * @see WebEndpointMappingGlobalFilter.RequestMappingContext
 * @since 1.0.0
 */
public class WebEndpointMappingGlobalFilterStaticTest {

    @Test
    void testConfigWithDefaults() {
        Config config = new Config();
        config.init();
        assertFalse(config.isExcludedRequest(null));
    }

    @Test
    void testRequestMappingContext() {
        WebEndpointMapping mapping = of(WEB_FLUX)
                .endpoint(this)
                .pattern("/test")
                .method("GET")
                .build();
        RequestMappingContext context = new RequestMappingContext(mapping);

        DefaultRegistration registration = new DefaultRegistration();

        context.addServiceInstance(registration);
        context.addServiceInstance(registration);

        MockServerHttpRequest request = get("/test-app/test/helloworld").build();
        MockServerWebExchange exchange = from(request);

        for (int i = 0; i < 10; i++) {
            ServiceInstance serviceInstance = context.choose(exchange);
            assertSame(registration, serviceInstance);
        }
    }
}
