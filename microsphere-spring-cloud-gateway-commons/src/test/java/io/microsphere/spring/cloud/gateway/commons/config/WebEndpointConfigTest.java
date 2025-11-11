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


import io.microsphere.spring.cloud.gateway.commons.config.WebEndpointConfig.Mapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.validation.ValidationBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.util.List;

import static io.microsphere.util.ArrayUtils.ofArray;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.boot.context.properties.bind.Bindable.of;
import static org.springframework.boot.context.properties.source.ConfigurationPropertySources.from;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * {@link WebEndpointConfig} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebEndpointConfig
 * @since 1.0.0
 */
class WebEndpointConfigTest {


    private Iterable<ConfigurationPropertySource> configurationPropertySources;

    private BindHandler bindHandler;

    @BeforeEach
    void setUp() throws IOException {
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:META-INF/config/default/test.yaml");
        YamlPropertySourceLoader propertySourceLoader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = propertySourceLoader.load(resource.toString(), resource);
        this.configurationPropertySources = from(propertySources);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        this.bindHandler = new ValidationBindHandler(validator);
    }

    @Test
    void test() {
        Binder binder = new Binder(this.configurationPropertySources);
        Bindable<WebEndpointConfig> bindable = of(WebEndpointConfig.class);
        WebEndpointConfig config = binder.bind("metadata.web-endpoint", bindable, bindHandler).get();
        assertNotNull(config);
        List<Mapping> excludes = config.getExcludes();
        assertEquals(3, excludes.size());

        Mapping exclude = excludes.get(0);
        assertArrayEquals(ofArray("/test-1/**"), exclude.getPatterns());
        assertArrayEquals(ofArray(GET), exclude.getMethods());
        assertArrayEquals(ofArray("p=1"), exclude.getParams());
        assertNull(exclude.getHeaders());
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
        assertNull(exclude.getHeaders());
        assertArrayEquals(ofArray("application/json"), exclude.getConsumes());
        assertArrayEquals(ofArray("plain/text"), exclude.getProduces());
    }
}