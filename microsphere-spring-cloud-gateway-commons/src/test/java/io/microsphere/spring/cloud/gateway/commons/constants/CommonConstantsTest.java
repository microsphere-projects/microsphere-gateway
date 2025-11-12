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

import static io.microsphere.spring.cloud.gateway.commons.constants.CommonConstants.APPLICATION_NAME_URI_TEMPLATE_VARIABLE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link CommonConstants}  Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see CommonConstants
 * @since 1.0.0
 */
class CommonConstantsTest {

    @Test
    void testConstants() {
        assertEquals("application", APPLICATION_NAME_URI_TEMPLATE_VARIABLE_NAME);
    }
}