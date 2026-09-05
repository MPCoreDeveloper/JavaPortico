package io.github.mpcoredeveloper.javaportico;

import com.sun.net.httpserver.HttpServer;
import io.github.mpcoredeveloper.javaportico.runtime.GrpcStatusMapper;
import io.github.mpcoredeveloper.javaportico.runtime.HttpRestClient;
import io.github.mpcoredeveloper.javaportico.runtime.MemoryProxyCache;
import io.github.mpcoredeveloper.javaportico.runtime.RestRequest;
import io.github.mpcoredeveloper.javaportico.runtime.RestResponse;
import io.github.mpcoredeveloper.javaportico.runtime.UlidClientKeyValidator;
import io.grpc.Status;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the runtime proxy building blocks.
 */
class RuntimeProxyTests {

    @Test
    void ulidValidatorChecksShapeAndAllowedList() {
        String key = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        UlidClientKeyValidator validator = new UlidClientKeyValidator(List.of(key));

        assertTrue(validator.isValid(key));
        assertFalse(validator.isValid("not-a-ulid-key"));
        assertFalse(validator.isValid("01ARZ3NDEKTSV4RRFFQ69G5FAI")); // contains I
        assertFalse(validator.isValid(""));
        assertFalse(validator.isValid(null));
    }

    @Test
    void ulidDescribeReportsAccurateIssuedTimestamp() {
        String key = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        UlidClientKeyValidator validator = new UlidClientKeyValidator(List.of(key));
        // The first 10 chars decode to the 48-bit ULID timestamp 1469922850259 ms
        // (2016-07-30T23:54:10.259Z); a previous `>> 8` shift made this ~256x too small.
        assertEquals("ulid:2016-07-30T23:54:10.259Z", validator.describe(key));
    }

    @Test
    void statusMapperMapsHttpCodes() {
        assertEquals(Status.Code.INTERNAL, GrpcStatusMapper.toStatus(500, "boom").getCode());
        assertEquals(Status.Code.INTERNAL, GrpcStatusMapper.toStatus(503, "down").getCode());
        assertEquals(Status.Code.NOT_FOUND, GrpcStatusMapper.toStatus(404, "missing").getCode());
        assertEquals(Status.Code.UNAUTHENTICATED, GrpcStatusMapper.toStatus(401, "bad").getCode());
        assertEquals(Status.Code.INVALID_ARGUMENT, GrpcStatusMapper.toStatus(400, "bad").getCode());
    }

    @Test
    void memoryCacheHonoursTtl() {
        MemoryProxyCache cache = new MemoryProxyCache();
        cache.set("k", new byte[]{1, 2, 3}, Duration.ofMillis(60));
        assertArrayEquals(new byte[]{1, 2, 3}, cache.get("k"));
        // Busy-wait (no Thread.sleep) until the 60 ms TTL has expired.
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (cache.get("k") != null && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertNull(cache.get("k"));
    }

    @Test
    void httpRestClientSubstitutesPathAndQuery() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        AtomicReference<String> capturedUri = new AtomicReference<>();
        AtomicReference<String> capturedHeader = new AtomicReference<>();
        server.createContext("/pets", ex -> {
            capturedUri.set(ex.getRequestURI().toString());
            capturedHeader.set(ex.getRequestHeaders().getFirst("X-Api-Key"));
            byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            try (HttpRestClient client = new HttpRestClient("http://localhost:" + port)) {
                RestRequest request = new RestRequest("GET", "/pets/{petId}",
                        Map.of("PetId", "42"),
                        Map.of("petId", "42"),
                        Map.of("X-Api-Key", "secret"),
                        null, "application/json");

                RestResponse resp = client.send(request);
                assertEquals(200, resp.getStatusCode());
                assertEquals("{\"ok\":true}", new String(resp.getBody(), StandardCharsets.UTF_8));
                assertTrue(capturedUri.get().startsWith("/pets/42?petId=42"));
                assertEquals("secret", capturedHeader.get());
            }
        } finally {
            server.stop(0);
        }
    }
}
