# Microsphere Gateway

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/microsphere-projects/microsphere-gateway)
[![Maven Build](https://github.com/microsphere-projects/microsphere-gateway/actions/workflows/maven-build.yml/badge.svg)](https://github.com/microsphere-projects/microsphere-gateway/actions/workflows/maven-build.yml)
[![Codecov](https://codecov.io/gh/microsphere-projects/microsphere-gateway/branch/main/graph/badge.svg)](https://app.codecov.io/gh/microsphere-projects/microsphere-gateway)
![Maven](https://img.shields.io/maven-central/v/io.github.microsphere-projects/microsphere-gateway.svg)
![License](https://img.shields.io/github/license/microsphere-projects/microsphere-gateway.svg)

Microsphere Gateway extends the Spring Cloud Gateway ecosystem with Microsphere-specific routing, service discovery, and
refresh behavior. It provides both reactive and servlet-based gateway modules so you can expose discovered service web
endpoints behind a single gateway without duplicating route definitions in every service.

## Why It's Useful

- **Use the stack you already run** with dedicated modules for Spring Cloud Gateway WebFlux and Spring Cloud Gateway
  Server MVC.
- **Route discovered web endpoints dynamically** through the custom `we://` route scheme backed by Spring Cloud
  discovery.
- **Keep routes current** when gateway configuration, route definitions, or service instances change.
- **Refine exposure rules per route** with `metadata.web-endpoint.excludes` filters for paths, methods, headers, params,
  consumes, and produces.
- **Adopt consistently across modules** with a BOM and shared commons module.

## Modules

| Module                                            | Purpose                                                               |
|---------------------------------------------------|-----------------------------------------------------------------------|
| `microsphere-gateway-parent`                      | Parent POM and shared build configuration                             |
| `microsphere-gateway-dependencies`                | BOM for dependency management                                         |
| `microsphere-spring-cloud-gateway-commons`        | Shared constants, annotations, and configuration binding              |
| `microsphere-spring-cloud-gateway-server-webflux` | Reactive gateway integration for Spring Cloud Gateway                 |
| `microsphere-spring-cloud-gateway-server-webmvc`  | Servlet-based gateway integration for Spring Cloud Gateway Server MVC |

## Getting Started

### Prerequisites

- JDK 17 or newer
- Maven 3.9+ or the included Maven Wrapper
- A Spring Cloud application using a supported release train
- A discovery client setup for the services you want the gateway to route to

The CI build covers Java 17, 21, and 25 against Spring Cloud `2022`, `2023`, `2024`, and `2025` profiles.

### 1. Import the BOM

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-gateway-dependencies</artifactId>
            <version>${microsphere-gateway.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. Add the Gateway Module You Need

**WebFlux**

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-spring-cloud-gateway-server-webflux</artifactId>
</dependency>
```

**Server MVC**

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-spring-cloud-gateway-server-webmvc</artifactId>
</dependency>
```

### 3. Configure a Web Endpoint Mapping Route

`we://all` subscribes to all discovered services. You can also target specific services with hosts such as
`we://orders-service` or `we://orders-service,payments-service`.

The target services must publish Microsphere web endpoint metadata so the gateway can discover request mappings and
rewrite incoming paths correctly.

**WebFlux**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: web-endpoint-mapping
          uri: we://all
          predicates:
            - Path=/{application}/**
          metadata:
            web-endpoint:
              excludes:
                - patterns: /internal/**
                  methods: GET
```

**Server MVC**

```yaml
spring:
  cloud:
    gateway:
      mvc:
        routes:
          - id: web-endpoint-mapping
            uri: we://all
            predicates:
              - Path=/{application}/**
            metadata:
              web-endpoint:
                excludes:
                  - patterns: /internal/**
                    methods: GET
```

By default, Microsphere Gateway is enabled. You can turn features off with:

```yaml
microsphere:
  spring:
    cloud:
      gateway:
        enabled: false
      web-endpoint-mapping:
        enabled: false
```

### 4. Build or Test Locally

```bash
./mvnw package
./mvnw test -Ptest,spring-cloud-2025
```

## Getting Help

- [Issue tracker](https://github.com/microsphere-projects/microsphere-gateway/issues) for bugs and feature requests
- [Project wiki](https://github.com/microsphere-projects/microsphere-gateway/wiki) for broader documentation
- [DeepWiki](https://deepwiki.com/microsphere-projects/microsphere-gateway) and [ZRead](https://zread.ai/microsphere-projects/microsphere-gateway) for browsable project context
- Javadocs:
  - [WebFlux](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-spring-cloud-gateway-server-webflux)
  - [Server MVC](https://javadoc.io/doc/io.github.microsphere-projects/microsphere-spring-cloud-gateway-server-webmvc)

## Maintainers and Contributing

This project is maintained by the [Microsphere organization](https://github.com/microsphere-projects).

- Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.
- Follow the community expectations in [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## License

Distributed under the [Apache License 2.0](LICENSE).
