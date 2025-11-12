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
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.microsphere.spring.cloud.gateway.commons.config.ConfigUtils.getWebEndpointConfig;
import static io.microsphere.spring.cloud.gateway.commons.config.ConfigUtilsTest.createEnvironment;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.context.properties.bind.BindHandler.DEFAULT;
import static org.springframework.boot.context.properties.bind.Bindable.of;
import static org.springframework.boot.context.properties.bind.Binder.get;

/**
 * {@link WebEndpointConfigurationPropertiesBindHandlerAdvisor} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebEndpointConfigurationPropertiesBindHandlerAdvisor
 * @since 1.0.0
 */
class WebEndpointConfigurationPropertiesBindHandlerAdvisorTest {

    private Environment environment;

    private WebEndpointConfigurationPropertiesBindHandlerAdvisor advisor;

    @BeforeEach
    void setUp() throws IOException {
        this.environment = createEnvironment();

        this.advisor = new WebEndpointConfigurationPropertiesBindHandlerAdvisor("spring.cloud.gateway.routes");
        this.advisor.setEnvironment(environment);
    }

    @Test
    void testApply() {
        BindHandler bindHandler = this.advisor.apply(DEFAULT);
        Binder binder = get(environment, bindHandler);
        Bindable<LinkedHashMap> bindable = of(LinkedHashMap.class);
        Map<String, Object> result = binder.bind("spring.cloud.gateway.routes[0].metadata", bindable).get();
        WebEndpointConfig config = getWebEndpointConfig(result);
        assertNotNull(config);

        binder.bind("spring.cloud.gateway.routes[1].metadata", bindable);
        binder.bind("spring.cloud.gateway.routes[2].metadata", bindable);
        binder.bind("spring.cloud", bindable);
    }
}