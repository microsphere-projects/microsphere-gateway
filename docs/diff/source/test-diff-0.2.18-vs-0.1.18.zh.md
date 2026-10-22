# 源码对比：`src/test` —— tag `0.2.18` vs tag `0.1.18`

> 基于 2026-10-22 对 git tags [`0.2.18`](https://github.com/microsphere-projects/microsphere-gateway/tree/0.2.18)
> （当前 `0.2.x` 线，Java 17 + Jakarta EE）与 [`0.1.18`](https://github.com/microsphere-projects/microsphere-gateway/tree/0.1.18)
> （维护 `0.1.x` 线，Java 8 + Java EE）生成。
> 姊妹篇：[main-diff-0.2.18-vs-0.1.18.zh.md](main-diff-0.2.18-vs-0.1.18.zh.md)（`src/main`）。
> 英文版：[test-diff-0.2.18-vs-0.1.18.md](test-diff-0.2.18-vs-0.1.18.md)。

## 对比范围与方法

与 `src/main` 篇一致：仅对比同名 Maven 模块，`src/test` 独立对比，排除 Java 8/17 语法差异与 `javax`/`jakarta`
命名空间差异。模块匹配表见 [main-diff-0.2.18-vs-0.1.18.zh.md](main-diff-0.2.18-vs-0.1.18.zh.md)
（`…-server-webmvc` 仅存在于 `0.2.18`，跳过对比）。

## 文件清单（`src/test`）

| 模块 | 0.2.18 文件数 | 0.1.18 文件数 | 完全一致 | 有修改 | 仅 0.2.18 新增 | 仅 0.1.18 存在 |
|---|---|---|---|---|---|---|
| `…-gateway-commons` | 8 | 8 | 8 | 0 | 0 | 0 |
| `…-server-webflux` | 14 | 14 | 9 | 5 | 0 | 0 |

## 差异 —— `microsphere-spring-cloud-gateway-commons`

无任何差异 —— 该模块 `src/test` 在两个 tag 中逐字节一致。

## 差异 —— `microsphere-spring-cloud-gateway-server-webflux`

| 文件 | 变更内容（0.1.18 → 0.2.18） | 类别 | 原因 |
|---|---|---|---|
| `autoconfigure/GatewayAutoConfigurationTest.java` | 全局禁用属性 `spring.cloud.gateway.enabled=false` → `spring.cloud.gateway.server.webflux.enabled=false` | **功能 / API 兼容** | Spring Cloud Gateway `2025` 在网关 server 更名时把 `GatewayProperties.PREFIX` 改为 `spring.cloud.gateway.server.webflux`；测试同步钉住新的启用属性，才能在 `0.2.x` 类路径下正确禁用自动配置。与 `src/main` 侧 `GatewayPropertyConstants.java` 的 Javadoc 更新相互印证。 |
| `autoconfigure/WebEndpointMappingGatewayAutoConfigurationTest.java` | 同样的属性更名 `spring.cloud.gateway.enabled` → `spring.cloud.gateway.server.webflux.enabled` | **功能 / API 兼容** | 原因同上；两个测试类都通过覆写 `configureGlobalDisabledPropertyValues` 使用各自版本线的属性键。 |
| `filter/WebEndpointMappingGlobalFilterStaticTest.java` | `DefaultServiceInstance` 测试夹具新增 `serviceInstance.setSecure(true)` | **功能 / 测试覆盖** | `we://` 路由方案依据实例的 secure 标志决定生成 `http` 还是 `https` 目标地址；夹具必须走 secure 分支，断言才有意义。 |
| `resources/META-INF/config/default/test.yaml` | 在 `microsphere.spring.boot.web.server.webflux.routes` 下追加约 45 行 | **功能 / 测试覆盖** | 新增三组路由夹具：(1) 带 `RequestSize` 过滤器（`maxSize: 5000000`）与 metadata 的 `request_size_route`；(2) 带 `Retry` 过滤器（retries、statuses、methods 及嵌套 `backoff`/`jitter` 参数）的 `retry_test`；(3) `we://all` 的 web-endpoint-mapping 路由并携带 `excludes` 模式。用于验证 `0.2.x` 对具名过滤器工厂 + 嵌套 `args` 映射的路由绑定，以及 web-endpoint 排除语义。 |

## 结论

`src/test` 的差距主要来自两方面：**跟随 Spring Cloud Gateway 2025 更名的属性键调整**（2 个文件），以及
**新增的过滤器 / 路由特性覆盖**（secure 实例夹具 + `test.yaml` 中三组更丰富的路由夹具）。两个版本线均未增删测试
类，`commons` 模块测试完全一致 —— 说明测试结构保持同步，仅在 `0.2.x` 运行时 API 变化处才产生分歧。
