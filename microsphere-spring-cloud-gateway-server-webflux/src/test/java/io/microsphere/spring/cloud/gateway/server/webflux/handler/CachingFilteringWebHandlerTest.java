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
package io.microsphere.spring.cloud.gateway.server.webflux.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.event.RefreshRoutesResultEvent;
import org.springframework.cloud.gateway.route.RouteLocator;

import static io.microsphere.spring.cloud.gateway.server.webflux.handler.CachingFilteringWebHandler.EMPTY_FILTER_ARRAY;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Flux.empty;

/**
 * {@link CachingFilteringWebHandler} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class CachingFilteringWebHandlerTest {

    private CachingFilteringWebHandler webHandler;

    @BeforeEach
    void setUp() {
        this.webHandler = new CachingFilteringWebHandler(emptyList());
    }

    @Test
    public void testDestroy() {
        testOnRefreshRoutesResultEvent();
        this.webHandler.destroy();
    }

    @Test
    public void testDestroyWithoutInitialization() {
        this.webHandler.destroy();
    }

    @Test
    void testOnRefreshRoutesResultEvent() {
        testOnRefreshRoutesResultEvent(null);
    }

    @Test
    void testOnRefreshRoutesResultEventWithThrowable() {
        testOnRefreshRoutesResultEvent(new Exception("For testing"));
    }

    @Test
    void testGetRoutedGatewayFilters() {
        assertSame(EMPTY_FILTER_ARRAY, this.webHandler.getRoutedGatewayFilters(null));
    }

    void testOnRefreshRoutesResultEvent(Throwable throwable) {
        RouteLocator routeLocator = mock(RouteLocator.class);
        when(routeLocator.getRoutes()).thenReturn(empty());
        RefreshRoutesResultEvent event = new RefreshRoutesResultEvent(routeLocator, throwable);
        this.webHandler.onRefreshRoutesResultEvent(event);
    }
}