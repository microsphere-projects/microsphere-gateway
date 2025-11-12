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

package io.microsphere.spring.cloud.gateway.server.webflux.filter;

import io.microsphere.spring.cloud.gateway.server.webflux.filter.WebEndpointMappingGlobalFilter;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import java.net.URI;

import static io.microsphere.spring.cloud.client.service.registry.constants.InstanceConstants.WEB_CONTEXT_PATH_METADATA_NAME;
import static io.microsphere.spring.cloud.gateway.server.webflux.filter.WebEndpointMappingGlobalFilter.buildPath;
import static java.net.URI.create;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link WebEndpointMappingGlobalFilter} Static staff Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see WebEndpointMappingGlobalFilter
 * @see WebEndpointMappingGlobalFilter.RequestMappingContext
 * @since 1.0.0
 */
public class WebEndpointMappingGlobalFilterStaticTest {

    @Test
    void testBuildPath() {
        ServiceInstance serviceInstance = createServiceInstance();
        URI uri = serviceInstance.getUri();
        assertEquals("", buildPath(serviceInstance, uri));

        serviceInstance.getMetadata().put(WEB_CONTEXT_PATH_METADATA_NAME, "/");
        assertEquals("", buildPath(serviceInstance, uri));

        uri = create("we://all/test-app/test-path");
        assertEquals("/test-path", buildPath(serviceInstance, uri));
    }

    private ServiceInstance createServiceInstance() {
        DefaultServiceInstance serviceInstance = new DefaultServiceInstance();
        serviceInstance.setServiceId("test-app");
        serviceInstance.setSecure(true);
        serviceInstance.setHost("127.0.0.1");
        serviceInstance.setPort(8080);
        return serviceInstance;
    }
}