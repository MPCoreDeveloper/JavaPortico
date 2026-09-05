package io.github.mpcoredeveloper.javaportico;

import io.github.mpcoredeveloper.javaportico.annotations.JavaPorticoOptions;
import io.github.mpcoredeveloper.javaportico.emit.ProtoEmitter;
import io.github.mpcoredeveloper.javaportico.mapping.OpenApiParser;
import io.github.mpcoredeveloper.javaportico.mapping.ParseResult;
import io.github.mpcoredeveloper.javaportico.model.GrpcModel;
import io.github.mpcoredeveloper.javaportico.model.WorkItem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Snapshot-style assertions on the emitted proto descriptor.
 */
class ProtoSnapshotTests {

    @Test
    void emitsProtoForPetstore() {
        GrpcModel model = petstore();
        String proto = ProtoEmitter.emit(model);

        assertTrue(proto.contains("syntax = \"proto3\";"));
        assertTrue(proto.contains("package com.example.generated;"));
        assertTrue(proto.contains("option java_package = \"com.example.generated\";"));
        assertTrue(proto.contains("option java_multiple_files = true;"));
        assertTrue(proto.contains("enum StatusEnum {"));
        assertTrue(proto.contains("AVAILABLE = 0;"));
        assertTrue(proto.contains("PENDING = 1;"));
        assertTrue(proto.contains("SOLD = 2;"));
        assertTrue(proto.contains("message Pet {"));
        assertTrue(proto.contains("int64 id = 1;"));
        assertTrue(proto.contains("string name = 2;"));
        assertTrue(proto.contains("StatusEnum status = 3;"));
        assertTrue(proto.contains("repeated Pet items = 1;"));
        assertTrue(proto.contains("message GetPetRequest {"));
        assertTrue(proto.contains("int64 pet_id = 1;"));
        assertTrue(proto.contains("service PetService {"));
        assertTrue(proto.contains("rpc GetPet (GetPetRequest) returns (GetPetResponse);"));
        assertTrue(proto.contains("rpc ListPets (ListPetsRequest) returns (ListPetsResponse);"));
        assertTrue(proto.contains("rpc CreatePet (CreatePetRequest) returns (CreatePetResponse);"));
    }

    private static GrpcModel petstore() {
        ParseResult result = OpenApiParser.parseAndMap(item());
        assertTrue(result.isSuccess(), () -> "parse failed: " + result.diagnostics());
        return result.model();
    }

    private static WorkItem item() {
        String content;
        try (var in = ProtoSnapshotTests.class.getResourceAsStream("/openapi/petstore.yaml")) {
            content = new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return WorkItem.builder()
                .filePath("openapi/petstore.yaml")
                .hintName("petstore")
                .serviceName("PetService")
                .namespaceName("com.example.generated")
                .content(content)
                .options(JavaPorticoOptions.defaults())
                .build();
    }
}
