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

import static io.microsphere.spring.cloud.gateway.commons.constants.RouteConstants.ALL_SERVICES;
import static io.microsphere.spring.cloud.gateway.commons.constants.RouteConstants.ID_KEY;
import static io.microsphere.spring.cloud.gateway.commons.constants.RouteConstants.METADATA_KEY;
import static io.microsphere.spring.cloud.gateway.commons.constants.RouteConstants.SCHEME;
import static io.microsphere.spring.cloud.gateway.commons.constants.RouteConstants.WEB_ENDPOINT_KEY;
import static io.microsphere.spring.cloud.gateway.commons.constants.RouteConstants.WEB_ENDPOINT_REWRITE_PATH_ATTRIBUTE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link RouteConstants} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see RouteConstants
 * @since 1.0.0
 */
class RouteConstantsTest {

    @Test
    void testConstants() {
        assertEquals("we", SCHEME);
        assertEquals("all", ALL_SERVICES);
        assertEquals("id", ID_KEY);
        assertEquals("metadata", METADATA_KEY);
        assertEquals("web-endpoint", WEB_ENDPOINT_KEY);
        assertEquals("msgw-we-rewrite-path", WEB_ENDPOINT_REWRITE_PATH_ATTRIBUTE_NAME);
    }
}