# Source Diff: `src/test` — tag `0.2.18` vs tag `0.1.18`

> Generated on 2026-10-22 from git tags [`0.2.18`](https://github.com/microsphere-projects/microsphere-gateway/tree/0.2.18)
> (current `0.2.x` line, Java 17 + Jakarta EE) and [`0.1.18`](https://github.com/microsphere-projects/microsphere-gateway/tree/0.1.18)
> (maintenance `0.1.x` line, Java 8 + Java EE).
> Companion file: [main-diff-0.2.18-vs-0.1.18.md](main-diff-0.2.18-vs-0.1.18.md) (`src/main`).
> 中文版：[test-diff-0.2.18-vs-0.1.18.zh.md](test-diff-0.2.18-vs-0.1.18.zh.md).

## Scope and method

Same as the `src/main` comparison: matched-name modules only, `src/test` compared independently, Java 8/17 syntax and
`javax`/`jakarta` differences excluded. See [main-diff-0.2.18-vs-0.1.18.md](main-diff-0.2.18-vs-0.1.18.md) for the
module matching table (`…-server-webmvc` exists only in `0.2.18` and is skipped).

## File inventory (`src/test`)

| Module | Files in 0.2.18 | Files in 0.1.18 | Identical | Modified | Only in 0.2.18 | Only in 0.1.18 |
|---|---|---|---|---|---|---|
| `…-gateway-commons` | 8 | 8 | 8 | 0 | 0 | 0 |
| `…-server-webflux` | 14 | 14 | 9 | 5 | 0 | 0 |

## Differences — `microsphere-spring-cloud-gateway-commons`

No differences at all — the module's `src/test` is byte-identical between the two tags.

## Differences — `microsphere-spring-cloud-gateway-server-webflux`

| File | Change (0.1.18 → 0.2.18) | Category | Reason |
|---|---|---|---|
| `autoconfigure/GatewayAutoConfigurationTest.java` | Global-disabled property `spring.cloud.gateway.enabled=false` → `spring.cloud.gateway.server.webflux.enabled=false` | **Functional / API compatibility** | Spring Cloud Gateway `2025` renamed `GatewayProperties.PREFIX` to `spring.cloud.gateway.server.webflux` as part of the gateway server rebranding; the test pins the new enable-property so the auto-configuration is properly disabled under the `0.2.x` classpath. Mirrors the Javadoc-only change in `GatewayPropertyConstants.java` on the `src/main` side. |
| `autoconfigure/WebEndpointMappingGatewayAutoConfigurationTest.java` | Same property rename `spring.cloud.gateway.enabled` → `spring.cloud.gateway.server.webflux.enabled` | **Functional / API compatibility** | Same reason as above; both test classes override `configureGlobalDisabledPropertyValues` with the line-appropriate property key. |
| `filter/WebEndpointMappingGlobalFilterStaticTest.java` | `serviceInstance.setSecure(true)` added to the `DefaultServiceInstance` fixture | **Functional / test coverage** | The `we://` routing scheme derives `http` vs `https` from the instance's secure flag; `0.2.18`'s filter builds the target URI from the request URL attributes consistently, so the fixture must exercise the secure path to keep the assertion meaningful. |
| `resources/META-INF/config/default/test.yaml` | ~45 lines appended under `microsphere.spring.boot.web.server.webflux.routes` | **Functional / test coverage** | Three new route fixtures: (1) `request_size_route` with a `RequestSize` filter (`maxSize: 5000000`) and metadata, (2) `retry_test` with a `Retry` filter (retries, statuses, methods, nested `backoff`/`jitter` args), (3) a `we://all` web-endpoint-mapping route with `excludes` patterns. These verify `0.2.x` route-binding of named filter factories with nested `args` maps plus the web-endpoint exclude semantics. |

## Conclusion

The `src/test` gap is dominated by two causes: the **property rename following the Spring Cloud Gateway 2025
rebranding** (2 files) and **new coverage for filter/route features** (secure-instance fixture + three richer route
fixtures in `test.yaml`). No test class was added or removed, and the `commons` module tests are identical —
indicating the two lines keep the same test structure, diverging only where the `0.2.x` runtime API changed.
