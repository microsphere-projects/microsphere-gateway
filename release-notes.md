# Release Notes

## v0.2.3

# Release Notes for Version 0.2.3

## New Features
- Added Copilot-powered release notes generation to streamline changelog drafting. ([b06b06e](https://example.com/commit/b06b06e))
- Introduced a workflow to sync branches from upstream automatically. ([5c9adf8](https://example.com/commit/5c9adf8))

## Bug Fixes
- Corrected indentation issues in `dependabot.yml`, ensuring proper configuration parsing. ([d407903](https://example.com/commit/d407903))

## Other Changes
- Updated Maven wrapper to version 3.9.15 for enhanced build compatibility. ([fb2d3e6](https://example.com/commit/fb2d3e6))
- Bumped Microsphere Spring Cloud parent version to 0.2.10. ([8f2f9d4](https://example.com/commit/8f2f9d4))
- Updated branch version numbers in the README for consistency. ([87c23b2](https://example.com/commit/87c23b2))
- Simplified merge workflow by introducing matrix strategy. ([10d8934](https://example.com/commit/10d8934))
- Added workflow contents read permission for better automation control. ([ddf75d4](https://example.com/commit/ddf75d4)) 

---

For full details, visit the [commit history](https://example.com/commit-history).

## v0.2.4

# Release Notes - Version 0.2.4

## Dependency Updates
- Bumped `microsphere Spring Cloud` to `0.2.11`. ([d4738de](https://github.com/...))

## Bug Fixes
- Fixed indentation issues in `dependabot.yml`. ([4fa0e94](https://github.com/...))

## Documentation
- Updated branch versions in `README`. ([aed9fe4](https://github.com/...))
- Improved release notes and automated release creation process. ([12eacd8](https://github.com/...))

## Build and Workflow Enhancements
- Removed trailing newline in `generate-wiki-docs.py`. ([a398e96](https://github.com/...))

---

For a detailed list of changes, refer to the [full changelog](https://github.com/.../compare/v0.2.3...v0.2.4).

**Full Changelog**: https://github.com/microsphere-projects/microsphere-gateway/compare/0.2.3...0.2.4## v0.2.5

# Release Notes - Version 0.2.5

## Dependency Updates
- Bumped `microsphere-spring-cloud` dependency to `0.2.12`. ([7673b5d](#))

## Build and Workflow Enhancements
- Added OSSRH credentials to Maven publish workflow to streamline deployment. ([5d4f530](#))
- Adjusted Maven GitHub Actions and fixed EOF issues. ([70725f9](#))

## Documentation
- Updated README to reflect changes in branches and versions. ([e744b5c](#))

---

No other changes.

**Full Changelog**: https://github.com/microsphere-projects/microsphere-gateway/compare/0.2.4...0.2.5## v0.2.6

# Release Notes for v0.2.6

## New Features
- Added Copilot instructions for `microsphere-gateway`. (#d414d84)
- Introduced agents/prompts and updated the `CONTRIBUTING.md` file. (#77e94c0)

## Documentation
- Reformatted README project links and Javadocs. (#10fc70a)
- Updated README badges. (#08a7311)

## Dependency Updates
- Upgraded `microsphere Spring Cloud` to version `0.2.13`. (#4905cd6)

---

For more details, see the [Full Changelog](https://github.com/your-repo/compare/0.2.5...0.2.6).

**Full Changelog**: https://github.com/microsphere-projects/microsphere-gateway/compare/0.2.5...0.2.6## v0.2.7

# Release Notes: Version 0.2.7

## Dependency Updates
- **Bumped `microsphere-spring-cloud` to `0.2.14`.** ([436ff2b](https://github.com/microsphere-projects/commit/436ff2b))

## Bug Fixes
- **Removed duplicated line separators and trailing whitespace** in Java source files, improving code consistency and readability. ([3e7ba6c](https://github.com/microsphere-projects/commit/3e7ba6c))

## Build and Workflow Enhancements
- Routine merges between `main` and `release` branches to streamline workflow and maintain versioning integrity. ([8c3eb54](https://github.com/microsphere-projects/commit/8c3eb54), [7432600](https://github.com/microsphere-projects/commit/7432600), [dd17967](https://github.com/microsphere-projects/commit/dd17967))
- Updated project version to prepare for the next patch. ([c9afa66](https://github.com/microsphere-projects/commit/c9afa66))

---

**Full Changelog:** [v0.2.6...v0.2.7](https://github.com/microsphere-projects/compare/v0.2.6...v0.2.7)

**Full Changelog**: https://github.com/microsphere-projects/microsphere-gateway/compare/0.2.6...0.2.7## v0.2.8

# Release Notes for Version 0.2.8

## Dependency Updates
- **Spring Cloud:** Upgraded Microsphere Spring Cloud to `0.2.15`.  
- Updated `pom.xml` configuration.

## Build and Workflow Enhancements
- Merged `main` into `release` and `release` back into `main`.  
- Bumped version to the next patch (`0.2.8`) after publishing `0.2.7`.  

---  
*No additional new features, bug fixes, or documentation changes in this release.*  

**Full Changelog**: https://github.com/microsphere-projects/microsphere-gateway/compare/0.2.7...0.2.8## v0.2.9

# Release Notes for v0.2.9

## Dependency Updates
- Bumped Microsphere Spring Cloud to `0.2.16`. (*6a6e997*)

## Build and Workflow Enhancements
- Merged `main` branch into `release`. (*0f84da7, 7d563be*)
- Merged `release` branch into `main` post-publishing. (*b4292df*)
- Updated version to prepare for the next patch release. (*398c4fb*)

**Full Changelog**: https://github.com/microsphere-projects/microsphere-gateway/compare/0.2.8...0.2.9## v0.2.10

# Release Notes for v0.2.10

## New Features
- Introduced WebFlux availability condition in auto-configuration. [#34da1a7](https://github.com/user/repo/commit/34da1a7)
- Added `ConditionalOnMicrosphereGatewayEnabled` tests for improved validation. [#444414d](https://github.com/user/repo/commit/444414d)

## Bug Fixes
- Disabled WebMVC in tests via configuration property. [#f84bf9f](https://github.com/user/repo/commit/f84bf9f)
- Removed `EnableWebMvcExtension` and `EnableWebFluxExtension` from various test classes. [#911622a](https://github.com/user/repo/commit/911622a), [#b3a42f2](https://github.com/user/repo/commit/b3a42f2)
- Removed unused imports in test classes. [#57b27c9](https://github.com/user/repo/commit/57b27c9)

## Dependency Updates
- Upgraded `microsphere-spring-cloud` to v0.2.19. [#8dc0d32](https://github.com/user/repo/commit/8dc0d32)
- Previously bumped `microsphere-spring-cloud` to v0.2.18. [#c8508d1](https://github.com/user/repo/commit/c8508d1)

## Test Improvements
- Added `microsphere` web endpoint mapping tests for better coverage. [#74481c8](https://github.com/user/repo/commit/74481c8)

## Build and Workflow Enhancements
- Introduced logic to require Web MVC and order it after `WebMvcAutoConfiguration` in auto-configuration. [#bb7e415](https://github.com/user/repo/commit/bb7e415)
- Reordered `@ConditionalOnGatewayEnabled` annotation for proper validation. [#e6356f1](https://github.com/user/repo/commit/e6356f1)

## Other Changes
- Added minor Javadoc improvements. [#3fae668](https://github.com/user/repo/commit/3fae668) 

--- 

**Full Changelog**: https://github.com/microsphere-projects/microsphere-gateway/compare/0.2.9...0.2.10## v0.2.11

# Release Notes - Version 0.2.11

## New Features
- **Gateway Features**: Registered metadata for gateway features. ([d495c83](#))

## Dependency Updates
- **Microsphere Spring Cloud**: Bumped version to `0.2.20`. ([61eb871](#))

## Other Changes
- Renamed "web endpoint bind advisor" to "listener" for improved clarity. ([a36b81d](#))

---

**Note**: This release includes minor internal merges and version bumps but no user-facing breaking changes.

**Full Changelog**: https://github.com/microsphere-projects/microsphere-gateway/compare/0.2.10...0.2.11## v0.2.12

# Release Notes - Version 0.2.12

### Other Changes
- Bumped version to the next patch after publishing `0.2.11`. (`f20486f`)  

**Full Changelog**: https://github.com/microsphere-projects/microsphere-gateway/compare/0.2.11...0.2.12## v0.2.13

# Release Notes - Version 0.2.13

### Dependency Updates
- **microsphere-spring-cloud**: Bumped to version `0.2.21`. ([0973c5c](#))

### Build and Workflow Enhancements
- Merged `main` into `release`. ([456358e](#), [074171e](#))
- Prepared for next development cycle by bumping version post `0.2.12` publication. ([ef505de](#))

---

**Full Changelog**: https://github.com/microsphere-projects/microsphere-gateway/compare/0.2.12...0.2.13## v0.2.14

# Release Notes for Version 0.2.14

## Improvements

### Bug Fixes
- **Gateway Configuration**:
  - Refined MVC gateway availability conditions. ([b616720](https://github.com/link-to-commit))
  - Added WebFlux-specific checks to gateway conditions for improved compatibility. ([3272ca9](https://github.com/link-to-commit), [b869149](https://github.com/link-to-commit))
  - Disabled `GatewayAutoConfigurationTest`. ([0fb75e4](https://github.com/link-to-commit))
  - Removed unused imports and redundant dependencies. ([1d37f94](https://github.com/link-to-commit), [964faf8](https://github.com/link-to-commit))

### Test Improvements
- Aligned WebFlux gateway auto-config tests with other modules. ([dc9a3d7](https://github.com/link-to-commit), [6024a25](https://github.com/link-to-commit))
- Used a shared test base for gateway auto-configuration scenarios. ([5676201](https://github.com/link-to-commit))
- Removed obsolete test file `WebEndpointMappingGatewayAutoConfigurationTest.java`. ([076434d](https://github.com/link-to-commit))

### Dependency Updates
- Upgraded Microsphere Spring Cloud to `0.2.22`. ([bf9fc72](https://github.com/link-to-commit))

### Build and Workflow Enhancements
- Various merges from `main` to ensure release branch alignment. ([Multiple `chore: merge main` commits](https://github.com/link-to-commits))
- Bumped version to prepare for next patch release cycle. ([04fc744](https://github.com/link-to-commit))

---

For a complete list of changes, refer to the [Full Changelog](https://github.com/link-to-full-changelog).

**Full Changelog**: https://github.com/microsphere-projects/microsphere-gateway/compare/0.2.13...0.2.14