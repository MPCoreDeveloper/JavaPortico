package io.github.mpcoredeveloper.javaportico;

import io.github.mpcoredeveloper.javaportico.model.FieldKind;
import io.github.mpcoredeveloper.javaportico.model.FieldModel;
import io.github.mpcoredeveloper.javaportico.model.GrpcModel;
import io.github.mpcoredeveloper.javaportico.model.MessageModel;
import io.github.mpcoredeveloper.javaportico.model.WellKnownTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A property the contract declares as "any JSON object" maps to protobuf's own answer for one.
 *
 * <p>Found by consuming the package: the artifact path of a real contract stores recorded payloads as JSON,
 * declared as objects with {@code additionalProperties}, and what came back was a placeholder message whose only
 * member was {@code _HasValue} - not what the contract said, and not something a consumer can use.
 * </p>
 */
class FreeFormObjectTests {

    @Test
    void freeFormObjectMapsToStruct() {
        String spec = spec("payload: { type: object, additionalProperties: true }");

        FieldModel payload = field(GeneratorTestDriver.map(spec, "FreeFormService"), "Thing", "Payload");
        assertEquals(FieldKind.MESSAGE, payload.kind());
        assertEquals(WellKnownTypes.PROTO_STRUCT, payload.typeName());

        String proto = GeneratorTestDriver.proto(spec, "FreeFormService");
        assertTrue(proto.contains("google.protobuf.Struct payload = 1;"), proto);
        assertFalse(proto.contains("_HasValue"), proto);
        assertFalse(proto.contains("message Payload"), proto);
    }

    @Test
    void anArrayOfFreeFormObjectsMapsToRepeatedStruct() {
        String spec = spec(
                "payloads:\n"
                        + "          type: array\n"
                        + "          items: { type: object, additionalProperties: true }");

        String proto = GeneratorTestDriver.proto(spec, "FreeFormService");

        // The declaration, not just the type name: a double-wrapped `repeated repeated` also contains the type
        // name, and it is not what a consumer can use.
        assertTrue(proto.contains("repeated google.protobuf.Struct payloads = 1;"), proto);
        assertEquals(1, GeneratorTestDriver.occurrences(proto, "repeated "), proto);

        String proxy = GeneratorTestDriver.run(spec, "FreeFormService").proxy();
        assertTrue(proxy.contains("for (com.google.protobuf.Struct item : msg.getPayloadsList()) "
                + "arr.add(structToJson(item));"), proxy);
    }

    @Test
    void theDescriptorNamesAndImportsTheWellKnownType() {
        String proto = GeneratorTestDriver.proto(spec("payload: { type: object, additionalProperties: true }"),
                "FreeFormService");

        assertTrue(proto.contains("google.protobuf.Struct"), proto);
        assertTrue(proto.contains("import \"google/protobuf/struct.proto\";"), proto);
    }

    @Test
    void theProxyConvertsStructThroughProtobufsJsonPrinter() {
        String proxy = GeneratorTestDriver.run(spec("payload: { type: object, additionalProperties: true }"),
                "FreeFormService").proxy();

        // Struct is not a generated message, so it has no per-message reader or writer: it is converted through
        // protobuf's JSON printer/parser, and a call site that invented `serializePayload`/`parsePayload` would
        // not compile against the descriptor this generator writes.
        assertTrue(proxy.contains("o.set(\"payload\", structToJson(msg.getPayload()));"), proxy);
        assertTrue(proxy.contains("b.setPayload(parseStruct(el.get(\"payload\")));"), proxy);
        assertFalse(proxy.contains("serializePayload("), proxy);
        assertFalse(proxy.contains("parsePayload("), proxy);
    }

    private static FieldModel field(GrpcModel model, String messageName, String fieldName) {
        MessageModel message = model.messages().stream()
                .filter(m -> m.name().equals(messageName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing message " + messageName));
        return message.fields().stream()
                .filter(f -> f.name().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing field " + fieldName));
    }

    private static String spec(String properties) {
        return String.join("\n",
                "openapi: 3.0.3",
                "info: { title: FreeForm, version: 1.0.0 }",
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
                "        " + properties);
    }
}
