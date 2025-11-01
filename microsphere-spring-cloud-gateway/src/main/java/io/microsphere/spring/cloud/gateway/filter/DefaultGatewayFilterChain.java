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

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static io.microsphere.util.ArrayUtils.length;
import static io.microsphere.util.Assert.assertNoNullElements;
import static io.microsphere.util.Assert.assertTrue;
import static reactor.core.publisher.Mono.defer;
import static reactor.core.publisher.Mono.empty;

/**
 * Default {@link GatewayFilterChain}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see GatewayFilterChain
 * @since 1.0.0
 */
public class DefaultGatewayFilterChain implements GatewayFilterChain {

    private final GatewayFilter[] gatewayFilters;

    private final int length;

    private int position;

    public DefaultGatewayFilterChain(GatewayFilter... gatewayFilters) {
        int length = length(gatewayFilters);
        assertTrue(length != 0, () -> "The 'gatewayFilters' must not be empty");
        assertNoNullElements(gatewayFilters, "Any element of 'gatewayFilters' must not be null");
        this.gatewayFilters = gatewayFilters;
        this.length = length;
        this.position = 0;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange) {
        if (position < length) {
            GatewayFilter gatewayFilter = this.gatewayFilters[position++];
            return defer(() -> gatewayFilter.filter(exchange, this));
        }
        return empty();
    }
}