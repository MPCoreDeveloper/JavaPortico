# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/), and this project adheres to
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Fixed

- `UlidClientKeyValidator` decodes ULID timestamps correctly: `describe()` previously reported an
  issuance time ~256x too small (erroneous `>> 8` shift) and used an incorrect Crockford base32
  alphabet mapping for letters after `H`. `describe()` now returns the accurate `ulid:<instant>`.

### Security

- `ProxyJavaEmitter` escapes OpenAPI-derived values (paths, parameter names, enum raw values) when
  emitting Java string literals, closing a source-code injection vector in generated proxy classes
  (`CWE-77`).
- `GenerateMojo` validates `<namespace>` / `<serviceName>` as a Java package / identifier before using
  them as output paths, preventing path-traversal writes outside the configured output directory
  (`CWE-22`, `CWE-23`, `CWE-36`).
- `SchemaMapper` range-checks numeric enum values against the protobuf `int32` range and fails fast
  instead of silently truncating (`CWE-681`).
- `HttpRestClient` implements `AutoCloseable` (releases the internally-created `HttpClient`) and caps
  response body size at 64 MiB by default (`CWE-772`, `CWE-789`).
- `LegacyRestServer` uses a thread-safe pet store (`CopyOnWriteArrayList`), always closes the
  `HttpExchange`, limits POST bodies to 1 MiB, and logs failed authentication
  (`CWE-567`, `CWE-662`, `CWE-775`, `CWE-778`, `CWE-789`, `CWE-820`, `CWE-821`).
- `LegacyPetstoreController` logs failed authentication attempts (`CWE-778`).

### Added

- `RestException(String message)` constructor.
- `HttpRestClient(HttpClient, String, int maxResponseBytes)` constructor.
- Unit tests for ULID `describe()` timestamp decoding and `ProxyJavaEmitter` string-literal escaping.

### Notes

- The hard-coded sample credentials (`legacy-secret-key`, the `01ARZ3NDEK…` ULID client key) are
  demo fixtures, intentionally hard-coded for illustration. A cloud/security scan flagged them
  (`CWE-259`/`CWE-798`) and they were rejected as out of scope; real deployments should supply keys
  via `IKeyProvider` / configuration / a vault.
- Cloud Readiness findings from the scan (Azure Container Apps migration, localhost URLs, local file
  I/O in the CLI/plugin, no Dockerfile) were rejected as not applicable to this library / its demo
  samples or as false positives (e.g., "restricted configurations", "Jakarta EE version").
