# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/), and this project adheres to
[Semantic Versioning](https://semver.org/).

## [0.1.1] - 2026-09-30

### Changed

- The version is `0.1.1`, and every document that names it names this one: `README.md`,
  `docs/JavaPortico.md` and the CLI's `--version` output (both said `0.1.0`).
- `samples/spring-boot-example` builds at the project's Java 25 baseline (`maven.compiler.release` 25 instead
  of 21). Verified by running the packaged sample end to end on Temurin 25: `SpringProxyApplication` starts in
  under two seconds, the gRPC-to-REST proxy serves, caches, bypasses and creates, and the demo reports the
  expected 4 REST calls.

### Fixed

- **A free-form object maps to protobuf's own type for one.** A property declared as `type: object` with no
  `properties` (`additionalProperties: true`) used to become a nested message whose only member was the
  `_HasValue` placeholder, so the JSON the contract described arrived as a one-field message no consumer could
  use. Such a property - and such an array element - now maps to `google.protobuf.Struct`, including the
  descriptor's own field declaration (`repeated` exactly once) and the Java class protobuf compiles that type
  into. The proxy converts a `Struct` through protobuf's JSON printer/parser, because `Struct` is not a generated
  message and has no per-message reader or writer: a call site that invented `parsePayload`/`serializePayload`
  would not compile against the descriptor this generator writes.
- **The descriptor imports the file that declares a well-known type.** A `.proto` that names
  `google.protobuf.Struct` or `google.protobuf.Timestamp` without the import that declares it does not compile.
  The imports are now derived from the model - one per file, alphabetically ordered, ahead of the first
  definition - so a contract that names no well-known type still carries none, and a contract that names two types
  from one file carries one import. The proto-name-to-import and proto-name-to-Java-class translations live in the
  new `WellKnownTypes`.
- **Two enumerations behind one name are no longer the same type.** Enum identity is the name *and* the members:
  two inline `state` properties that declare the same members share the enumeration the property implies, and two
  that declare different members - an artifact lifecycle and a session lifecycle - become two types, the second
  named after its members (`StateEnumOpenClosedExpired`), with each message referencing its own. Before, one
  enumeration silently carried the other's members. A derived name already taken by a different member set is
  refused rather than mapped, because the contract is the only place that can say which of the two the name means.
- **A refusal names the fault the reader reported, not the section that is missing because of it.** A reader that
  reports a recoverable error still hands back a document, and that document can be missing whole sections; the
  generator answered `missing info/title section`, which sends the caller to inspect the one section that is almost
  always fine. The refusal now carries the reader's own message, and falls back to the generator's text only when the
  reader said nothing. The shape this cost a session on is a YAML file with a tab where only spaces are allowed.
- `UlidClientKeyValidator` decodes ULID timestamps correctly: `describe()` previously reported an
  issuance time ~256x too small (erroneous `>> 8` shift) and used an incorrect Crockford base32
  alphabet mapping for letters after `H`. `describe()` now returns the accurate `ulid:<instant>`.
- **The parent POM is published with the artifacts.** Every module POM names
  `io.github.mpcoredeveloper:javaportico-parent` as its parent, and a consumer resolving a module reads
  that parent to build the module's effective model: while the parent was missing from the repository
  neither `0.1.0` nor the first deployment of this version resolved anything — `Failed to read artifact
  descriptor for io.github.mpcoredeveloper:javaportico-core:jar:0.1.1 ... Could not find artifact
  io.github.mpcoredeveloper:javaportico-parent:pom:0.1.1`. A `-pl` list of the published modules filters
  the aggregator out of the reactor, which is how the parent went missing; the publish workflow now leads
  that list with `.`, and `javaportico-parent:0.1.1` itself was published by an additional `parent-only`
  deployment of the same workflow, after which a fresh local repository resolves all five artifacts.

### Security

- `javaportico-runtime` resolves the patched Jackson. The module pins its own dependency versions, where
  `dependencyManagement` cannot reach, so it kept `jackson-databind` 2.22.2 while the parent's
  `version.jackson` named 2.22.3 - and the version a consumer resolved was the module's. Both now say 2.22.3.
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
- `WellKnownTypes` - the proto-name-to-import and proto-name-to-Java-class translations the emitters share
  (`google.protobuf.Struct`, `Timestamp`, `Empty`, the `wrappers.proto` values, `Any`, `Duration`, `FieldMask` and
  the struct types).
- `SchemaMapper.enumConflicts()` plus the refusal `OpenApiParser` builds from it: a derived enumeration name that
  is already taken by a different member set fails the run with both member sets named, instead of emitting code
  whose two meanings collide.
- Tests for the four fixes above: `FreeFormObjectTests` (free-form object, array of free-form objects, the
  descriptor's imports, the proxy's JSON conversion), `ProtoImportTests` (one import per file, ordered, absent when
  unused), `SchemaCollisionTests` (two member sets become two types, one member set stays one type, a schema's
  message carries its own fields) and `ContractRefusalTests` (the reader's own message, the refused derived name).

### Code quality (SonarCloud cleanup)

- `RestRequest`/`RestResponse` records now override `equals`/`hashCode`/`toString` so the `byte[]`
  payload participates by content (`S6218`).
- `GenerateMojo` validates package names by splitting on `.` instead of a nested-quantifier regex,
  removing a stack-overflow/backtracking risk (`S5998`); output paths now use `File.separator`
  (`S1075`).
- Split the largest mapping/emission methods (`OpenApiParser.mapOperation`, `parseAndMap`,
  `SchemaMapper.mapSchemaToMessage`/`mapProperty`/`mapEnum`, `ProtoEmitter.emit`,
  `ProxyJavaEmitter.emitUnary`/`emitSerializeField`/`emitParseField`) into focused helpers
  (cognitive complexity, `S3776`), switched `if/else` chains to pattern `switch`
  (`S6880`, `S7467`), and de-duplicated generated-code template fragments (`S1192`).
- Removed unused parameters/imports/local variables (`S1172`, `S1128`, `S1481`), redundant casts
  (`S1905`) and nested ternaries (`S3358`); fixed `byte`/generic/logging/cache smells in the
  runtime (`S2629`, `S1168`, `S4276`, `S8786`, `S119`, `S6218`).
- CLI and demo samples intentionally keep console output (`S106`) and the JVM `main(String[])`
  contract (`S1172`); these are documented with targeted `@SuppressWarnings`.
- Publishing prep: switched from the legacy OSSRH staging API to the Central Portal
  `org.sonatype.central:central-publishing-maven-plugin` (0.11.0) — `mvn -Prelease deploy` uploads
  and publishes via `central.sonatype.com` using a user token; `samples/*` and `tests/*` are
  excluded from deployment.
- CI runs again. The SonarCloud step tested `secrets.SONAR_TOKEN` inside a step `if`, and GitHub does
  not make the `secrets` context available there: it rejects the whole workflow file, so every push to
  `main` failed before a job started ("This run likely failed because of a workflow file issue"). The
  token now reaches the step through job-level `env`, and the step gates on `env.SONAR_TOKEN != ''`.

### Notes

- Proxy mode's free-form conversion uses `com.google.protobuf.util.JsonFormat`, which lives in
  `protobuf-java-util`; `javaportico-runtime` already depends on it, so a consumer of the generated proxy gets it
  transitively.
- The hard-coded sample credentials (`legacy-secret-key`, the `01ARZ3NDEK…` ULID client key) are
  demo fixtures, intentionally hard-coded for illustration. A cloud/security scan flagged them
  (`CWE-259`/`CWE-798`) and they were rejected as out of scope; real deployments should supply keys
  via `IKeyProvider` / configuration / a vault.
- Cloud Readiness findings from the scan (Azure Container Apps migration, localhost URLs, local file
  I/O in the CLI/plugin, no Dockerfile) were rejected as not applicable to this library / its demo
  samples or as false positives (e.g., "restricted configurations", "Jakarta EE version").
