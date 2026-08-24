# JavaPortico

**Build-time OpenAPI 3.0/3.1 → gRPC + Protobuf generator for Java (JDK 25 LTS).**

[![Sponsor](https://img.shields.io/badge/Sponsor-%E2%9D%A4-ff69b4)](https://github.com/sponsors/MPCoreDeveloper)

![JavaPortico](docs/assets/JavaPortico.jpg)

JavaPortico is the Java sibling of [SharpPortico](https://github.com/MPCoreDeveloper/SharpPortico). A Maven plugin
turns OpenAPI specifications (YAML or JSON) into production-quality protobuf messages, gRPC service stubs and an
optional **gRPC↔REST proxy** (`{Service}Proxy`) that forwards gRPC calls to a legacy REST API (X-Api-Key outbound,
response cache with per-call bypass, ULID client keys, audit logging).

## How it works

```
openapi.yaml / .json
   │  javaportico:generate (Maven, generate-sources phase)
   ▼
.proto (proto3, java_package + java_multiple_files)
   │  protobuf-maven-plugin (protoc + protoc-gen-grpc-java)
   ▼
Java messages + {Service}Grpc (ImplBase + Stub / BlockingStub / FutureStub + bindService)
   │  javaportico:generate again
   ▼
{Service}Proxy (extends {Service}Grpc.{Service}ImplBase) — gRPC -> REST gateway
```

The proxy pipeline is provided by `javaportico-runtime` (port of the SharpPortico proxy runtime):
`IRestClient`, `IProxyCache`, `IKeyProvider`, `IClientKeyValidator` / `UlidClientKeyValidator`,
`IProxyAuditLogger`, `ProxyOptions`, `ProxyContext`, plus Bearer/API-Key/OAuth2 auth helpers and interceptors.

## Repo layout

```
JavaPortico/
├── javaportico-annotations/   // @OpenApiToGrpc + options model + enums
├── javaportico-core/          // IR + OpenAPI parser + schema mapper + emitters (proto, proxy)
├── javaportico-maven-plugin/  // javaportico:generate goal
├── javaportico-runtime/       // proxy pipeline + auth helpers
├── javaportico-cli/           // javaportico generate <file> [--out DIR] (preview/validate)
├── samples/
│   ├── grpc-server-example/   // plain gRPC server + client (petstore)
│   └── legacy-proxy-example/  // gRPC->REST proxy: cache hit + bypass demo
├── tests/javaportico-tests/   // JUnit 5 mapping + snapshot + proxy tests
└── docs/
```

## Quick start

```xml
<plugin>
  <groupId>io.github.mpcoredeveloper</groupId>
  <artifactId>javaportico-maven-plugin</artifactId>
  <version>0.1.0</version>
  <executions>
    <execution>
      <goals><goal>generate</goal></goals>
    </execution>
  </executions>
  <configuration>
    <specs>
      <spec>
        <file>openapi/petstore.yaml</file>
        <serviceName>PetService</serviceName>
        <namespace>com.example.generated</namespace>
        <enableProxyGeneration>true</enableProxyGeneration>
        <proxyBaseUrl>http://localhost:5099</proxyBaseUrl>
        <proxyApiKeyHeaderName>X-Api-Key</proxyApiKeyHeaderName>
        <proxyClientKeyMode>OWN</proxyClientKeyMode>
      </spec>
    </specs>
  </configuration>
</plugin>
```

When no `<specs>` are configured, `src/main/openapi/**/*.{yaml,yml,json}` is scanned automatically.

See `docs/JavaPortico.md` for the full guide and `samples/` for working examples.

## Related projects

- **[SharpPortico](https://github.com/MPCoreDeveloper/SharpPortico)** — the C#/.NET equivalent of
  JavaPortico: the same OpenAPI → gRPC generator and gRPC↔REST proxy for the .NET ecosystem
  (Roslyn source generator, C# 14, NativeAOT-safe). JavaPortico is its Java sibling and mirrors its
  mapping rules, proxy pipeline and configuration knobs.
- **[MPCoreDeveloper](https://github.com/MPCoreDeveloper)** — the author organization.

## License

MIT — see [LICENSE](LICENSE). Copyright (c) 2026 MPCoreDeveloper.
