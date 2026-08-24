package com.example.springproxy;

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
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.stub.MetadataUtils;
import io.github.mpcoredeveloper.javaportico.runtime.ProxyMetadataInterceptor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Starts the gRPC server backed by the generated {@code PetServiceProxy} and runs the same
 * cache-hit + bypass demo as the plain-gRPC sample — all dependencies injected by Spring.
 */
@Component
public class GrpcServerDemoRunner implements CommandLineRunner {

    private static final int GRPC_PORT = 50053;

    private final PetServiceProxy proxy;
    private final LegacyCallCounter counter;

    public GrpcServerDemoRunner(PetServiceProxy proxy, LegacyCallCounter counter) {
        this.proxy = proxy;
        this.counter = counter;
    }

    @Override
    public void run(String... args) throws Exception {
        Server server = ServerBuilder.forPort(GRPC_PORT)
                .addService(ServerInterceptors.intercept(proxy, new ProxyMetadataInterceptor()))
                .build();
        server.start();
        System.out.println("gRPC proxy listening on " + GRPC_PORT + " (Spring Boot)");

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", GRPC_PORT).usePlaintext().build();
        Metadata keyHeaders = new Metadata();
        keyHeaders.put(Metadata.Key.of("x-portico-key", Metadata.ASCII_STRING_MARSHALLER), ProxyConfiguration.CLIENT_KEY);
        PetServiceGrpc.PetServiceBlockingStub authed = PetServiceGrpc.newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(keyHeaders));

        GetPetResponse get1 = authed.getPet(GetPetRequest.newBuilder().setPetId(1).build());
        System.out.println("GetPet(1) -> " + get1.getData().getName()
                + " (status " + get1.getData().getStatus() + "), REST calls: " + counter.getCount());

        GetPetResponse get2 = authed.getPet(GetPetRequest.newBuilder().setPetId(1).build());
        System.out.println("GetPet(1) cached -> " + get2.getData().getName() + ", REST calls: " + counter.getCount());

        Metadata bypassHeaders = new Metadata();
        bypassHeaders.put(Metadata.Key.of("x-portico-key", Metadata.ASCII_STRING_MARSHALLER), ProxyConfiguration.CLIENT_KEY);
        bypassHeaders.put(Metadata.Key.of("x-portico-bypass-cache", Metadata.ASCII_STRING_MARSHALLER), "true");
        PetServiceGrpc.PetServiceBlockingStub bypassStub = PetServiceGrpc.newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(bypassHeaders));
        GetPetResponse get3 = bypassStub.getPet(GetPetRequest.newBuilder().setPetId(1).build());
        System.out.println("GetPet(1) bypass -> " + get3.getData().getName() + ", REST calls: " + counter.getCount());

        ListPetsResponse list = authed.listPets(ListPetsRequest.newBuilder().setLimit(20).setPage(1).build());
        System.out.println("ListPets -> " + list.getItemsCount() + " pets, REST calls: " + counter.getCount());

        CreatePetResponse created = authed.createPet(CreatePetRequest.newBuilder()
                .setBody(Pet.newBuilder().setId(3).setName("Luna").setStatus(StatusEnum.SOLD).build())
                .build());
        System.out.println("CreatePet -> id=" + created.getData().getId() + " " + created.getData().getName()
                + ", REST calls: " + counter.getCount());

        System.out.println("TOTAL REST calls: " + counter.getCount()
                + "  (expected 4: get + bypass + list + create)");
        System.out.println("Spring Boot proxy demo complete.");
    }
}
