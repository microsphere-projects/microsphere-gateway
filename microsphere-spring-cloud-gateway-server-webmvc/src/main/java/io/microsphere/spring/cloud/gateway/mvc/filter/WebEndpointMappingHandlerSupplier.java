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

package io.microsphere.spring.cloud.gateway.mvc.filter;

import io.microsphere.spring.web.metadata.WebEndpointMapping;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerDiscoverer.Result;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerSupplier;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static io.microsphere.collection.Lists.ofList;
import static java.util.Collections.emptyList;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

/**
 * The {@link HandlerSupplier} for {@link WebEndpointMapping}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see HandlerSupplier
 * @see WebEndpointMapping
 * @since 1.0.0
 */
public class WebEndpointMappingHandlerSupplier implements HandlerSupplier {

    private static final Map<String, WebEndpointMappingHandlerFilterFunction> handlerFilterFunctionsCache = new HashMap<>();

    @Override
    public Collection<Method> get() {
        return ofList(getClass().getMethods());
    }

    public static Result we(RouteProperties routeProperties) {
        String routeId = routeProperties.getId();
        WebEndpointMappingHandlerFilterFunction function = handlerFilterFunctionsCache.computeIfAbsent(routeId,
                id -> new WebEndpointMappingHandlerFilterFunction(routeProperties));
        return new Result(http(),
                ofList(function),
                emptyList());
    }

    public static WebEndpointMappingHandlerFilterFunction getWebEndpointMappingHandlerFilterFunction(String routeId) {
        return handlerFilterFunctionsCache.get(routeId);
    }
}
