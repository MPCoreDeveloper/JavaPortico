package com.example.legacyproxy;

import com.example.generated.CreatePetRequest;
import com.example.generated.CreatePetResponse;
import com.example.generated.GetPetRequest;
import com.example.generated.GetPetResponse;
import com.example.generated.ListPetsRequest;
import com.example.generated.ListPetsResponse;
import com.example.generated.Pet;
import com.example.generated.PetServiceGrpc;
import com.example.generated.PetServiceProxy;
import com.example.generated.StatusEnum;
import io.github.mpcoredeveloper.javaportico.annotations.ClientKeyMode;
import io.github.mpcoredeveloper.javaportico.runtime.DefaultAuditLogger;
import io.github.mpcoredeveloper.javaportico.runtime.DelegateKeyProvider;
import io.github.mpcoredeveloper.javaportico.runtime.HttpRestClient;
import io.github.mpcoredeveloper.javaportico.runtime.IClientKeyValidator;
import io.github.mpcoredeveloper.javaportico.runtime.IKeyProvider;
import io.github.mpcoredeveloper.javaportico.runtime.IProxyAuditLogger;
import io.github.mpcoredeveloper.javaportico.runtime.IProxyCache;
import io.github.mpcoredeveloper.javaportico.runtime.MemoryProxyCache;
import io.github.mpcoredeveloper.javaportico.runtime.ProxyMetadataInterceptor;
import io.github.mpcoredeveloper.javaportico.runtime.ProxyOptions;
import io.github.mpcoredeveloper.javaportico.runtime.UlidClientKeyValidator;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.stub.MetadataUtils;

import java.time.Duration;
import java.util.List;

/**
 * gRPC -> REST proxy demo (mirrors SharpPortico's LegacyProxyExample):
 * getPet (cache miss), getPet again (cache hit), getPet with bypass (forced fresh),
 * listPets, createPet. The legacy REST call counter proves the caching behaviour.
 */
@SuppressWarnings("java:S106") // Demo console output is intentional.
public final class Main {

    private static final String CLIENT_KEY = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String REST_CALLS_LABEL = ", REST calls: ";
    private static final int REST_PORT = 5099;
    private static final int GRPC_PORT = 50053;

    @SuppressWarnings("java:S1172") // Parameter required by the JVM main(String[]) launcher contract.
    public static void main(String[] args) throws Exception {
        // ------------------------------------------------------------------ legacy REST service
        LegacyRestServer rest = new LegacyRestServer(REST_PORT);
        rest.start();
        System.out.println("Legacy REST listening on " + REST_PORT);

        // ------------------------------------------------------------------ generated proxy (gRPC server)
        ProxyOptions options = new ProxyOptions()
                .setBaseUrl("http://localhost:" + REST_PORT)
                .setApiKeyHeaderName("X-Api-Key")
                .setClientKeyMode(ClientKeyMode.OWN)
                .setCacheTtl(Duration.ofSeconds(60));

        HttpRestClient restClient = new HttpRestClient("http://localhost:" + REST_PORT);
        IProxyCache cache = new MemoryProxyCache();
        IKeyProvider keys = new DelegateKeyProvider(headerName -> LegacyRestServer.LEGACY_KEY);
        IClientKeyValidator validator = new UlidClientKeyValidator(List.of(CLIENT_KEY));
        IProxyAuditLogger audit = new DefaultAuditLogger();

        PetServiceProxy proxy = new PetServiceProxy(options, restClient, cache, keys, validator, audit);

        Server server = ServerBuilder.forPort(GRPC_PORT)
                .addService(ServerInterceptors.intercept(proxy, new ProxyMetadataInterceptor()))
                .build();
        server.start();
        System.out.println("gRPC proxy listening on " + GRPC_PORT);

        // ------------------------------------------------------------------ local gRPC client
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", GRPC_PORT).usePlaintext().build();

        Metadata keyHeaders = new Metadata();
        keyHeaders.put(Metadata.Key.of("x-portico-key", Metadata.ASCII_STRING_MARSHALLER), CLIENT_KEY);
        PetServiceGrpc.PetServiceBlockingStub authed = PetServiceGrpc.newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(keyHeaders));

        // 1) Path param -> REST /pets/{id} with X-Api-Key (cacheable GET)
        GetPetResponse get1 = authed.getPet(GetPetRequest.newBuilder().setPetId(1).build());
        System.out.println("GetPet(1) -> " + get1.getData().getName()
                + " (status " + get1.getData().getStatus() + "), REST calls: " + rest.callCount());

        // 2) Same request again -> cache hit; REST count unchanged
        GetPetResponse get2 = authed.getPet(GetPetRequest.newBuilder().setPetId(1).build());
        System.out.println("GetPet(1) cached -> " + get2.getData().getName() + REST_CALLS_LABEL + rest.callCount());

        // 3) Bypass cache per call -> REST again
        Metadata bypassHeaders = new Metadata();
        bypassHeaders.put(Metadata.Key.of("x-portico-key", Metadata.ASCII_STRING_MARSHALLER), CLIENT_KEY);
        bypassHeaders.put(Metadata.Key.of("x-portico-bypass-cache", Metadata.ASCII_STRING_MARSHALLER), "true");
        PetServiceGrpc.PetServiceBlockingStub bypassStub = PetServiceGrpc.newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(bypassHeaders));
        GetPetResponse get3 = bypassStub.getPet(GetPetRequest.newBuilder().setPetId(1).build());
        System.out.println("GetPet(1) bypass -> " + get3.getData().getName() + REST_CALLS_LABEL + rest.callCount());

        // 4) Query params -> REST /pets?limit=20&page=1
        ListPetsResponse list = authed.listPets(ListPetsRequest.newBuilder().setLimit(20).setPage(1).build());
        System.out.println("ListPets -> " + list.getItemsCount() + " pets, REST calls: " + rest.callCount());

        // 5) POST body -> REST /pets (not cached)
        CreatePetResponse created = authed.createPet(CreatePetRequest.newBuilder()
                .setBody(Pet.newBuilder().setId(3).setName("Luna").setStatus(StatusEnum.SOLD).build())
                .build());
        System.out.println("CreatePet -> id=" + created.getData().getId() + " " + created.getData().getName()
                + REST_CALLS_LABEL + rest.callCount());

        System.out.println("TOTAL REST calls: " + rest.callCount()
                + "  (expected 4: get + bypass + list + create)");
        System.out.println("Proxy demo complete.");

        server.shutdown();
        channel.shutdownNow();
        rest.stop();
    }
}
