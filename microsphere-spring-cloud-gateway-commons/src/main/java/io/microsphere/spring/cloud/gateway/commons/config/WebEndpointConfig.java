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

import io.microsphere.annotation.Nonnull;
import io.microsphere.annotation.Nullable;
import io.microsphere.spring.web.metadata.WebEndpointMapping;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.LinkedList;
import java.util.List;

import static io.microsphere.util.ArrayUtils.EMPTY_STRING_ARRAY;
import static io.microsphere.util.ArrayUtils.isEmpty;
import static org.springframework.web.bind.annotation.RequestMethod.values;

/**
 * The config of Web Endpoint for Microsphere Spring Cloud Gateway
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebEndpointMapping
 * @since 1.0.0
 */
@Validated
public class WebEndpointConfig {

    @NotNull
    @Valid
    private List<Mapping> excludes = new LinkedList<>();

    public List<Mapping> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<Mapping> excludes) {
        this.excludes = excludes;
    }

    /**
     * @see WebEndpointMapping
     */
    public static class Mapping {

        @NotNull
        @Valid
        private String[] patterns;

        @Nullable
        private RequestMethod[] methods;

        @Nullable
        private String[] params;

        @Nullable
        private String[] headers;

        @Nullable
        private String[] consumes;

        @Nullable
        private String[] produces;

        public String[] getPatterns() {
            return patterns;
        }

        public void setPatterns(String[] patterns) {
            this.patterns = patterns;
        }

        @Nonnull
        public RequestMethod[] getMethods() {
            if (isEmpty(methods)) {
                return values();
            }
            return methods;
        }

        public void setMethods(@Nullable RequestMethod[] methods) {
            this.methods = methods;
        }

        @Nullable
        public String[] getParams() {
            return params;
        }

        public void setParams(@Nullable String[] params) {
            this.params = params;
        }

        @Nonnull
        public String[] getHeaders() {
            if (isEmpty(headers)) {
                return EMPTY_STRING_ARRAY;
            }
            return headers;
        }

        public void setHeaders(@Nullable String[] headers) {
            this.headers = headers;
        }

        @Nullable
        public String[] getConsumes() {
            return consumes;
        }

        public void setConsumes(@Nullable String[] consumes) {
            this.consumes = consumes;
        }

        @Nullable
        public String[] getProduces() {
            return produces;
        }

        public void setProduces(@Nullable String[] produces) {
            this.produces = produces;
        }
    }
}
