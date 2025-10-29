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

package io.microsphere.spring.cloud.gateway.autoconfigure;


import io.microsphere.spring.cloud.client.service.registry.DefaultRegistration;
import io.microsphere.spring.cloud.client.service.registry.event.RegistrationPreRegisteredEvent;
import io.microsphere.spring.cloud.gateway.filter.WebEndpointMappingGlobalFilter;
import io.microsphere.spring.cloud.gateway.handler.ServiceInstancePredicate;
import io.microsphere.spring.test.web.controller.TestController;
import io.microsphere.spring.webflux.annotation.EnableWebFluxExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.get;
import static org.springframework.mock.web.server.MockServerWebExchange.from;
import static org.springframework.test.web.reactive.server.WebTestClient.bindToApplicationContext;

/**
 * {@link WebEndpointMappingGatewayAutoConfiguration} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebEndpointMappingGatewayAutoConfiguration
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {
                TestController.class,
                WebEndpointMappingGatewayAutoConfigurationTest.class,
                WebEndpointMappingGatewayAutoConfigurationTest.Config.class
        },
        properties = {
                "spring.profiles.active=simple-service-registry,gateway"
        },
        webEnvironment = RANDOM_PORT
)
@EnableAutoConfiguration
@EnableWebFluxExtension
class WebEndpointMappingGatewayAutoConfigurationTest {

    @Autowired
    private ServiceInstancePredicate serviceInstancePredicate;

    @Autowired
    private WebEndpointMappingGlobalFilter webEndpointMappingGlobalFilter;

    @Autowired
    private ConfigurableApplicationContext context;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        this.webTestClient = bindToApplicationContext(context)
                .build();
    }

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

    @Test
    void testServiceInstancePredicate() {
        testServiceInstancePredicate("/test/abc", "test", true);
        testServiceInstancePredicate("/test/abc", "TEST", true);
        testServiceInstancePredicate("/test/abc", "Test", true);
        testServiceInstancePredicate("/test/abc", "t", false);
        testServiceInstancePredicate("/", "t", false);
        testServiceInstancePredicate("", "t", false);
        testServiceInstancePredicate("/a", "t", false);
    }

    @Test
    void testRequestWebEndpointMappingGlobalFilter() {
        this.webTestClient.get().uri("/test-app/test/helloworld")
                .exchange()
                .expectStatus().isOk()
                .expectBody();
    }

    void testServiceInstancePredicate(String path, String serviceId, boolean expected) {
        MockServerHttpRequest request = get(path).build();
        MockServerWebExchange serverWebExchange = from(request);
        DefaultServiceInstance instance = new DefaultServiceInstance();
        instance.setServiceId(serviceId);
        assertEquals(expected, serviceInstancePredicate.test(serverWebExchange, instance));
    }
}