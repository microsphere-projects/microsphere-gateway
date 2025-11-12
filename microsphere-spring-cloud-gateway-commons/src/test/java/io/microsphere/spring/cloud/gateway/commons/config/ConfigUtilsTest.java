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
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.microsphere.spring.cloud.gateway.commons.config.ConfigUtils.getWebEndpointConfig;
import static io.microsphere.spring.cloud.gateway.commons.constants.RouteConstants.WEB_ENDPOINT_KEY;
import static io.microsphere.util.ArrayUtils.EMPTY_STRING_ARRAY;
import static io.microsphere.util.ArrayUtils.ofArray;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.springframework.boot.context.properties.source.ConfigurationPropertySources.attach;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * {@link ConfigUtils} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see ConfigUtils
 * @since 1.0.0
 */
class ConfigUtilsTest {

    Environment environment;

    @BeforeEach
    void setUp() throws IOException {
        this.environment = createEnvironment();
    }

    public static Environment createEnvironment() throws IOException {
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:META-INF/config/default/test.yaml");
        YamlPropertySourceLoader propertySourceLoader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = propertySourceLoader.load(resource.toString(), resource);

        MockEnvironment environment = new MockEnvironment();
        for (PropertySource<?> propertySource : propertySources) {
            environment.getPropertySources().addFirst(propertySource);
        }

        attach(environment);
        return environment;
    }

    @Test
    void testGetWebEndpointConfig() {
        WebEndpointConfig config = getWebEndpointConfig(this.environment, "metadata.web-endpoint");
        assertWebEndpointConfig(config);

        Map<String, Object> metadata = new HashMap<>();
        config = getWebEndpointConfig(metadata);
        assertNull(config);

        metadata.put(WEB_ENDPOINT_KEY, metadata);
        config = getWebEndpointConfig(metadata);
        assertNull(config);

        metadata.put(WEB_ENDPOINT_KEY, getWebEndpointConfig(this.environment, "spring.cloud.gateway.routes[0].metadata.web-endpoint"));
        config = getWebEndpointConfig(metadata);
        assertNotNull(config);
    }

    @Test
    void testGetWebEndpointConfigOnMissing() {
        WebEndpointConfig config = getWebEndpointConfig(this.environment, "spring.cloud.gateway.routes[1].metadata.web-endpoint");
        assertNotNull(config);

        config = getWebEndpointConfig(this.environment, "spring.cloud.gateway.routes[1]");
        assertNull(config);
    }

    static void assertWebEndpointConfig(WebEndpointConfig config) {
        assertNotNull(config);
        List<WebEndpointConfig.Mapping> excludes = config.getExcludes();
        assertEquals(3, excludes.size());

        WebEndpointConfig.Mapping exclude = excludes.get(0);
        assertArrayEquals(ofArray("/test-1/**"), exclude.getPatterns());
        assertArrayEquals(ofArray(GET), exclude.getMethods());
        assertArrayEquals(ofArray("p=1"), exclude.getParams());
        assertSame(EMPTY_STRING_ARRAY, exclude.getHeaders());
        assertNull(exclude.getConsumes());
        assertNull(exclude.getProduces());

        exclude = excludes.get(1);
        assertArrayEquals(ofArray("/test-2/**"), exclude.getPatterns());
        assertArrayEquals(ofArray(POST), exclude.getMethods());
        assertNull(exclude.getParams());
        assertArrayEquals(ofArray("h=1"), exclude.getHeaders());
        assertNull(exclude.getConsumes());
        assertNull(exclude.getProduces());

        exclude = excludes.get(2);
        assertArrayEquals(ofArray("/test-3/abc/**"), exclude.getPatterns());
        assertArrayEquals(RequestMethod.values(), exclude.getMethods());
        assertNull(exclude.getParams());
        assertSame(EMPTY_STRING_ARRAY, exclude.getHeaders());
        assertArrayEquals(ofArray("application/json"), exclude.getConsumes());
        assertArrayEquals(ofArray("plain/text"), exclude.getProduces());
    }
}