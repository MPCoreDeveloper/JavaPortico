package io.github.mpcoredeveloper.javaportico;

import io.github.mpcoredeveloper.javaportico.model.EnumModel;
import io.github.mpcoredeveloper.javaportico.model.FieldModel;
import io.github.mpcoredeveloper.javaportico.model.GrpcModel;
import io.github.mpcoredeveloper.javaportico.model.MessageModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two schemas that say different things may not end up sharing one generated type.
 *
 * <p>Both cases below were found by consuming the package: a contract with two {@code state} properties (an
 * artifact lifecycle and a session lifecycle) and a contract whose schemas share property names. They fail
 * silently - the generated code compiles only when nothing references the missing members, and the consumer then
 * debugs generated source instead of the contract.
 * </p>
 */
class SchemaCollisionTests {

    /**
     * Two enumerations that share a property name but declare different members become two types.
     *
     * <p>The first keeps the name the property implies; the second is named after its members, because the contract
     * never named it and its members are exactly what make it a different type.
     * </p>
     */
    @Test
    void twoEnumerationsWithOneNameAndDifferentMembersBecomeTwoTypes() {
        String spec = String.join("\n",
                "openapi: 3.0.3",
                "info: { title: Collisions, version: 1.0.0 }",
                "paths:",
                "  /artifacts:",
                "    get:",
                "      operationId: ListArtifacts",
                "      responses:",
                "        '200':",
                "          description: ok",
                "          content:",
                "            application/json:",
                "              schema: { $ref: '#/components/schemas/Artifact' }",
                "  /sessions:",
                "    get:",
                "      operationId: ListSessions",
                "      responses:",
                "        '200':",
                "          description: ok",
                "          content:",
                "            application/json:",
                "              schema: { $ref: '#/components/schemas/Session' }",
                "components:",
                "  schemas:",
                "    Artifact:",
                "      type: object",
                "      properties:",
                "        state: { type: string, enum: [pending, committed, purged] }",
                "    Session:",
                "      type: object",
                "      properties:",
                "        state: { type: string, enum: [open, closed, expired] }");

        GrpcModel model = GeneratorTestDriver.map(spec, "CollisionService");

        // Both lifecycles have to exist, with their own members - and as two enumerations, not one.
        assertEquals(2, model.enums().size(), () -> model.enums().toString());
        assertEquals(List.of("StateEnum", "StateEnumOpenClosedExpired"),
                model.enums().stream().map(EnumModel::name).toList());

        EnumModel artifact = model.enums().get(0);
        EnumModel session = model.enums().get(1);
        assertEquals(List.of("Pending", "Committed", "Purged"),
                artifact.values().stream().map(v -> v.name()).toList());
        assertEquals(List.of("Open", "Closed", "Expired"),
                session.values().stream().map(v -> v.name()).toList());

        // Each message references the enumeration that carries its own members: a field that named the property's
        // name instead would hand the session the artifact's lifecycle.
        assertEquals("StateEnum", field(model, "Artifact", "State").typeName());
        assertEquals("StateEnumOpenClosedExpired", field(model, "Session", "State").typeName());

        String proto = GeneratorTestDriver.proto(spec, "CollisionService");
        assertEquals(2, GeneratorTestDriver.occurrences(proto, "enum "), proto);
        assertTrue(proto.contains("StateEnum state = 1;"), proto);
        assertTrue(proto.contains("StateEnumOpenClosedExpired state = 1;"), proto);
        assertTrue(proto.contains("PENDING = 0;") && proto.contains("COMMITTED = 1;")
                && proto.contains("PURGED = 2;"), proto);
        assertTrue(proto.contains("OPEN = 0;") && proto.contains("CLOSED = 1;")
                && proto.contains("EXPIRED = 2;"), proto);
    }

    /** Two enumerations with identical members under one name are one type, which is what sharing a vocabulary means. */
    @Test
    void twoEnumerationsWithTheSameMembersUnderOneNameAreOneType() {
        String spec = String.join("\n",
                "openapi: 3.0.3",
                "info: { title: Shared, version: 1.0.0 }",
                "paths:",
                "  /things:",
                "    get:",
                "      operationId: ListThings",
                "      responses:",
                "        '200':",
                "          description: ok",
                "          content:",
                "            application/json:",
                "              schema: { $ref: '#/components/schemas/Thing' }",
                "components:",
                "  schemas:",
                "    Thing:",
                "      type: object",
                "      properties:",
                "        status: { type: string, enum: [active, removed] }",
                "    OtherThing:",
                "      type: object",
                "      properties:",
                "        status: { type: string, enum: [active, removed] }");

        GrpcModel model = GeneratorTestDriver.map(spec, "SharedService");

        assertEquals(1, model.enums().size(), () -> model.enums().toString());
        assertEquals("StatusEnum", model.enums().get(0).name());
        assertEquals("StatusEnum", field(model, "Thing", "Status").typeName());
        assertEquals("StatusEnum", field(model, "OtherThing", "Status").typeName());

        String proto = GeneratorTestDriver.proto(spec, "SharedService");
        assertEquals(1, GeneratorTestDriver.occurrences(proto, "enum "), proto);
    }

    /** A schema's message carries that schema's own fields - not its neighbour's, and not one field twice. */
    @Test
    void aSchemaMessageCarriesItsOwnFields() {
        String spec = String.join("\n",
                "openapi: 3.0.3",
                "info: { title: Neighbours, version: 1.0.0 }",
                "paths:",
                "  /runbooks:",
                "    get:",
                "      operationId: ListRunbooks",
                "      responses:",
                "        '200':",
                "          description: ok",
                "          content:",
                "            application/json:",
                "              schema: { $ref: '#/components/schemas/RunbookList' }",
                "  /runbooks/apply:",
                "    post:",
                "      operationId: ApplyRunbook",
                "      responses:",
                "        '200':",
                "          description: ok",
                "          content:",
                "            application/json:",
                "              schema: { $ref: '#/components/schemas/AppliedRunbook' }",
                "  /sessions:",
                "    post:",
                "      operationId: CreateSession",
                "      responses:",
                "        '200':",
                "          description: ok",
                "          content:",
                "            application/json:",
                "              schema: { $ref: '#/components/schemas/SessionCreated' }",
                "components:",
                "  schemas:",
                "    RunbookVersion:",
                "      type: object",
                "      properties:",
                "        runbook_ref: { type: string }",
                "        name: { type: string }",
                "        version: { type: integer, format: int32 }",
                "        status: { type: string, enum: [active, removed] }",
                "    RunbookList:",
                "      type: object",
                "      properties:",
                "        runbooks:",
                "          type: array",
                "          items: { $ref: '#/components/schemas/RunbookVersion' }",
                "    AppliedRunbook:",
                "      type: object",
                "      properties:",
                "        runbook_ref: { type: string }",
                "        name: { type: string }",
                "        version: { type: integer, format: int32 }",
                "        status: { type: string, enum: [active, removed] }",
                "    SessionCreated:",
                "      type: object",
                "      properties:",
                "        session_id: { type: string }",
                "        runbook_ref: { type: string }",
                "        permitted_collections:",
                "          type: array",
                "          items: { type: string }");

        GrpcModel model = GeneratorTestDriver.map(spec, "NeighbourService");
        List<String> names = message(model, "RunbookVersion").fields().stream()
                .map(f -> f.name())
                .toList();

        // One assertion carrying the mapped names, so a failure shows what actually came out.
        assertTrue(names.containsAll(List.of("RunbookRef", "Name", "Version", "Status")), () -> names.toString());
        assertFalse(names.contains("SessionId"), () -> names.toString());
        assertFalse(names.contains("PermittedCollections"), () -> names.toString());
        assertEquals(names.size(), names.stream().distinct().count(), () -> names.toString());
    }

    private static MessageModel message(GrpcModel model, String name) {
        return model.messages().stream()
                .filter(m -> m.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing message " + name));
    }

    private static FieldModel field(GrpcModel model, String messageName, String fieldName) {
        return message(model, messageName).fields().stream()
                .filter(f -> f.name().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing field " + fieldName + " on " + messageName));
    }
}
