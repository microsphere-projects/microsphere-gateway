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

package io.microsphere.spring.cloud.gateway.server.webflux.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates that the annotated element is only available when Spring Cloud Gateway is available.
 * <h3>Example Usage</h3>
 * <h4>Example usage on a configuration class:</h4>
 * <pre>{@code
 * @Configuration
 * @ConditionalOnGatewayAvailable
 * public class MyGatewayConfig {
 *     // ...
 * }
 * }</pre>
 *
 * <h4>Example usage on a bean method:</h4>
 * <pre>{@code
 * @Bean
 * @ConditionalOnGatewayAvailable
 * public MyCustomFilter myCustomFilter() {
 *     return new MyCustomFilter();
 * }
 * }</pre>
 *
 * @see ConditionalOnGatewayEnabled
 * @see ConditionalOnProperty
 * @see ConditionalOnClass
 * @see org.springframework.cloud.gateway.config.GatewayProperties
 * @since 1.0.0
 */
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Documented
@ConditionalOnGatewayEnabled
@ConditionalOnClass(name = {
        "org.springframework.cloud.gateway.config.GatewayProperties"    // Spring Cloud Gateway Server WebFlux API
})
public @interface ConditionalOnGatewayAvailable {
}
