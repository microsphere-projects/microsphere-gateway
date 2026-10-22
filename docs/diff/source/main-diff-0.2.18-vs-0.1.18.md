# Source Diff: `src/main` — tag `0.2.18` vs tag `0.1.18`

> Generated on 2026-10-22 from git tags [`0.2.18`](https://github.com/microsphere-projects/microsphere-gateway/tree/0.2.18)
> (current `0.2.x` line, Java 17 + Jakarta EE) and [`0.1.18`](https://github.com/microsphere-projects/microsphere-gateway/tree/0.1.18)
> (maintenance `0.1.x` line, Java 8 + Java EE).
> Companion file: [test-diff-0.2.18-vs-0.1.18.md](test-diff-0.2.18-vs-0.1.18.md) (`src/test`).
> 中文版：[main-diff-0.2.18-vs-0.1.18.zh.md](main-diff-0.2.18-vs-0.1.18.zh.md).

## Scope and method

- Compared per Maven module, only modules with the **same name in both tags** are compared.
- `src/main` and `src/test` are compared independently (this file covers `src/main`).
- Java 17 vs Java 8 syntax/API differences and Java EE (`javax.*`) vs Jakarta EE (`jakarta.*`) differences are
  **called out and excluded** from the "real" implementation gap.
- Chinese variant: [main-diff-0.2.18-vs-0.1.18.zh.md](main-diff-0.2.18-vs-0.1.18.zh.md).

## Module matching

| Module (same name in both tags?) | 0.2.18 | 0.1.18 | Compared |
|---|---|---|---|
| `microsphere-gateway-parent` | yes | yes | POM only, no `src/main` |
| `microsphere-gateway-dependencies` | yes | yes | POM only, no `src/main` |
| `microsphere-spring-cloud-gateway-commons` | yes | yes | yes |
| `microsphere-spring-cloud-gateway-server-webflux` | yes | yes | yes |
| `microsphere-spring-cloud-gateway-server-webmvc` | yes | **no** | skipped (no counterpart in `0.1.18`) |

## File inventory (`src/main`)

| Module | Files in 0.2.18 | Files in 0.1.18 | Identical | Modified | Only in 0.2.18 | Only in 0.1.18 |
|---|---|---|---|---|---|---|
| `…-gateway-commons` | 9 | 9 | 8 | 1 | 0 | 0 |
| `…-server-webflux` | 15 | 15 | 11 | 4 | 0 | 0 |

Every Java class and resource under `src/main` exists in both tags with the same path — there are no added or
removed `src/main` files in the matched modules.

## Differences — `microsphere-spring-cloud-gateway-commons`

| File | 0.2.18 | 0.1.18 | Category | Reason |
|---|---|---|---|---|
| `config/WebEndpointConfig.java` | imports `jakarta.validation.Valid` / `jakarta.validation.constraints.NotNull` | imports `javax.validation.Valid` / `javax.validation.constraints.NotNull` | **Excluded** — Java EE → Jakarta EE | `0.2.x` targets Spring Boot 3+, which requires the `jakarta.*` namespace; the annotation usage itself is unchanged. The module POM mirrors this (`jakarta.validation:jakarta.validation-api` vs `javax.validation:validation-api`). |

No behavioral `src/main` difference remains in this module once the namespace swap is excluded.

## Differences — `microsphere-spring-cloud-gateway-server-webflux`

| File | Change (0.1.18 → 0.2.18) | Category | Reason |
|---|---|---|---|
| `filter/WebEndpointMappingGlobalFilter.java` | Local constant `private static final int LOAD_BALANCER_CLIENT_FILTER_ORDER = 10150;` replaced by a static import of `org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER` | **Functional / API compatibility** | Spring Cloud Gateway rebranded and relocated its server artifacts across the `4.x` trains; depending on the upstream constant instead of hard-coding `10150` keeps the filter order correct for whichever gateway server artifact is on the classpath (see the `spring-cloud-2022` … `spring-cloud-2025` profiles in the module POM). |
| `filter/WebEndpointMappingGlobalFilter.java` | `onApplicationEvent` rewritten from explicit casts (`(ContextRefreshedEvent) event`) to Java 17 pattern-matching `instanceof` bindings | **Excluded** — Java 17 syntax | Semantically identical; `0.1.x` must compile on Java 8, which has no pattern-matching for `instanceof`. |
| `filter/WebEndpointMappingGlobalFilter.java` | `(Mono) loadBalancer.choose()` unchecked cast removed | **Functional / API compatibility** | Upstream `ReactorLoadBalancer#choose()` return type tightened in the newer Spring Cloud versions, so the cast that `0.1.x` needed is obsolete (and warned) in `0.2.x`. |
| `annotation/ConditionalOnGatewayAvailable.java` | Comment only: "Spring Cloud Gateway API" → "Spring Cloud Gateway Server WebFlux API" | **Naming alignment** | Reflects the Spring Cloud Gateway `2025` rename of `spring-cloud-starter-gateway` to the `spring-cloud-gateway-server-webflux` artifact; no code change. |
| `constants/GatewayPropertyConstants.java` | Javadoc only: enable-property description now says "Spring Cloud Server WebFlux Gateway: `spring.cloud.gateway.server.webflux.enabled`" | **Naming alignment** | The constant still composes `GatewayProperties.PREFIX + ".enabled"`; in the `2025` train that `PREFIX` resolves to `spring.cloud.gateway.server.webflux`. Javadoc updated to document the new effective property name (the property-name change itself is verified in the test sources, see the companion file). |
| `resources/META-INF/spring.factories` | `org.springframework.boot.autoconfigure.EnableAutoConfiguration=…` entry removed (only `ApplicationContextInitializer` remains) | **Functional / API compatibility** | Spring Boot 3 no longer loads auto-configurations from `spring.factories`; `0.2.x` relies solely on `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (which is byte-identical in both tags). `0.1.x` keeps both entries to stay Boot 2.x compatible. |

## POM-level differences (context, not `src/main` sources)

| File | Difference | Reason |
|---|---|---|
| `microsphere-gateway-parent/pom.xml` | `microsphere-spring-cloud.version` `0.1.26` → `0.2.26` | Line-specific microsphere-spring-cloud BOM (Jakarta / Boot 3 vs Java EE / Boot 2). |
| `…-server-webflux/pom.xml` | Direct optional `spring-cloud-starter-gateway` dependency replaced by profiles `spring-cloud-2022` / `2023` / `2024` (same artifact) and `spring-cloud-2025` (default, `spring-cloud-gateway-server-webflux`) | Support multiple Spring Cloud trains whose gateway artifact ids differ. |
| `…-gateway-commons/pom.xml` | `javax.validation:validation-api` → `jakarta.validation:jakarta.validation-api` | Java EE → Jakarta EE (excluded category). |

## Conclusion

After excluding the Jakarta-namespace swap and Java 17 syntax rewrites, the genuine `src/main` implementation gap in
the matched modules is small and concentrated in `WebEndpointMappingGlobalFilter.java` and `spring.factories`: all of
it stems from **Spring Boot 3 / Spring Cloud 2025 API compatibility** (delegated filter-order constant, auto-config
registration path, multi-train profiles). `0.2.18` additionally ships the brand-new `…-server-webmvc` module, which
has no `0.1.x` counterpart and is therefore outside this comparison.
