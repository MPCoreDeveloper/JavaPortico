package io.github.mpcoredeveloper.javaportico;

import io.github.mpcoredeveloper.javaportico.annotations.JavaPorticoOptions;
import io.github.mpcoredeveloper.javaportico.mapping.OpenApiParser;
import io.github.mpcoredeveloper.javaportico.mapping.ParseResult;
import io.github.mpcoredeveloper.javaportico.model.FieldKind;
import io.github.mpcoredeveloper.javaportico.model.FieldModel;
import io.github.mpcoredeveloper.javaportico.model.GrpcModel;
import io.github.mpcoredeveloper.javaportico.model.MessageModel;
import io.github.mpcoredeveloper.javaportico.model.RpcModel;
import io.github.mpcoredeveloper.javaportico.model.WorkItem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mapping scenario tests against the petstore fixture (port of SharpPortico's MappingScenarioTests).
 */
class MappingScenarioTests {

    @Test
    void mapsPetstoreService() {
        GrpcModel model = petstore();

        assertEquals("PetService", model.serviceName());
        assertEquals("com.example.generated", model.namespace());
        assertEquals("com.example.generated", model.protoPackage());
        assertEquals(1, model.services().size());

        assertEquals(3, model.services().get(0).rpcMethods().size());
        var names = model.services().get(0).rpcMethods().stream().map(RpcModel::name).toList();
        assertTrue(names.containsAll(List.of("ListPets", "GetPet", "CreatePet")));
    }

    @Test
    void mapsMessages() {
        GrpcModel model = petstore();
        assertTrue(hasMessage(model, "Pet"));
        assertTrue(hasMessage(model, "GetPetRequest"));
        assertTrue(hasMessage(model, "GetPetResponse"));
        assertTrue(hasMessage(model, "ListPetsRequest"));
        assertTrue(hasMessage(model, "ListPetsResponse"));
        assertTrue(hasMessage(model, "CreatePetRequest"));
        assertTrue(hasMessage(model, "CreatePetResponse"));
    }

    @Test
    void mapsPetFields() {
        GrpcModel model = petstore();
        MessageModel pet = message(model, "Pet");

        assertEquals(3, pet.fieldCount());
        assertEquals(FieldKind.INT64, pet.fields().get(0).kind());       // id
        assertEquals(FieldKind.STRING, pet.fields().get(1).kind());      // name
        assertEquals(FieldKind.ENUM, pet.fields().get(2).kind());        // status
        assertEquals("StatusEnum", pet.fields().get(2).typeName());
    }

    @Test
    void detectsPagination() {
        GrpcModel model = petstore();
        RpcModel listPets = model.services().get(0).rpcMethods().stream()
                .filter(r -> r.name().equals("ListPets")).findFirst().orElseThrow();
        assertTrue(listPets.hasPagination());

        FieldModel limit = message(model, "ListPetsRequest").fields().stream()
                .filter(f -> f.originalName().equals("limit")).findFirst().orElseThrow();
        assertEquals(FieldKind.INT32, limit.kind());
        assertEquals("limit", limit.protoName());
    }

    @Test
    void mapsGetPetRequestPathParameter() {
        GrpcModel model = petstore();
        FieldModel petId = message(model, "GetPetRequest").fields().stream()
                .filter(f -> f.originalName().equals("petId")).findFirst().orElseThrow();
        assertEquals(FieldKind.INT64, petId.kind());
        assertEquals("pet_id", petId.protoName());
        assertEquals("PetId", petId.name());
    }

    @Test
    void mapsResponseShapes() {
        GrpcModel model = petstore();
        // Array response -> repeated Items message field.
        MessageModel listResp = message(model, "ListPetsResponse");
        assertEquals(1, listResp.fieldCount());
        assertTrue(listResp.fields().get(0).repeated());
        assertEquals("Pet", listResp.fields().get(0).typeName());

        // $ref response -> single Data message field.
        MessageModel getResp = message(model, "GetPetResponse");
        assertEquals(1, getResp.fieldCount());
        assertEquals("Data", getResp.fields().get(0).name());
        assertEquals("Pet", getResp.fields().get(0).typeName());
    }

    @Test
    void mapsProxyConfigWhenEnabled() {
        WorkItem item = petstoreItem(JavaPorticoOptions.defaults()
                .setEnableProxyGeneration(true)
                .setProxyBaseUrl("http://localhost:5099")
                .setProxyClientKeyMode(io.github.mpcoredeveloper.javaportico.annotations.ClientKeyMode.OWN));
        ParseResult result = OpenApiParser.parseAndMap(item);
        assertTrue(result.isSuccess(), () -> "parse failed: " + result.diagnostics());
        assertEquals("http://localhost:5099", result.model().proxy().baseUrl());
        assertEquals(2, result.model().proxy().clientKeyMode()); // OWN
        assertTrue(result.model().proxy().enabled());
    }

    // ---- helpers ----

    private static GrpcModel petstore() {
        ParseResult result = OpenApiParser.parseAndMap(petstoreItem(JavaPorticoOptions.defaults()));
        assertTrue(result.isSuccess(), () -> "parse failed: " + result.diagnostics());
        return result.model();
    }

    private static WorkItem petstoreItem(JavaPorticoOptions options) {
        return WorkItem.builder()
                .filePath("openapi/petstore.yaml")
                .hintName("petstore")
                .serviceName("PetService")
                .namespaceName("com.example.generated")
                .content(petstoreContent())
                .options(options)
                .build();
    }

    private static String petstoreContent() {
        try (var in = MappingScenarioTests.class.getResourceAsStream("/openapi/petstore.yaml")) {
            return new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean hasMessage(GrpcModel model, String name) {
        return model.messages().stream().anyMatch(m -> m.name().equals(name));
    }

    private static MessageModel message(GrpcModel model, String name) {
        return model.messages().stream()
                .filter(m -> m.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing message " + name));
    }
}
