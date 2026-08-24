package io.github.mpcoredeveloper.javaportico;

import io.github.mpcoredeveloper.javaportico.annotations.ClientKeyMode;
import io.github.mpcoredeveloper.javaportico.annotations.JavaPorticoOptions;
import io.github.mpcoredeveloper.javaportico.emit.ProxyJavaEmitter;
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
 * Assertions on the generated gRPC-to-REST proxy source.
 */
class ProxyEmitTest {

    @Test
    void emitsProxyForPetstore() {
        GrpcModel model = petstore();
        String proxy = ProxyJavaEmitter.emit(model, item());

        assertTrue(proxy.contains("public final class PetServiceProxy extends PetServiceGrpc.PetServiceImplBase"));
        assertTrue(proxy.contains("public void getPet(GetPetRequest request, StreamObserver<GetPetResponse> responseObserver)"));
        assertTrue(proxy.contains("public void listPets(ListPetsRequest request, StreamObserver<ListPetsResponse> responseObserver)"));
        assertTrue(proxy.contains("public void createPet(CreatePetRequest request, StreamObserver<CreatePetResponse> responseObserver)"));
        // Path param registration (Pascal name for case-insensitive template substitution).
        assertTrue(proxy.contains("pathParams.put(\"PetId\""));
        // Query param registration (original OpenAPI name).
        assertTrue(proxy.contains("queryParams.put(\"petId\""));
        assertTrue(proxy.contains("queryParams.put(\"limit\""));
        // Body serialization for POST.
        assertTrue(proxy.contains("serializePet(request.getBody())"));
        // Cache store after a successful read.
        assertTrue(proxy.contains("cache.set(cacheKey"));
        // Enum tolerant parsing.
        assertTrue(proxy.contains("s.equalsIgnoreCase(\"available\")"));
        // Metadata interop.
        assertTrue(proxy.contains("ProxyContext.REQUEST_METADATA.get()"));
    }

    private static GrpcModel petstore() {
        ParseResult result = OpenApiParser.parseAndMap(item());
        assertTrue(result.isSuccess(), () -> "parse failed: " + result.diagnostics());
        return result.model();
    }

    private static WorkItem item() {
        String content;
        try (var in = ProxyEmitTest.class.getResourceAsStream("/openapi/petstore.yaml")) {
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
                .options(JavaPorticoOptions.defaults()
                        .setEnableProxyGeneration(true)
                        .setProxyBaseUrl("http://localhost:5099")
                        .setProxyApiKeyHeaderName("X-Api-Key")
                        .setProxyClientKeyMode(ClientKeyMode.OWN))
                .build();
    }
}
