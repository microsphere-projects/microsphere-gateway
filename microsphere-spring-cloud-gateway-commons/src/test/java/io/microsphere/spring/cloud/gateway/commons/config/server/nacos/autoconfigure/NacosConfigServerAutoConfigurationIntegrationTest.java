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

package io.microsphere.spring.cloud.gateway.commons.config.server.nacos.autoconfigure;


import io.microsphere.spring.cloud.gateway.commons.config.server.nacos.environment.NacosEnvironmentRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testcontainers.containers.wait.strategy.Wait.forLogMessage;

/**
 * {@link NacosConfigServerAutoConfiguration} Integration Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see NacosConfigServerAutoConfiguration
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "testcontainers.enabled", matches = "true")
@EnabledIfDockerAvailable
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        NacosConfigServerAutoConfigurationIntegrationTest.class
})
@TestPropertySource(properties = {
        "spring.application.name=test",
})
@EnableAutoConfiguration
class NacosConfigServerAutoConfigurationIntegrationTest {

    private static ComposeContainer composeContainer;

    @BeforeAll
    static void beforeAll() throws Exception {
        ClassLoader classLoader = NacosConfigServerAutoConfigurationIntegrationTest.class.getClassLoader();
        URL resource = classLoader.getResource("META-INF/docker/service-registry-servers.yml");
        File dockerComposeFile = new File(resource.toURI());
        composeContainer = new ComposeContainer(dockerComposeFile);
        composeContainer.waitingFor("nacos", forLogMessage(".*started successfully.*", 1))
                .start();
    }

    @AfterAll
    static void afterAll() {
        composeContainer.stop();
    }

    @Autowired
    private NacosEnvironmentRepository nacosEnvironmentRepository;

    @Test
    void test() {
        Environment environment = nacosEnvironmentRepository.findOne("test", "default", null);
        assertNotNull(environment);
    }
}