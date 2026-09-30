package io.github.mpcoredeveloper.javaportico;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A .proto that names a well-known type without importing the file that declares it does not compile, so the
 * descriptor carries the import of every well-known type its messages use.
 */
class ProtoImportTests {

    @Test
    void aFreeFormObjectBringsInTheStructImport() {
        String proto = GeneratorTestDriver.proto(spec("payload: { type: object, additionalProperties: true }"),
                "FreeFormService");

        assertTrue(proto.contains("google.protobuf.Struct"), proto);
        assertTrue(proto.contains("import \"google/protobuf/struct.proto\";"), proto);
    }

    @Test
    void aDateTimeBringsInTheTimestampImport() {
        String proto = GeneratorTestDriver.proto(spec("created: { type: string, format: date-time }"),
                "TimestampService");

        assertTrue(proto.contains("google.protobuf.Timestamp created = 1;"), proto);
        assertTrue(proto.contains("import \"google/protobuf/timestamp.proto\";"), proto);
    }

    @Test
    void aContractWithoutWellKnownTypesCarriesNoImports() {
        String proto = GeneratorTestDriver.proto(spec("id: { type: integer }"), "PlainService");

        assertFalse(proto.contains("import "), proto);
    }

    @Test
    void theImportPrecedesEveryDefinition() {
        String proto = GeneratorTestDriver.proto(spec("payload: { type: object, additionalProperties: true }"),
                "FreeFormService");

        int packageAt = proto.indexOf("package ");
        int importAt = proto.indexOf("import \"google/protobuf/struct.proto\";");
        int definitionAt = proto.indexOf("message ");

        assertTrue(packageAt >= 0 && importAt > packageAt && definitionAt > importAt,
                "the import belongs after the package statement and before the first definition: " + proto);
    }

    @Test
    void everyWellKnownTypeLandsInOneImportList() {
        String proto = GeneratorTestDriver.proto(
                spec("created: { type: string, format: date-time }\n"
                        + "        payload: { type: object, additionalProperties: true }"),
                "MixedService");

        // One import per file: a second `import "google/protobuf/timestamp.proto";` is a descriptor protoc
        // rejects, and the same file reached twice must not produce one.
        assertEquals(2, GeneratorTestDriver.occurrences(proto, "import \""), proto);
        assertTrue(proto.contains("import \"google/protobuf/struct.proto\";"), proto);
        assertTrue(proto.contains("import \"google/protobuf/timestamp.proto\";"), proto);
        assertTrue(proto.indexOf("import \"google/protobuf/struct.proto\";")
                < proto.indexOf("import \"google/protobuf/timestamp.proto\";"), proto);
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
