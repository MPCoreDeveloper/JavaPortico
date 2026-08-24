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
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of the generated {@code PetServiceGrpc.PetServiceImplBase}.
 */
public final class PetServiceImpl extends PetServiceGrpc.PetServiceImplBase {

    private final Map<Long, Pet> pets = new ConcurrentHashMap<>();

    public PetServiceImpl() {
        pets.put(1L, Pet.newBuilder().setId(1).setName("Rex").setStatus(StatusEnum.AVAILABLE).build());
        pets.put(2L, Pet.newBuilder().setId(2).setName("Milo").setStatus(StatusEnum.PENDING).build());
    }

    @Override
    public void listPets(ListPetsRequest request, StreamObserver<ListPetsResponse> responseObserver) {
        int page = request.getPage() <= 0 ? 1 : request.getPage();
        int limit = request.getLimit() <= 0 ? 20 : request.getLimit();
        ListPetsResponse.Builder b = ListPetsResponse.newBuilder();
        pets.values().stream()
                .sorted((a, c) -> Long.compare(a.getId(), c.getId()))
                .skip((long) (page - 1) * limit)
                .limit(limit)
                .forEach(b::addItems);
        responseObserver.onNext(b.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getPet(GetPetRequest request, StreamObserver<GetPetResponse> responseObserver) {
        Pet pet = pets.get(request.getPetId());
        if (pet == null) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("pet " + request.getPetId() + " not found")
                    .asRuntimeException());
            return;
        }
        responseObserver.onNext(GetPetResponse.newBuilder().setData(pet).build());
        responseObserver.onCompleted();
    }

    @Override
    public void createPet(CreatePetRequest request, StreamObserver<CreatePetResponse> responseObserver) {
        if (!request.hasBody()) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("body required").asRuntimeException());
            return;
        }
        pets.put(request.getBody().getId(), request.getBody());
        responseObserver.onNext(CreatePetResponse.newBuilder().setData(request.getBody()).build());
        responseObserver.onCompleted();
    }
}
