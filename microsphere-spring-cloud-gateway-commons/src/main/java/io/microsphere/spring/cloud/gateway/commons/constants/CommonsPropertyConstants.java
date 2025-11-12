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
package io.microsphere.spring.cloud.gateway.commons.constants;

import io.microsphere.annotation.ConfigurationProperty;

import static io.microsphere.annotation.ConfigurationProperty.APPLICATION_SOURCE;
import static io.microsphere.constants.PropertyConstants.ENABLED_PROPERTY_NAME;
import static io.microsphere.spring.cloud.commons.constants.CommonsPropertyConstants.MICROSPHERE_SPRING_CLOUD_PROPERTY_NAME_PREFIX;

/**
 * Gateway Commons Property Constants
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public interface CommonsPropertyConstants {

    /**
     * The prefix of Microsphere Gateway Spring Cloud property name : "microsphere.spring.cloud.gateway."
     */
    String MICROSPHERE_GATEWAY_SPRING_CLOUD_PROPERTY_NAME_PREFIX = MICROSPHERE_SPRING_CLOUD_PROPERTY_NAME_PREFIX + "gateway.";

    /**
     * The default value of Microsphere Gateway Spring Cloud Gateway enabled : "true"
     */
    String DEFAULT_MICROSPHERE_GATEWAY_ENABLED = "true";

    /**
     * The property name of Microsphere Gateway Spring Cloud Gateway enabled : "microsphere.spring.cloud.gateway.enabled"
     */
    @ConfigurationProperty(
            type = boolean.class,
            defaultValue = DEFAULT_MICROSPHERE_GATEWAY_ENABLED,
            source = APPLICATION_SOURCE
    )
    String MICROSPHERE_GATEWAY_ENABLED_PROPERTY_NAME = MICROSPHERE_GATEWAY_SPRING_CLOUD_PROPERTY_NAME_PREFIX + ENABLED_PROPERTY_NAME;

    /**
     * The prefix of Microsphere Web Endpoint Mapping property name : "microsphere.spring.cloud.web-endpoint-mapping."
     */
    String MICROSPHERE_WEB_ENDPOINT_MAPPING_PROPERTY_NAME_PREFIX = MICROSPHERE_SPRING_CLOUD_PROPERTY_NAME_PREFIX + "web-endpoint-mapping.";

    /**
     * The property name of Microsphere Web Endpoint Mapping enabled : "microsphere.spring.cloud.web-endpoint-mapping.enabled"
     */
    @ConfigurationProperty(
            type = boolean.class,
            defaultValue = DEFAULT_MICROSPHERE_GATEWAY_ENABLED,
            source = APPLICATION_SOURCE
    )
    String MICROSPHERE_WEB_ENDPOINT_MAPPING_ENABLED_PROPERTY_NAME = MICROSPHERE_WEB_ENDPOINT_MAPPING_PROPERTY_NAME_PREFIX + ENABLED_PROPERTY_NAME;
}