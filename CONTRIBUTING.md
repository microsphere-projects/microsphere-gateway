# Contributing

Thanks for contributing to Microsphere Gateway.

## Before You Start

- Search [existing issues](https://github.com/microsphere-projects/microsphere-gateway/issues) before opening a new report or feature request.
- For behavior or API changes, open an issue first so the approach can be discussed early.

## Local Setup

```bash
git clone https://github.com/microsphere-projects/microsphere-gateway.git
cd microsphere-gateway
./mvnw package
./mvnw test -Ptest,spring-cloud-2025
```

The project targets Java 17+ and is exercised in CI across multiple Spring Cloud profiles.

## Making Changes

1. Create a branch from `main`.
2. Keep changes focused and update documentation when behavior changes.
3. Run the relevant Maven build or test command before submitting.

## Pull Requests

1. Describe the problem and the intended fix clearly.
2. Link the related issue when one exists.
3. Include tests for code changes when practical.

## Community Standards

By participating, you agree to follow [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).
