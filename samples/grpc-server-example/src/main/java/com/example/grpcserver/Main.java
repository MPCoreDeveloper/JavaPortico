package com.example.grpcserver;

import com.example.generated.CreatePetRequest;
import com.example.generated.CreatePetResponse;
import com.example.generated.GetPetRequest;
import com.example.generated.GetPetResponse;
import com.example.generated.ListPetsRequest;
import com.example.generated.ListPetsResponse;
import com.example.generated.Pet;
import com.example.generated.PetServiceGrpc;
import com.example.generated.StatusEnum;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

/**
 * Demo: starts a gRPC server with the generated PetService, then calls it with a BlockingStub.
 */
@SuppressWarnings("java:S106") // Demo console output is intentional.
public final class Main {

    @SuppressWarnings("java:S1172") // Parameter required by the JVM main(String[]) launcher contract.
    public static void main(String[] args) throws Exception {
        int port = 50051;
        Server server = ServerBuilder.forPort(port)
                .addService(new PetServiceImpl())
                .build();
        server.start();
        System.out.println("gRPC server listening on " + port);

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
        PetServiceGrpc.PetServiceBlockingStub stub = PetServiceGrpc.newBlockingStub(channel);

        GetPetResponse petResponse = stub.getPet(GetPetRequest.newBuilder().setPetId(1).build());
        System.out.println("GetPet(1) -> " + petResponse.getData().getName()
                + " (status " + petResponse.getData().getStatus() + ")");

        CreatePetResponse created = stub.createPet(CreatePetRequest.newBuilder()
                .setBody(Pet.newBuilder().setId(3).setName("Luna").setStatus(StatusEnum.SOLD).build())
                .build());
        System.out.println("CreatePet -> id=" + created.getData().getId() + " " + created.getData().getName());

        ListPetsResponse list = stub.listPets(ListPetsRequest.newBuilder().setLimit(1).setPage(1).build());
        System.out.println("ListPets(limit=1, page=1) -> " + list.getItemsCount() + " pet(s)");

        // Serialization round-trip through the generated protobuf messages.
        byte[] bytes = petResponse.getData().toByteArray();
        Pet roundTrip = Pet.parseFrom(bytes);
        System.out.println("Round-trip OK: name=" + roundTrip.getName() + ", status=" + roundTrip.getStatus());

        server.shutdown();
        channel.shutdownNow();
    }
}
