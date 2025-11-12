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

package io.microsphere.spring.cloud.gateway.server.webflux.event;


import io.microsphere.spring.context.event.EnableEventExtension;
import io.microsphere.util.ValueHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static io.microsphere.collection.Sets.ofSet;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.springframework.cloud.gateway.config.GatewayProperties.PREFIX;

/**
 * {@link PropagatingRefreshRoutesEventApplicationListener} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see PropagatingRefreshRoutesEventApplicationListener
 * @since 1.0.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {
                PropagatingRefreshRoutesEventApplicationListener.class,
                PropagatingRefreshRoutesEventApplicationListenerTest.class
        }
)
@EnableEventExtension
class PropagatingRefreshRoutesEventApplicationListenerTest {

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private PropagatingRefreshRoutesEventApplicationListener listener;

    @Test
    void testOnApplicationEvent() {
        ValueHolder<RefreshRoutesEvent> testValueHolder = new ValueHolder<>();
        this.context.addApplicationListener((ApplicationListener<RefreshRoutesEvent>)
                event -> testValueHolder.setValue(event));

        this.context.publishEvent(new EnvironmentChangeEvent(ofSet(PREFIX + ".enabled")));

        RefreshRoutesEvent event = testValueHolder.getValue();
        assertSame(this.listener, event.getSource());
    }

    @Test
    void testOnApplicationEventWithoutGatewayProperties() {
        ValueHolder<RefreshRoutesEvent> testValueHolder = new ValueHolder<>();
        this.context.addApplicationListener((ApplicationListener<RefreshRoutesEvent>)
                event -> testValueHolder.setValue(event));

        this.context.publishEvent(new EnvironmentChangeEvent(ofSet("enabled")));

        assertNull(testValueHolder.getValue());
    }
}