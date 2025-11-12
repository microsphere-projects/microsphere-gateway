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

import org.springframework.boot.context.properties.ConfigurationPropertiesBindHandlerAdvisor;
import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.Map;

import static io.microsphere.spring.cloud.gateway.commons.config.ConfigUtils.getWebEndpointConfig;
import static io.microsphere.spring.cloud.gateway.commons.constants.RouteConstants.METADATA_KEY;
import static io.microsphere.spring.cloud.gateway.commons.constants.RouteConstants.WEB_ENDPOINT_KEY;
import static io.microsphere.util.Assert.assertNotBlank;

/**
 * The {@link ConfigurationPropertiesBindHandlerAdvisor} class for {@link WebEndpointConfig}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see ConfigurationPropertiesBindHandlerAdvisor
 * @see WebEndpointConfig
 * @since 1.0.0
 */
public class WebEndpointConfigurationPropertiesBindHandlerAdvisor implements ConfigurationPropertiesBindHandlerAdvisor,
        EnvironmentAware {

    private final String gatewayRoutesPropertyNamePrefix;

    private Environment environment;

    public WebEndpointConfigurationPropertiesBindHandlerAdvisor(String gatewayRoutesPropertyNamePrefix) {
        assertNotBlank(gatewayRoutesPropertyNamePrefix, () -> "The 'gatewayRoutesPropertyNamePrefix' must not be blank");
        this.gatewayRoutesPropertyNamePrefix = gatewayRoutesPropertyNamePrefix;
    }

    @Override
    public BindHandler apply(BindHandler bindHandler) {
        return new AbstractBindHandler(bindHandler) {
            @Override
            public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
                String propertyName = name.toString();
                if (propertyName.startsWith(gatewayRoutesPropertyNamePrefix) && propertyName.endsWith(METADATA_KEY) && result != null) {
                    ConfigurationPropertyName webEndpointName = name.append(WEB_ENDPOINT_KEY);
                    WebEndpointConfig webEndpointConfig = getWebEndpointConfig(environment, webEndpointName.toString());
                    if (webEndpointConfig != null) {
                        Map<String, Object> metadata = (Map<String, Object>) result;
                        metadata.put(WEB_ENDPOINT_KEY, webEndpointConfig);
                    }
                }
            }
        };
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}