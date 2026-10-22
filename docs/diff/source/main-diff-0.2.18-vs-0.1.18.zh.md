# 源码对比：`src/main` —— tag `0.2.18` vs tag `0.1.18`

> 基于 2026-10-22 对 git tags [`0.2.18`](https://github.com/microsphere-projects/microsphere-gateway/tree/0.2.18)
> （当前 `0.2.x` 线，Java 17 + Jakarta EE）与 [`0.1.18`](https://github.com/microsphere-projects/microsphere-gateway/tree/0.1.18)
> （维护 `0.1.x` 线，Java 8 + Java EE）生成。
> 姊妹篇：[test-diff-0.2.18-vs-0.1.18.zh.md](test-diff-0.2.18-vs-0.1.18.zh.md)（`src/test`）。
> 英文版：[main-diff-0.2.18-vs-0.1.18.md](main-diff-0.2.18-vs-0.1.18.md)。

## 对比范围与方法

- 按 Maven 模块逐个对比，仅对比两个 tag 中**同名**的模块。
- `src/main` 与 `src/test` 独立对比（本篇覆盖 `src/main`）。
- Java 17 与 Java 8 的语法 / API 差异、Java EE（`javax.*`）与 Jakarta EE（`jakarta.*`）的差异均**标注并排除**，
  不计入真实的实现差距。

## 模块匹配

| 模块（两个 tag 是否同名） | 0.2.18 | 0.1.18 | 是否对比 |
|---|---|---|---|
| `microsphere-gateway-parent` | 有 | 有 | 仅 POM，无 `src/main` |
| `microsphere-gateway-dependencies` | 有 | 有 | 仅 POM，无 `src/main` |
| `microsphere-spring-cloud-gateway-commons` | 有 | 有 | 是 |
| `microsphere-spring-cloud-gateway-server-webflux` | 有 | 有 | 是 |
| `microsphere-spring-cloud-gateway-server-webmvc` | 有 | **无** | 跳过（`0.1.18` 中无同名模块） |

## 文件清单（`src/main`）

| 模块 | 0.2.18 文件数 | 0.1.18 文件数 | 完全一致 | 有修改 | 仅 0.2.18 新增 | 仅 0.1.18 存在 |
|---|---|---|---|---|---|---|
| `…-gateway-commons` | 9 | 9 | 8 | 1 | 0 | 0 |
| `…-server-webflux` | 15 | 15 | 11 | 4 | 0 | 0 |

匹配模块内 `src/main` 的每个 Java 类与资源文件在两个 tag 中路径完全对应，无新增或删除文件。

## 差异 —— `microsphere-spring-cloud-gateway-commons`

| 文件 | 0.2.18 | 0.1.18 | 类别 | 原因 |
|---|---|---|---|---|
| `config/WebEndpointConfig.java` | 导入 `jakarta.validation.Valid` / `jakarta.validation.constraints.NotNull` | 导入 `javax.validation.Valid` / `javax.validation.constraints.NotNull` | **排除** —— Java EE → Jakarta EE | `0.2.x` 面向 Spring Boot 3+，必须使用 `jakarta.*` 命名空间；注解本身用法未变。模块 POM 同步替换（`jakarta.validation:jakarta.validation-api` vs `javax.validation:validation-api`）。 |

排除命名空间替换后，该模块 `src/main` 无行为性差异。

## 差异 —— `microsphere-spring-cloud-gateway-server-webflux`

| 文件 | 变更内容（0.1.18 → 0.2.18） | 类别 | 原因 |
|---|---|---|---|
| `filter/WebEndpointMappingGlobalFilter.java` | 删除本地常量 `private static final int LOAD_BALANCER_CLIENT_FILTER_ORDER = 10150;`，改为静态导入 `ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER` | **功能 / API 兼容** | Spring Cloud Gateway 在 `4.x` 各版本线中重命名并调整了 server 构件；引用上游常量而非硬编码 `10150`，可保证无论类路径上装载哪个网关 server 构件（见模块 POM 的 `spring-cloud-2022`～`2025` profiles）过滤器顺序都正确。 |
| `filter/WebEndpointMappingGlobalFilter.java` | `onApplicationEvent` 由显式强转 `(ContextRefreshedEvent) event` 改写为 Java 17 的 `instanceof` 模式匹配绑定 | **排除** —— Java 17 语法 | 语义完全一致；`0.1.x` 必须能在 Java 8 上编译，而 Java 8 不支持 `instanceof` 模式匹配。 |
| `filter/WebEndpointMappingGlobalFilter.java` | 移除 `(Mono) loadBalancer.choose()` 的未检查强转 | **功能 / API 兼容** | 新版 Spring Cloud 中 `ReactorLoadBalancer#choose()` 返回类型收紧，`0.1.x` 必需的强转在 `0.2.x` 已过时且产生告警。 |
| `annotation/ConditionalOnGatewayAvailable.java` | 仅注释："Spring Cloud Gateway API" → "Spring Cloud Gateway Server WebFlux API" | **命名对齐** | 对应 Spring Cloud Gateway `2025` 将 `spring-cloud-starter-gateway` 更名为 `spring-cloud-gateway-server-webflux` 构件；无代码变更。 |
| `constants/GatewayPropertyConstants.java` | 仅 Javadoc：启用属性描述改为 "Spring Cloud Server WebFlux Gateway: `spring.cloud.gateway.server.webflux.enabled`" | **命名对齐** | 常量仍是 `GatewayProperties.PREFIX + ".enabled"`；在 `2025` 版本线中该 `PREFIX` 解析为 `spring.cloud.gateway.server.webflux`。Javadoc 更新以说明新的实际属性名（属性名变化在测试源码中验证，见姊妹篇）。 |
| `resources/META-INF/spring.factories` | 删除 `org.springframework.boot.autoconfigure.EnableAutoConfiguration=…` 条目（仅保留 `ApplicationContextInitializer`） | **功能 / API 兼容** | Spring Boot 3 不再从 `spring.factories` 加载自动配置，`0.2.x` 完全依赖 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（两个 tag 中该文件内容一致）。`0.1.x` 保留两处注册以兼容 Boot 2.x。 |

## POM 层差异（背景信息，非 `src/main` 源码）

| 文件 | 差异 | 原因 |
|---|---|---|
| `microsphere-gateway-parent/pom.xml` | `microsphere-spring-cloud.version` `0.1.26` → `0.2.26` | 两条线各自对应的 microsphere-spring-cloud BOM（Jakarta / Boot 3 vs Java EE / Boot 2）。 |
| `…-server-webflux/pom.xml` | 直接的 optional 依赖 `spring-cloud-starter-gateway` 被替换为 profiles：`spring-cloud-2022` / `2023` / `2024`（同一构件）与默认激活的 `spring-cloud-2025`（`spring-cloud-gateway-server-webflux`） | 兼容网关构件命名不同的多个 Spring Cloud 版本线。 |
| `…-gateway-commons/pom.xml` | `javax.validation:validation-api` → `jakarta.validation:jakarta.validation-api` | Java EE → Jakarta EE（排除类别）。 |

## 结论

排除 Jakarta 命名空间替换与 Java 17 语法改写后，匹配模块中真实的 `src/main` 实现差距很小，集中在
`WebEndpointMappingGlobalFilter.java` 与 `spring.factories`，全部源于 **Spring Boot 3 / Spring Cloud 2025 的
API 兼容**（改用上游过滤器顺序常量、自动配置注册路径、多版本线 profiles）。
另外，`0.2.18` 新增了全新的 `…-server-webmvc` 模块，由于 `0.1.x` 无同名模块，不在本次对比范围内。
