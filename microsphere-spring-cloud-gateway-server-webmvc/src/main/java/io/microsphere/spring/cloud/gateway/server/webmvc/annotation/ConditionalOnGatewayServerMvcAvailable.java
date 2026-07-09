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

package io.microsphere.spring.cloud.gateway.server.webmvc.annotation;

import io.microsphere.spring.boot.webmvc.autoconfigure.condition.ConditionalOnWebMvcAvailable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The conditional annotation meta-annotates {@link ConditionalOnProperty @ConditionalOnProperty} for
 * Spring Cloud Gateway Server MVC available.
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see ConditionalOnProperty
 * @see ConditionalOnClass
 * @see org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties
 * @since 1.0.0
 */
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Documented
@ConditionalOnClass(name = {
        "org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties"       // Spring Cloud Gateway Server MVC API
})
@ConditionalOnWebMvcAvailable
@ConditionalOnGatewayServerMvcEnabled
public @interface ConditionalOnGatewayServerMvcAvailable {
}
