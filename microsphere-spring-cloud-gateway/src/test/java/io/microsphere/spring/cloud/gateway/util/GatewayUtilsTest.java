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

package io.microsphere.spring.cloud.gateway.util;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static io.microsphere.spring.cloud.gateway.util.GatewayUtils.getGatewayProperties;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.cloud.gateway.config.GatewayProperties.PREFIX;

/**
 * {@link GatewayUtils} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see GatewayUtils
 * @since 1.0.0
 */
class GatewayUtilsTest {

    private MockEnvironment mockEnvironment;

    @BeforeEach
    void setUp() {
        this.mockEnvironment = new MockEnvironment();
    }

    @Test
    void testGetGatewayProperties() {
        Map<String, Object> gatewayProperties = getGatewayProperties(mockEnvironment);
        assertTrue(gatewayProperties.isEmpty());

        mockEnvironment.setProperty(PREFIX + ".enabled", "true");
        gatewayProperties = getGatewayProperties(mockEnvironment);
        assertEquals(1, gatewayProperties.size());
        assertEquals("true", gatewayProperties.get("enabled"));
    }

    @Test
    void testGetRouteProperties() {
    }
}