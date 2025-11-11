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
import org.springframework.cloud.client.ConditionalOnReactiveDiscoveryEnabled;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryProperties;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.microsphere.collection.CollectionUtils.first;
import static java.net.URI.create;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
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
                WebEndpointMappingGlobalFilterTest.TestConfig.class
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

    @ConditionalOnReactiveDiscoveryEnabled
    static class TestConfig {

        @Autowired
        private Environment environment;

        private final SimpleReactiveDiscoveryProperties simpleDiscoveryProperties;

        TestConfig(SimpleReactiveDiscoveryProperties simpleDiscoveryProperties) {
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

        this.webTestClient.get().uri("/test-1/test/helloworld")
                .exchange()
                .expectStatus().isOk();

        this.webTestClient.get().uri("/test-2/test/helloworld")
                .exchange()
                .expectStatus().isOk();

        this.webTestClient.get().uri("/test-3/abc/def")
                .header(CONTENT_TYPE, "application/json")
                .header(ACCEPT, "plain/text")
                .exchange()
                .expectStatus().isOk();

        URI uri = create("we://test-app");
        Collection<String> subscribedServices = this.filter.getSubscribedServices(uri);
        assertEquals(1, subscribedServices.size());
        assertEquals("test-app", first(subscribedServices));
    }

    @Test
    @Order(2)
    void testFilterForUnregisteredApplication() {
        this.webTestClient.get().uri("/test-0/test/helloworld")
                .exchange()
                .expectStatus().isOk();

        assertNull(this.filter.getMatchingRequestMappingContext("test-2", "not-found", null));
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

    @Test
    @Order(5)
    void testFilterForExcludedRequests() {
        this.webTestClient.get().uri("/test-1/test/helloworld")
                .exchange()
                .expectStatus().isOk();

        this.webTestClient.get().uri("/test-2/test/helloworld")
                .exchange()
                .expectStatus().isOk();

        this.webTestClient.get().uri("/test-3/abc/def")
                .header(CONTENT_TYPE, "application/json")
                .header(ACCEPT, "plain/text")
                .exchange()
                .expectStatus().isOk();
    }
}