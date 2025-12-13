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

package io.microsphere.spring.cloud.gateway.commons.config.server.nacos.environment;


import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import static io.microsphere.lang.function.ThrowableSupplier.execute;
import static java.lang.String.format;
import static org.springframework.util.StringUtils.hasText;

/**
 * Nacos {@link EnvironmentRepository}.
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 0.2.0
 */
public class NacosEnvironmentRepository implements EnvironmentRepository {

    private final NacosConfigProperties nacosConfigProperties;

    private final ConfigService configService;

    public NacosEnvironmentRepository(NacosConfigManager nacosConfigManager, NacosConfigProperties nacosConfigProperties) {
        this.nacosConfigProperties = nacosConfigProperties;
        this.configService = nacosConfigManager.getConfigService();
    }

    @Override
    public Environment findOne(String application, String profile, String label) {

        String dataId = resolveDataId(application, profile);

        String configContent = execute(() -> configService.getConfig(dataId, nacosConfigProperties.getGroup(), this.nacosConfigProperties.getTimeout()));

        return createEnvironment(configContent, application, profile);
    }

    protected String resolveDataId(String application, String profile) {
        return application + "-" + resolveProfile(profile) + ".properties";
    }

    protected String resolveProfile(String profile) {
        return hasText(profile) ? profile : "default";
    }

    private Environment createEnvironment(String configContent, String application,
                                          String profile) {

        Environment environment = new Environment(application, profile);

        Properties properties = createProperties(configContent);

        String propertySourceName = format("Nacos[application : '%s' , profile : '%s']", application, profile);

        PropertySource propertySource = new PropertySource(propertySourceName,
                properties);

        environment.add(propertySource);

        return environment;
    }

    private Properties createProperties(String configContent) {
        Properties properties = new Properties();
        if (hasText(configContent)) {
            try {
                properties.load(new StringReader(configContent));
            } catch (IOException e) {
                throw new IllegalStateException("The format of content is a properties");
            }
        }
        return properties;
    }
}