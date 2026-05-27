# Copilot Instructions for `microsphere-gateway`

## Build and test commands

Use the Maven wrapper from the repository root.

```bash
# Package the whole reactor
./mvnw --batch-mode -Drevision=0.0.1-SNAPSHOT -Ptest,spring-cloud-2025 package

# Run the full test suite
./mvnw --batch-mode -Drevision=0.0.1-SNAPSHOT -Ptest,spring-cloud-2025 test

# Run the same style of command as CI for one Spring Cloud profile
./mvnw --batch-mode --update-snapshots --file pom.xml -Drevision=0.0.1-SNAPSHOT -Ptest,coverage,spring-cloud-2025 test

# Run a single test class in one module
./mvnw --batch-mode -Drevision=0.0.1-SNAPSHOT \
  -pl microsphere-spring-cloud-gateway-server-webflux -am \
  -Ptest,spring-cloud-2025 \
  -Dtest=GatewayPropertyConstantsTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

CI runs Java 17, 21, and 25 across `spring-cloud-2022`, `spring-cloud-2023`, `spring-cloud-2024`, and `spring-cloud-2025`.

There is no dedicated lint task configured in this repository.

## High-level architecture

This repository is a multi-module Maven build:

- `microsphere-gateway-parent` imports shared Microsphere Spring Cloud dependency management.
- `microsphere-gateway-dependencies` is the BOM consumers import.
- `microsphere-spring-cloud-gateway-commons` holds shared route constants, feature-toggle annotations, and `WebEndpointConfig` binding for `metadata.web-endpoint.excludes`.
- `microsphere-spring-cloud-gateway-server-webflux` integrates with Spring Cloud Gateway reactive infrastructure.
- `microsphere-spring-cloud-gateway-server-webmvc` integrates with Spring Cloud Gateway Server MVC.

The central feature is the custom `we://` route scheme. A route like `we://all` subscribes to discovered services, reads each service's published `WebEndpointMapping` metadata, matches the incoming request against those endpoint mappings, rewrites the request path, and forwards the request to the selected service instance. Per-route exclusions come from `metadata.web-endpoint.excludes`.

At a high level, request handling works like this:

1. A gateway route with URI scheme `we` is selected by Spring Cloud Gateway.
2. Microsphere inspects the `{application}` path variable and the route ID.
3. It loads cached `WebEndpointMapping` metadata from discovered service instances for the subscribed services.
4. It filters out requests matched by `metadata.web-endpoint.excludes`.
5. It finds the best matching endpoint mapping, strips the leading `/{application}` segment, stores the rewritten path in a request attribute, and forwards the request through the normal load-balancer path.
6. It adds the `WebEndpointMapping` ID as a request header so the target service can identify the matched endpoint definition.

Configuration comes from three places that need to stay in sync when features change:

- gateway route definitions (`spring.cloud.gateway.routes` for WebFlux and `spring.cloud.gateway.mvc.routes` for MVC),
- Microsphere feature flags under `microsphere.spring.cloud.*`,
- service-instance metadata published by other Microsphere service-registry components.

The WebFlux and WebMVC modules implement the same behavior through different extension points:

- **WebFlux** registers `GatewayAutoConfiguration` and `WebEndpointMappingGatewayAutoConfiguration` through `AutoConfiguration.imports`.
  - `WebEndpointMappingGlobalFilter` runs just before the reactive load-balancer filter, caches request-mapping metadata per gateway route, and refreshes on `ContextRefreshedEvent`, `RefreshRoutesResultEvent`, `EnvironmentChangeEvent`, and `ServiceInstancesChangedEvent`.
  - `PropagatingRefreshRoutesEventApplicationListener` republishes `RefreshRoutesEvent` when gateway properties change.
  - `DisabledHeartbeatEventRouteRefreshListenerInterceptor` suppresses default heartbeat-driven refreshes.
  - `FilteringWebHandlerBeanDefinitionRegistryPostProcessor` replaces Spring's `FilteringWebHandler` with `CachingFilteringWebHandler`.
  - `CachingFilteringWebHandler` caches the combined global and per-route gateway filter chains after successful route refreshes instead of rebuilding them on every request.

- **WebMVC** wires in a custom `HandlerSupplier` and `ApplicationContextInitializer` through `META-INF/spring.factories`, and exposes `WebEndpointMappingGatewayServerMvcAutoConfiguration` through `AutoConfiguration.imports`.
  - `WebEndpointMappingHandlerSupplier` installs a `we(...)` handler that returns a cached `WebEndpointMappingHandlerFilterFunction` per route ID.
  - `WebEndpointMappingHandlerConfig` listens for context, environment, and service-instance changes and refreshes only the `we://` routes.
  - `WebEndpointMappingHandlerFilterFunction` mirrors the WebFlux matching and rewrite logic on the servlet stack.

The test fixtures under `microsphere-spring-cloud-gateway-server-webflux/src/test/resources/META-INF/config/default/test.yaml` and `microsphere-spring-cloud-gateway-server-webmvc/src/test/resources/application.yaml` are part of the architecture story: they document the supported route shapes, discovery profiles (`simple-service-registry`, `eureka`, `nacos`, `zookeeper`, `consul`, `kubernetes`), and the compatibility property layouts the modules are expected to keep supporting.

## Key conventions

- Keep WebFlux and WebMVC behavior aligned. If you change route matching, refresh behavior, route metadata handling, or feature toggles in one stack, inspect the corresponding class in the other stack and update both when needed.

- Preserve the registration mechanism for framework hooks. Auto-configurations live in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, while the extra WebFlux/WebMVC bootstrapping hooks use `META-INF/spring.factories`.

- Feature toggles are default-on. `microsphere.spring.cloud.gateway.enabled` and `microsphere.spring.cloud.web-endpoint-mapping.enabled` both use conditional annotations with `matchIfMissing = true`, so disabling behavior must stay explicit.

- `metadata.web-endpoint.excludes` binds to `WebEndpointConfig`. Exclusion entries support `patterns`, `methods`, `params`, `headers`, `consumes`, and `produces`; when `methods` is omitted it means all methods, and when `headers` is omitted it becomes an empty array rather than `null`.

- Route matching relies on gateway route metadata and service-published `WebEndpointMapping` metadata, not just static URI rewriting. Changes that touch matching usually need updates in:
  - `commons` configuration/binding classes,
  - the WebFlux filter path,
  - the WebMVC handler/filter path,
  - and the YAML fixtures under `src/test/resources`.

- Tests commonly use profile-driven Spring Boot fixtures instead of isolated mocks. The important test profiles are `simple-service-registry` and `gateway`, and the YAML fixtures intentionally cover both generic gateway properties and stack-specific route sections.

- Module-targeted Maven test runs usually need both `-pl <module>` and `-am`. When using `-Dtest=...` from the root, also set `-Dsurefire.failIfNoSpecifiedTests=false` so upstream modules without the named test do not fail first.
