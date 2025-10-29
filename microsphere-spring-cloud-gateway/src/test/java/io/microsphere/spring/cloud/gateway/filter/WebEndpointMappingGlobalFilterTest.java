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

package io.microsphere.spring.cloud.gateway.filter;


import io.microsphere.spring.cloud.client.service.registry.DefaultRegistration;
import io.microsphere.spring.cloud.client.service.registry.event.RegistrationPreRegisteredEvent;
import io.microsphere.spring.test.web.controller.TestController;
import io.microsphere.spring.webflux.annotation.EnableWebFluxExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.reactive.server.WebTestClient.bindToApplicationContext;

/**
 * {@link WebEndpointMappingGlobalFilter} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebEndpointMappingGlobalFilter
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                TestController.class,
                WebEndpointMappingGlobalFilterTest.class,
                WebEndpointMappingGlobalFilterTest.Config.class
        },
        properties = {
                "spring.profiles.active=simple-service-registry,gateway"
        },
        webEnvironment = RANDOM_PORT
)
@EnableAutoConfiguration
@EnableWebFluxExtension
@TestMethodOrder(OrderAnnotation.class)
class WebEndpointMappingGlobalFilterTest {

    static class Config {

        @Autowired
        private Environment environment;

        private final SimpleDiscoveryProperties simpleDiscoveryProperties;

        Config(SimpleDiscoveryProperties simpleDiscoveryProperties) {
            this.simpleDiscoveryProperties = simpleDiscoveryProperties;
        }

        @EventListener(RegistrationPreRegisteredEvent.class)
        public void onRegistrationPreRegisteredEvent(RegistrationPreRegisteredEvent event) {
            int localServerPort = this.environment.getProperty("local.server.port", int.class);
            DefaultRegistration registration = (DefaultRegistration) event.getRegistration();
            Map<String, List<DefaultServiceInstance>> instancesMap = simpleDiscoveryProperties.getInstances();
            List<DefaultServiceInstance> instances = instancesMap.computeIfAbsent(registration.getServiceId(), k -> new ArrayList<>());
            registration.setPort(localServerPort);
            instances.add(registration);
        }
    }

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private WebEndpointMappingGlobalFilter filter;

    @Autowired
    private TestController testController;

    @Autowired
    private Registration registration;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        this.webTestClient = bindToApplicationContext(context)
                .build();
    }

    @Test
    @Order(1)
    void testFilter() {
        this.webTestClient.get().uri("/test-app/test/helloworld")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo(testController.helloWorld());
    }

    @Test
    @Order(2)
    void testFilterForUnregisteredApplication() {
        this.webTestClient.get().uri("/test/test/helloworld")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @Order(3)
    void testFilterWithoutRoutedRequestMappingContexts() {
        this.filter.clear();
        this.filter.destroy();
        this.webTestClient.get().uri("/test-app/test/helloworld")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @Order(4)
    void testFilterForNoGateway() {
        this.webTestClient.get().uri("/test/helloworld")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo(testController.helloWorld());
    }
}