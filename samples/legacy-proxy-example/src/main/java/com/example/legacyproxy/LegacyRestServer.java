package com.example.legacyproxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mock legacy REST petstore (X-Api-Key checked, call counter). Serves plain JSON.
 */
public final class LegacyRestServer {

    public static final String LEGACY_KEY = "legacy-secret-key";

    private final ObjectMapper json = new ObjectMapper();
    private final List<ObjectNode> pets = new ArrayList<>();
    private final AtomicInteger callCount = new AtomicInteger();
    private final HttpServer server;

    public LegacyRestServer(int port) throws IOException {
        pets.add(pet(1, "Rex", "available"));
        pets.add(pet(2, "Milo", "pending"));
        server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/pets", this::handle);
        server.setExecutor(Executors.newFixedThreadPool(2));
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int callCount() {
        return callCount.get();
    }

    private ObjectNode pet(long id, String name, String status) {
        ObjectNode n = json.createObjectNode();
        n.put("id", id);
        n.put("name", name);
        n.put("status", status);
        return n;
    }

    private void handle(HttpExchange ex) throws IOException {
        if (!LEGACY_KEY.equals(ex.getRequestHeaders().getFirst("X-Api-Key"))) {
            ex.sendResponseHeaders(401, -1);
            ex.close();
            return;
        }
        callCount.incrementAndGet();

        String path = ex.getRequestURI().getPath();
        try {
            if ("/pets".equals(path)) {
                if ("GET".equals(ex.getRequestMethod())) {
                    respond(ex, 200, json.writeValueAsBytes(pets));
                } else if ("POST".equals(ex.getRequestMethod())) {
                    JsonNode node = json.readTree(ex.getRequestBody());
                    ObjectNode pet = (ObjectNode) node;
                    pets.add(pet);
                    respond(ex, 201, json.writeValueAsBytes(pet));
                } else {
                    ex.sendResponseHeaders(405, -1);
                    ex.close();
                }
            } else if (path.startsWith("/pets/")) {
                long id = Long.parseLong(path.substring("/pets/".length()));
                ObjectNode found = null;
                for (ObjectNode p : pets) {
                    if (p.path("id").asLong() == id) {
                        found = p;
                        break;
                    }
                }
                if (found == null) {
                    ex.sendResponseHeaders(404, -1);
                    ex.close();
                } else {
                    respond(ex, 200, json.writeValueAsBytes(found));
                }
            } else {
                ex.sendResponseHeaders(404, -1);
                ex.close();
            }
        } catch (RuntimeException ex2) {
            ex.sendResponseHeaders(400, -1);
            ex.close();
        }
    }

    private void respond(HttpExchange ex, int status, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }
}
