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

package io.microsphere.spring.cloud.gateway.commons.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.microsphere.spring.boot.context.properties.bind.util.BindUtils.bind;
import static io.microsphere.spring.cloud.gateway.commons.config.ConfigUtils.getWebEndpointConfig;
import static io.microsphere.spring.cloud.gateway.commons.config.ConfigUtilsTest.createEnvironment;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link WebEndpointConfigurationPropertiesBindListener} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebEndpointConfigurationPropertiesBindListener
 * @since 1.0.0
 */
class WebEndpointConfigurationPropertiesBindListenerTest {

    private Environment environment;

    private WebEndpointConfigurationPropertiesBindListener bindListener;

    @BeforeEach
    void setUp() throws IOException {
        this.environment = createEnvironment();

        this.bindListener = new WebEndpointConfigurationPropertiesBindListener("spring.cloud.gateway.routes");
        this.bindListener.setEnvironment(environment);
    }

    @Test
    void testApply() {
        Map<String, Object> result = bind(this.environment, "spring.cloud.gateway.routes[0].metadata", LinkedHashMap.class, this.bindListener);
        WebEndpointConfig config = getWebEndpointConfig(result);
        assertNotNull(config);

        bind(this.environment, "spring.cloud.gateway.routes[1].metadata", LinkedHashMap.class, this.bindListener);
        bind(this.environment, "spring.cloud.gateway.routes[2].metadata", LinkedHashMap.class, this.bindListener);
        bind(this.environment, "spring.cloud", LinkedHashMap.class, this.bindListener);
    }
}
