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

package io.microsphere.spring.cloud.gateway.server.webmvc.filter;


import io.microsphere.spring.cloud.client.event.ServiceInstancesChangedEvent;
import io.microsphere.spring.cloud.client.service.registry.DefaultRegistration;
import io.microsphere.spring.cloud.client.service.registry.event.RegistrationPreRegisteredEvent;
import io.microsphere.spring.test.web.controller.TestController;
import io.microsphere.spring.webmvc.annotation.EnableWebMvcExtension;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static io.microsphere.collection.Lists.ofList;
import static io.microsphere.collection.Sets.ofSet;
import static io.microsphere.spring.cloud.gateway.server.webmvc.constants.GatewayPropertyConstants.GATEWAY_ROUTES_PROPERTY_NAME_PREFIX;
import static io.microsphere.spring.cloud.gateway.server.webmvc.filter.WebEndpointMappingHandlerSupplier.getWebEndpointMappingHandlerFilterFunction;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.web.servlet.function.ServerRequest.create;

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

        @Autowired
        private ApplicationContext context;

        private final SimpleDiscoveryProperties simpleDiscoveryProperties;

        TestConfig(SimpleDiscoveryProperties simpleDiscoveryProperties) {
            this.simpleDiscoveryProperties = simpleDiscoveryProperties;
        }

        @EventListener(RegistrationPreRegisteredEvent.class)
        public void onRegistrationPreRegisteredEvent(RegistrationPreRegisteredEvent event) {
            register(event.getRegistration());
        }

        void register(Registration registration) {
            int localServerPort = this.environment.getProperty("local.server.port", int.class);
            DefaultRegistration defaultRegistration = (DefaultRegistration) registration;
            List<DefaultServiceInstance> instances = getInstances(registration.getServiceId());
            defaultRegistration.setHost("127.0.0.1");
            defaultRegistration.setPort(localServerPort);
            instances.add(defaultRegistration);
        }

        void deregister(Registration registration) {
            String serviceId = registration.getServiceId();
            List<DefaultServiceInstance> instances = getInstances(serviceId);
            instances.removeIf(instance -> instance.getServiceId().equals(serviceId));
        }

        List<DefaultServiceInstance> getInstances(String serviceId) {
            return simpleDiscoveryProperties.getInstances().computeIfAbsent(serviceId, k -> new ArrayList<>());
        }
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TestController testController;

    @Autowired
    private TestConfig testConfig;

    @Autowired
    private Registration registration;

    private MockMvc mockMvc;


    @BeforeEach
    void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void test() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        String content = restTemplate.getForObject(this.registration.getUri() + "/test/helloworld", String.class);
        assertEquals(this.testController.helloWorld(), content);
        this.mockMvc.perform(get("/we/test-app/test/helloworld"))
                .andExpect(status().isOk())
                .andExpect(content().string(this.testController.helloWorld()));

        assertThrows(Exception.class, () -> this.mockMvc.perform(
                get("/we/test-app/test/helloworld")
                        .header(CONTENT_TYPE, "application/json")
                        .header(ACCEPT, "plain/text")
        ));

        EnvironmentChangeEvent event = new EnvironmentChangeEvent(ofSet(GATEWAY_ROUTES_PROPERTY_NAME_PREFIX + "[0].id"));
        webApplicationContext.publishEvent(event);
        this.mockMvc.perform(get("/we/test-app/test/helloworld"))
                .andExpect(status().isOk())
                .andExpect(content().string(this.testController.helloWorld()));

        this.testConfig.deregister(this.registration);

        this.webApplicationContext.publishEvent(new ServiceInstancesChangedEvent(this.registration.getServiceId(), ofList(this.registration)));

        assertThrows(Exception.class, () -> this.mockMvc.perform(get("/we/test-app/test/helloworld")));

        testInternalMethods();
    }

    private void testInternalMethods() {
        String routeId = "web-endpoint-mapping";
        WebEndpointMappingHandlerFilterFunction function = getWebEndpointMappingHandlerFilterFunction(routeId);
        assertSame(emptySet(), function.buildExcludedRequestMappingInfoSet(new RouteProperties()));

        URI uri = URI.create("we://test-app");
        assertEquals(ofSet("test-app"), function.getSubscribedServices(uri));

        uri = URI.create("we://test-app-1");
        RouteProperties routeProperties = new RouteProperties();
        routeProperties.setUri(uri);
        assertTrue(function.buildRequestMappingContexts(routeProperties).isEmpty());


        routeProperties.setId(routeId + "[0]");
        function.refresh(routeProperties, this.webApplicationContext);

        HttpServletRequest request = new MockHttpServletRequest();
        function.excludedRequestMappingInfoSet = null;
        assertFalse(function.isExcludedRequest(request));

        function.requestMappingContexts = null;
        assertNull(function.getMatchingRequestMappingContext("test-app", routeId, create(request, emptyList())));
    }

    @Test
    public void testOnNotFound() {
        assertThrows(Exception.class, () -> this.mockMvc.perform(get("/we/ /test/helloworld")));
        assertThrows(Exception.class, () -> this.mockMvc.perform(get("/we/test-app2/test/helloworld")));
    }
}