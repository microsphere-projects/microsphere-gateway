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

import io.microsphere.annotation.Nullable;
import io.microsphere.util.Utils;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.validation.ValidationBindHandler;
import org.springframework.core.env.Environment;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Map;

import static io.microsphere.spring.cloud.gateway.commons.constants.RouteConstants.WEB_ENDPOINT_KEY;
import static org.springframework.boot.context.properties.bind.Bindable.of;
import static org.springframework.boot.context.properties.bind.Binder.get;

/**
 * The utilities class for Configuration
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebEndpointConfig
 * @since 1.0.0
 */
public abstract class ConfigUtils implements Utils {

    private static final BindHandler springValidatorBindHandler;

    private static final Bindable<WebEndpointConfig> WEB_ENDPOINT_CONFIG_BINDABLE = of(WebEndpointConfig.class);

    static {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        springValidatorBindHandler = new ValidationBindHandler(validator);
    }

    /**
     * Get the {@link WebEndpointConfig} from the specified {@link Environment}
     *
     * @param environment  the specified {@link Environment}
     * @param configPrefix the configuration prefix
     * @return If the configuration is not present , return <code>null</code>
     */
    @Nullable
    public static WebEndpointConfig getWebEndpointConfig(Environment environment, String configPrefix) {
        Binder binder = get(environment, springValidatorBindHandler);
        return binder.bind(configPrefix, WEB_ENDPOINT_CONFIG_BINDABLE).orElse(null);
    }

    /**
     * Get the {@link WebEndpointConfig} from the specified metadata
     *
     * @param metadata the specified {@link Map}
     * @return If the configuration is not present , return <code>null</code>
     */
    @Nullable
    public static WebEndpointConfig getWebEndpointConfig(Map<String, Object> metadata) {
        Object webEndpoint = metadata.get(WEB_ENDPOINT_KEY);
        return webEndpoint instanceof WebEndpointConfig ? (WebEndpointConfig) webEndpoint : null;
    }

    private ConfigUtils() {
    }
}
