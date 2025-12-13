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


import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.cloud.nacos.NacosConfigProperties;
import io.microsphere.spring.cloud.gateway.commons.config.server.nacos.environment.NacosEnvironmentRepository;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.springframework.cloud.config.server.config.ConfigServerProperties.PREFIX;

/**
 * Nacos Config Server Auto-Configuration.
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
@ConditionalOnClass(name = "org.springframework.cloud.config.server.EnableConfigServer")
@ConditionalOnProperty(prefix = PREFIX, name = "enabled", matchIfMissing = true)
@ConditionalOnProperty(prefix = "spring.nacos.config", name = "enabled", matchIfMissing = true)
@AutoConfigureBefore(name = "org.springframework.cloud.config.server.config.ConfigServerAutoConfiguration")
@AutoConfigureAfter(name = "com.alibaba.cloud.nacos.NacosConfigAutoConfiguration")
@Configuration(proxyBeanMethods = false)
public class NacosConfigServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(value = {NacosConfigManager.class, NacosConfigProperties.class})
    public NacosEnvironmentRepository nacosEnvironmentRepository(NacosConfigManager nacosConfigManager,
                                                                 NacosConfigProperties nacosConfigProperties) {
        return new NacosEnvironmentRepository(nacosConfigManager, nacosConfigProperties);
    }
}