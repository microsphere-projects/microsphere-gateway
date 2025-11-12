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

package io.microsphere.spring.cloud.gateway.commons.constants;


import org.junit.jupiter.api.Test;

import static io.microsphere.spring.cloud.gateway.commons.constants.CommonsPropertyConstants.DEFAULT_MICROSPHERE_GATEWAY_ENABLED;
import static io.microsphere.spring.cloud.gateway.commons.constants.CommonsPropertyConstants.MICROSPHERE_GATEWAY_ENABLED_PROPERTY_NAME;
import static io.microsphere.spring.cloud.gateway.commons.constants.CommonsPropertyConstants.MICROSPHERE_GATEWAY_SPRING_CLOUD_PROPERTY_NAME_PREFIX;
import static io.microsphere.spring.cloud.gateway.commons.constants.CommonsPropertyConstants.MICROSPHERE_WEB_ENDPOINT_MAPPING_ENABLED_PROPERTY_NAME;
import static io.microsphere.spring.cloud.gateway.commons.constants.CommonsPropertyConstants.MICROSPHERE_WEB_ENDPOINT_MAPPING_PROPERTY_NAME_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link CommonsPropertyConstants} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see CommonsPropertyConstants
 * @since 1.0.0
 */
class CommonsPropertyConstantsTest {

    @Test
    void testConstants() {
        assertEquals("microsphere.spring.cloud.gateway.", MICROSPHERE_GATEWAY_SPRING_CLOUD_PROPERTY_NAME_PREFIX);
        assertEquals("true", DEFAULT_MICROSPHERE_GATEWAY_ENABLED);
        assertEquals("microsphere.spring.cloud.gateway.enabled", MICROSPHERE_GATEWAY_ENABLED_PROPERTY_NAME);
        assertEquals("microsphere.spring.cloud.web-endpoint-mapping.", MICROSPHERE_WEB_ENDPOINT_MAPPING_PROPERTY_NAME_PREFIX);
        assertEquals("microsphere.spring.cloud.web-endpoint-mapping.enabled", MICROSPHERE_WEB_ENDPOINT_MAPPING_ENABLED_PROPERTY_NAME);
    }
}