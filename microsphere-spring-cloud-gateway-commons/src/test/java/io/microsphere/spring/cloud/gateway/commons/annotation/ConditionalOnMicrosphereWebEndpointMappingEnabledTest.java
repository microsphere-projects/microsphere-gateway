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

package io.microsphere.spring.cloud.gateway.commons.annotation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link ConditionalOnMicrosphereWebEndpointMappingEnabled} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see ConditionalOnMicrosphereWebEndpointMappingEnabled
 * @since 1.0.0
 */
class ConditionalOnMicrosphereWebEndpointMappingEnabledTest {

    @Nested
    @SpringJUnitConfig(value = {
            Config.class
    })
    class DefaultTest {

        @Autowired
        private Config config;

        @Test
        void test() {
            assertNotNull(this.config);
        }

    }

    @Nested
    @SpringJUnitConfig(value = {
            Config.class
    })
    @TestPropertySource(properties = {
            "microsphere.spring.cloud.web-endpoint-mapping.enabled=true"
    })
    class EnabledTest {

        @Autowired
        private Config config;

        @Test
        void test() {
            assertNotNull(this.config);
        }

    }

    @Nested
    @SpringJUnitConfig(value = {
            Config.class
    })
    @TestPropertySource(properties = {
            "microsphere.spring.cloud.web-endpoint-mapping.enabled=false"
    })
    class DisabledTest {

        @Autowired(required = false)
        private Config config;

        @Test
        void test() {
            assertNull(this.config);
        }
    }

    @ConditionalOnMicrosphereWebEndpointMappingEnabled
    static class Config {
    }
}
