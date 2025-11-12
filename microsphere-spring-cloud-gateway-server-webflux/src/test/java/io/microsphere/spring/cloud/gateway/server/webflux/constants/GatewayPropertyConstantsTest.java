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

package io.microsphere.spring.cloud.gateway.server.webflux.constants;


import org.junit.jupiter.api.Test;

import static io.microsphere.constants.PropertyConstants.ENABLED_PROPERTY_NAME;
import static io.microsphere.constants.SymbolConstants.DOT;
import static io.microsphere.spring.cloud.gateway.server.webflux.constants.GatewayPropertyConstants.GATEWAY_ENABLED_PROPERTY_NAME;
import static io.microsphere.spring.cloud.gateway.server.webflux.constants.GatewayPropertyConstants.GATEWAY_ROUTES_PROPERTY_NAME_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.cloud.gateway.config.GatewayProperties.PREFIX;

/**
 * {@link GatewayPropertyConstants} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see GatewayPropertyConstants
 * @since 1.0.0
 */
class GatewayPropertyConstantsTest {

    @Test
    void testConstants() {
        assertEquals(PREFIX + DOT + ENABLED_PROPERTY_NAME, GATEWAY_ENABLED_PROPERTY_NAME);
        assertEquals(PREFIX + DOT + "routes", GATEWAY_ROUTES_PROPERTY_NAME_PREFIX);
    }
}