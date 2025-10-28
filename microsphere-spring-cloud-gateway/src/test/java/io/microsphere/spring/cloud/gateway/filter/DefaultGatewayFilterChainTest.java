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


import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.get;
import static org.springframework.mock.web.server.MockServerWebExchange.from;
import static reactor.core.publisher.Mono.empty;

/**
 * {@link DefaultGatewayFilterChain} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see DefaultGatewayFilterChain
 * @since 1.0.0
 */
class DefaultGatewayFilterChainTest {

    @Test
    void testConstructorWithEmptyGatewayFilters() {
        assertThrows(IllegalArgumentException.class, DefaultGatewayFilterChain::new);
    }

    @Test
    void testConstructorWithNullGatewayFilter() {
        assertThrows(IllegalArgumentException.class, () -> new DefaultGatewayFilterChain((GatewayFilter) null));
    }

    @Test
    void testFilter() {
        MockServerHttpRequest request = get("/test").build();
        MockServerWebExchange serverWebExchange = from(request);

        DefaultGatewayFilterChain chain = new DefaultGatewayFilterChain(new NoOpGatewayFilter());
        assertNotNull(chain.filter(serverWebExchange));
        assertSame(empty(), chain.filter(serverWebExchange));

        chain = new DefaultGatewayFilterChain(new NoOpGatewayFilter(), new NoOpGatewayFilter(), new NoOpGatewayFilter());
        assertNotNull(chain.filter(serverWebExchange));
    }
}