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
package io.microsphere.spring.cloud.gateway.server.webflux.constants;

import io.microsphere.annotation.ConfigurationProperty;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;

import static io.microsphere.annotation.ConfigurationProperty.APPLICATION_SOURCE;
import static io.microsphere.constants.PropertyConstants.ENABLED_PROPERTY_NAME;
import static io.microsphere.constants.SymbolConstants.DOT;
import static io.microsphere.spring.cloud.gateway.commons.constants.CommonsPropertyConstants.DEFAULT_MICROSPHERE_GATEWAY_ENABLED;
import static org.springframework.cloud.gateway.config.GatewayProperties.PREFIX;

/**
 * Gateway Property Constants
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public interface GatewayPropertyConstants {

    /**
     * The property name of enabling Spring Cloud Server WebFlux Gateway: "spring.cloud.gateway.server.webflux.enabled"
     *
     * @see GatewayProperties#PREFIX
     */
    @ConfigurationProperty(
            type = boolean.class,
            defaultValue = DEFAULT_MICROSPHERE_GATEWAY_ENABLED,
            source = APPLICATION_SOURCE
    )
    String GATEWAY_ENABLED_PROPERTY_NAME = PREFIX + DOT + ENABLED_PROPERTY_NAME;

    /**
     * The property name prefix of Gateway {@link GatewayProperties#getRoutes() Routes}
     *
     * @see GatewayProperties#getRoutes()
     */
    @ConfigurationProperty(
            type = RouteDefinition.class,
            source = APPLICATION_SOURCE
    )
    String GATEWAY_ROUTES_PROPERTY_NAME_PREFIX = PREFIX + DOT + "routes";
}
