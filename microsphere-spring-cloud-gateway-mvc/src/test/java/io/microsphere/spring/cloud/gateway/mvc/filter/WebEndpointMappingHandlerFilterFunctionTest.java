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

package io.microsphere.spring.cloud.gateway.mvc.filter;


import io.microsphere.spring.cloud.client.service.registry.DefaultRegistration;
import io.microsphere.spring.cloud.client.service.registry.event.RegistrationPreRegisteredEvent;
import io.microsphere.spring.test.web.controller.TestController;
import io.microsphere.spring.webmvc.annotation.EnableWebMvcExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * {@link WebEndpointMappingHandlerFilterFunction} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebEndpointMappingHandlerFilterFunction
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                TestController.class,
                WebEndpointMappingHandlerFilterFunctionTest.class,
                WebEndpointMappingHandlerFilterFunctionTest.TestConfig.class
        },
        properties = {
                "spring.profiles.active=simple-service-registry,gateway"
        },
        webEnvironment = RANDOM_PORT
)
@EnableAutoConfiguration
@EnableWebMvcExtension
@DirtiesContext
class WebEndpointMappingHandlerFilterFunctionTest {

    @ConditionalOnBlockingDiscoveryEnabled
    static class TestConfig {

        @Autowired
        private Environment environment;

        private final SimpleDiscoveryProperties simpleDiscoveryProperties;

        TestConfig(SimpleDiscoveryProperties simpleDiscoveryProperties) {
            this.simpleDiscoveryProperties = simpleDiscoveryProperties;
        }

        @EventListener(RegistrationPreRegisteredEvent.class)
        public void onRegistrationPreRegisteredEvent(RegistrationPreRegisteredEvent event) {
            int localServerPort = this.environment.getProperty("local.server.port", int.class);
            DefaultRegistration registration = (DefaultRegistration) event.getRegistration();
            Map<String, List<DefaultServiceInstance>> instancesMap = new HashMap<>();
            List<DefaultServiceInstance> instances = instancesMap.computeIfAbsent(registration.getServiceId(), k -> new ArrayList<>());
            registration.setPort(localServerPort);
            instances.add(registration);
            instances.add(registration);
            instances.add(registration);
            simpleDiscoveryProperties.setInstances(instancesMap);
        }
    }


    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TestController testController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }


    @Test
    public void test() throws Exception {
        this.mockMvc.perform(get("/we/test-app/test/helloworld"))
                .andExpect(status().isOk())
                .andExpect(content().string(this.testController.helloWorld()));
    }

    @Test
    public void testLoadBalancer() throws Exception {
        this.mockMvc.perform(get("/api/test/helloworld"))
                .andExpect(status().isOk())
                .andExpect(content().string(this.testController.helloWorld()));
    }
}