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

package io.microsphere.spring.cloud.gateway.event;


import io.microsphere.spring.context.event.EnableEventExtension;
import io.microsphere.util.ValueHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.cloud.client.discovery.event.ParentHeartbeatEvent;
import org.springframework.cloud.gateway.route.RouteRefreshListener;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link DisabledHeartbeatEventRouteRefreshListenerInterceptor} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see DisabledHeartbeatEventRouteRefreshListenerInterceptor
 * @since 1.0.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {
                RouteRefreshListener.class,
                DisabledHeartbeatEventRouteRefreshListenerInterceptor.class,
                DisabledHeartbeatEventRouteRefreshListenerInterceptorTest.class
        }
)
@EnableEventExtension
class DisabledHeartbeatEventRouteRefreshListenerInterceptorTest {

    @Autowired
    private ConfigurableApplicationContext context;

    @Test
    void testIntercept() {
        this.context.publishEvent(new HeartbeatEvent(this, this));
        this.context.publishEvent(new ParentHeartbeatEvent(this, this));

        ValueHolder<Object> testValueHolder = new ValueHolder<>();
        this.context.addApplicationListener((ApplicationListener<PayloadApplicationEvent>)
                event -> testValueHolder.setValue(event.getPayload()));

        String value = "Hello,World";
        this.context.publishEvent(value);
        Object payload = testValueHolder.getValue();
        assertSame(value, payload);
    }
}