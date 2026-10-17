# OSDU Core Library for Azure

[![Release](https://img.shields.io/github/v/release/danielscholl-osdu/osdu-spi-core-lib-azure)](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/releases)
[![Validate](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/actions/workflows/validate.yml/badge.svg?branch=main)](https://github.com/danielscholl-osdu/osdu-spi-core-lib-azure/actions/workflows/validate.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> [!NOTE]
> This library began in the [OSDU community project](https://community.opengroup.org/osdu/platform/system/lib/cloud/azure/os-core-lib-azure) and carries its history. Releases from `3.1.0` on are built and published here; `3.0.4` and earlier stay in the community registry.

core-lib-azure is the shared Azure layer for OSDU services: clients and configuration for Cosmos DB, Blob and Data Lake Storage, Key Vault, Service Bus, Event Grid and Redis, plus the partition, entitlements and legal clients, logging, and HTTP resiliency that every Azure provider uses.

## At a glance

| | |
|---|---|
| Maven coordinates | `org.opengroup.osdu:core-lib-azure` |
| Maven repository | `https://spipackagesproto.z1.web.core.windows.net/maven` |
| Java | 17 |
| Depends on | `org.opengroup.osdu:os-core-common`, from the OSDU community registry |
| Used by | The Azure provider of each `osdu-spi-<service>` |

## Use it

Add the repository and the dependency to the service's poms. The repository allows anonymous reads.

```xml
<repositories>
  <repository>
    <id>spi-packages</id>
    <url>https://spipackagesproto.z1.web.core.windows.net/maven</url>
  </repository>
</repositories>

<dependency>
  <groupId>org.opengroup.osdu</groupId>
  <artifactId>core-lib-azure</artifactId>
  <version>3.1.0</version>
</dependency>
```

The library brings a transaction logger and an SLF4J binding. A service that already has another binding gets a "multiple SLF4J bindings" warning at start-up; exclude the duplicate binding from whichever dependency brings it.

## Repository layout

| Path | Contents |
|---|---|
| `src/main/java/org/opengroup/osdu/azure/` | One package per Azure concern: `cosmosdb`, `blobstorage`, `datalakestorage`, `servicebus`, `eventgrid`, `cache`, `partition`, `entitlements`, `legal`, `logging`, `httpconfig`, `resiliency`, `health` and others |
| `src/main/resources/` | `common.properties`, defaults shared by every service ([guide](docs/01_Using_Common_Properties.md)) |
| `docs/` | Usage guides |
| `.github/` | Workflows, rulesets and Dependabot configuration |

## Build

Requires Java 17 and Maven 3.8+. `os-core-common` resolves from the OSDU community registry through the settings file in `.mvn`; the empty `CI_JOB_TOKEN` makes that read anonymous:

```bash
CI_JOB_TOKEN= mvn --settings .mvn/community-maven.settings.xml clean verify
```

The jar lands at `target/core-lib-azure-<version>.jar`, and the coverage report at `target/site/jacoco/index.html`.

## Configuration

A service turns features on and tunes them with application properties.

**Tenant factory** (`TenantFactoryImpl`):

| Property | Example | Purpose |
|---|---|---|
| `tenantFactoryImpl.required` | `true` | Use the library's `TenantFactoryImpl` |
| `tenantInfo.container.name` | `TenantInfo` | Cosmos container |
| `azure.cosmosdb.database` | `osdu-db` | Cosmos database |

**Blob store**:

| Property | Example | Purpose |
|---|---|---|
| `azure.blobStore.required` | `true` | Create the blob store beans |
| `azure.storage.account-name` | `<storage-account>` | Storage account |

**Service-to-service HTTP**:

| Property | Default |
|---|---|
| `azure.service.retry.config.maxRetry` | `3` |
| `azure.service.retry.config.connectTimeout` | `60000` ms |
| `azure.service.retry.config.requestTimeout` | `60000` ms |
| `azure.service.retry.config.socketTimeout` | `60000` ms |

**Circuit breaker**, built on [Resilience4j](https://resilience4j.readme.io/docs/circuitbreaker). When it is enabled, every HTTP call a service makes goes through it; once failures cross the threshold the circuit opens and calls fail fast with 500 Service Unavailable until a half-open probe succeeds ([state diagram](docs/circuitbreakerstatediagram.png)).

| Property | Default | Purpose |
|---|---|---|
| `azure.circuitbreaker.enable` | `false` | Turn the circuit breaker on |
| `azure.circuitbreaker.defaultCircuitBreaker` | `false` | Use Resilience4j's defaults instead of the values below |
| `azure.circuitbreaker.slidingWindowType` | `TIME_BASED` | `TIME_BASED` or `COUNT_BASED` |
| `azure.circuitbreaker.slidingWindowSize` | `50` | Seconds (time based) or calls (count based) used for the failure rate |
| `azure.circuitbreaker.minimumNumberOfCalls` | `50` | Calls needed in the window before the rate counts |
| `azure.circuitbreaker.failureRate` | `50` | Failure percentage that opens the circuit |
| `azure.circuitbreaker.permittedCallsInHalfOpenState` | `10` | Calls allowed while half open |

Further guides: [health check failure logging](docs/02_Enable_Logging_for_Health_Check_Fails.md) and [local development authentication](docs/03_local_api_development_guide.md).

## Test

| Suite | Runs in CI | Run it yourself |
|---|---|---|
| Unit | Pull requests and `main` (Java Build) | `mvn ... verify` from [Build](#build) |

A library change is proven in a service by pinning a release in that service's fork and running its Deploy and Test lane.

## Release

Pull request titles follow Conventional Commits and pick the next version: `fix` is a patch, `feat` a minor, `feat!` or a `BREAKING CHANGE` footer a major. release-please keeps a release PR open; merging it tags `vX.Y.Z`, attaches the jar and pom to the GitHub release, and publishes them to the Maven repository above. A released version is never replaced. A branch can be published as a snapshot for testing in a service first. [CONTRIBUTING.md](CONTRIBUTING.md) covers snapshots, forcing a version, restoring the host, and how Dependabot updates are released.

## License

Copyright © Microsoft Corporation

Licensed under the [Apache License 2.0](LICENSE). [NOTICE](NOTICE) lists the other contributors and third-party components.
