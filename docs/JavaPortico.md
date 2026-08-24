# JavaPortico — Developer Guide

JavaPortico is the **Java sibling of SharpPortico**: a build-time generator (Maven) that converts
OpenAPI 3.0/3.1 specs (YAML/JSON) into **protobuf messages, gRPC service stubs and an optional
gRPC↔REST proxy** for legacy REST APIs. Built on **JDK 25 LTS**.

---

## 1. Modules

| Module | What it contains |
| --- | --- |
| `javaportico-annotations` | `@OpenApiToGrpc` annotation, `JavaPorticoOptions`, enums (`ClientKeyMode`, `NamingConvention`, `GrpcStatusCodeMapping`) |
| `javaportico-core` | OpenAPI parser + schema mapper, immutable IR, proto emitter, proxy emitter |
| `javaportico-maven-plugin` | `javaportico:generate` goal (generate-sources phase) |
| `javaportico-runtime` | Proxy pipeline: `IRestClient`, `IProxyCache`, `IKeyProvider`, `IClientKeyValidator`, audit, auth helpers/interceptors |
| `javaportico-cli` | `javaportico generate <file> [--out DIR]` preview/validation tool |

---

## 2. Quick start — generate a gRPC service

Declare the spec in your `pom.xml`, then run protoc via `protobuf-maven-plugin`:

```xml
<build>
  <extensions>
    <extension>
      <groupId>kr.motd.maven</groupId>
      <artifactId>os-maven-plugin</artifactId>
      <version>1.7.1</version>
    </extension>
  </extensions>
  <plugins>
    <!-- 1) JavaPortico writes .proto (+ optional proxy) -->
    <plugin>
      <groupId>io.github.mpcoredeveloper</groupId>
      <artifactId>javaportico-maven-plugin</artifactId>
      <version>0.1.0</version>
      <executions>
        <execution><goals><goal>generate</goal></goals></execution>
      </executions>
      <configuration>
        <specs>
          <spec>
            <file>openapi/petstore.yaml</file>
            <serviceName>PetService</serviceName>
            <namespace>com.example.generated</namespace>
          </spec>
        </specs>
      </configuration>
    </plugin>
    <!-- 2) protoc + grpc-java generate messages + {Service}Grpc -->
    <plugin>
      <groupId>org.xolstice.maven.plugins</groupId>
      <artifactId>protobuf-maven-plugin</artifactId>
      <version>0.6.1</version>
      <configuration>
        <protocArtifact>com.google.protobuf:protoc:4.36.0:exe:${os.detected.classifier}</protocArtifact>
        <pluginId>grpc-java</pluginId>
        <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.83.1:exe:${os.detected.classifier}</pluginArtifact>
        <protoSourceRoot>${project.build.directory}/generated-sources/protobuf</protoSourceRoot>
      </configuration>
      <executions>
        <execution>
          <goals><goal>compile</goal><goal>compile-custom</goal></goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

> The `javaportico:generate` goal must be declared **before** `protobuf-maven-plugin` so the
> generated `.proto` exists when protoc runs (both bind to the `generate-sources` phase).

When no `<specs>` are configured, `src/main/openapi/**/*.{yaml,yml,json}` is scanned automatically.
Add `javax.annotation:javax.annotation-api` to compile grpc-java generated code.

Use the generated code:

```java
ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
PetServiceGrpc.PetServiceBlockingStub stub = PetServiceGrpc.newBlockingStub(channel);
GetPetResponse resp = stub.getPet(GetPetRequest.newBuilder().setPetId(42).build());
```

---

## 3. Proxy mode — gRPC clients → legacy REST

Enable proxy generation:

```xml
<spec>
  <file>openapi/petstore.yaml</file>
  <serviceName>PetService</serviceName>
  <namespace>com.example.generated</namespace>
  <enableProxyGeneration>true</enableProxyGeneration>
  <proxyBaseUrl>http://localhost:5099</proxyBaseUrl>
  <proxyApiKeyHeaderName>X-Api-Key</proxyApiKeyHeaderName>
  <proxyClientKeyMode>OWN</proxyClientKeyMode>
  <proxyAuditEnabled>true</proxyAuditEnabled>
</spec>
```

Host the generated `{Service}Proxy`:

```java
ProxyOptions options = new ProxyOptions()
        .setBaseUrl("http://localhost:5099")
        .setApiKeyHeaderName("X-Api-Key")
        .setClientKeyMode(ClientKeyMode.OWN);

PetServiceProxy proxy = new PetServiceProxy(
        options,
        new HttpRestClient("http://localhost:5099"),
        new MemoryProxyCache(),
        new DelegateKeyProvider(h -> "legacy-secret-key"),
        new UlidClientKeyValidator(List.of(CLIENT_KEY)),
        new DefaultAuditLogger());

Server server = ServerBuilder.forPort(50053)
        .addService(ServerInterceptors.intercept(proxy, new ProxyMetadataInterceptor()))
        .build();
```

- **Cache**: GET responses are cached (TTL). Clients bypass per call via the
  `x-portico-bypass-cache` metadata header.
- **Client keys** (`ClientKeyMode`): `NONE` · `FORWARD` (client key → outbound X-Api-Key 1:1) ·
  `OWN` (validate a ULID-shaped key `x-portico-key`, use the configured outbound key).
- **Keys never hardcoded**: `IKeyProvider` (config / delegate / composite).
- **Audit**: `ProxyAuditEnabled` (or inject `IProxyAuditLogger`).
- **Metadata bridge**: grpc-java does not expose request headers to `ImplBase` handlers, so the
  host must wrap the service with `ProxyMetadataInterceptor` (as above).

---

## 4. Mapping rules

| OpenAPI | gRPC / protobuf |
| --- | --- |
| paths + HTTP verb | Unary RPC by default; `x-grpc-streaming` or large POST payloads → streaming |
| path/query/header params | Single `*Request` message |
| body | Nested message; `application/octet-stream` → `bytes` |
| response | `*Response` message (+ google.rpc.Status-shaped error wrapper) |
| components/schemas | `message` definitions (`$ref`, `allOf`, `oneOf`/`anyOf`) |
| arrays | `repeated` |
| enums | protobuf enums (proxy JSON parse is case-insensitive on raw values) |
| auth | `AuthMetadata` helpers + Bearer/API-Key/OAuth2 client interceptors |
| pagination | `page/limit/cursor/next_page_token` detection |

---

## 5. CLI

```bash
java -jar javaportico-cli/target/javaportico.jar generate openapi/petstore.yaml --out out/
```

---

## 6. Samples

- `samples/grpc-server-example` — plain gRPC server + client (petstore).
- `samples/legacy-proxy-example` — mock legacy REST (X-Api-Key + call counter), generated proxy,
  cache hit + bypass demo:

```
GetPet(1)         -> Rex    (REST calls: 1)
GetPet(1) cached  -> Rex    (REST calls: 1)   <- cache hit
GetPet(1) bypass  -> Rex    (REST calls: 2)   <- forced fresh
ListPets          -> 2 pets (REST calls: 3)
CreatePet         -> id=3 Luna (REST calls: 4)
TOTAL REST calls: 4  (expected 4: get + bypass + list + create)
```

---

## 7. Configuration knobs (spec)

`file`, `serviceName`, `namespace`, `emitProtoFile`, `emitClient`, `emitServer`,
`emitDependencyInjection`, `respectStreamingHints`, `largePayloadStreamingThresholdBytes`,
`generateAuthMetadataHelpers`, `generateAuthInterceptors`, `detectPagination`,
`paginationPageParameter`, `paginationLimitParameter`, `paginationCursorParameter`,
`paginationNextPageTokenParameter`, `emitGoogleRpcStatusWrapper`, `serviceNameSuffix`,
`enableProxyGeneration`, `proxyBaseUrl`, `proxyApiKeyHeaderName`, `proxyCacheTtlSeconds`,
`proxyBypassCacheMetadataKey`, `proxyClientKeyHeaderName`, `proxyClientKeyMode`, `proxyAuditEnabled`.

---

## License

MIT — see [LICENSE](../LICENSE). Copyright (c) 2026 MPCoreDeveloper.

