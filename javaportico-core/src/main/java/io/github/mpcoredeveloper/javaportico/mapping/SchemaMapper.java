package io.github.mpcoredeveloper.javaportico.mapping;

import io.github.mpcoredeveloper.javaportico.annotations.JavaPorticoOptions;
import io.github.mpcoredeveloper.javaportico.model.EnumModel;
import io.github.mpcoredeveloper.javaportico.model.EnumValueModel;
import io.github.mpcoredeveloper.javaportico.model.FieldKind;
import io.github.mpcoredeveloper.javaportico.model.FieldModel;
import io.github.mpcoredeveloper.javaportico.model.MessageModel;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.mpcoredeveloper.javaportico.mapping.NameSanitizer.refId;
import static io.github.mpcoredeveloper.javaportico.mapping.NameSanitizer.sanitizeEnumName;
import static io.github.mpcoredeveloper.javaportico.mapping.NameSanitizer.sanitizePascal;
import static io.github.mpcoredeveloper.javaportico.mapping.NameSanitizer.toProtoName;

/**
 * Maps OpenAPI schemas to protobuf-style messages and enums, handling {@code $ref},
 * {@code allOf}/{@code oneOf}/{@code anyOf} composition, arrays (repeated fields),
 * enums and file uploads. Ported from SharpPortico's {@code SchemaMapper}.
 */
public final class SchemaMapper {

    private final OpenAPI document;
    private final LinkedHashMap<String, MessageModel> messages = new LinkedHashMap<>();
    private final LinkedHashMap<String, EnumModel> enums = new LinkedHashMap<>();

    public SchemaMapper(OpenAPI document) {
        this.document = document;
    }

    /** All messages registered during mapping (by name, first wins). */
    public List<MessageModel> allMessages() {
        return List.copyOf(messages.values());
    }

    /** All enums registered during mapping (by name, first wins). */
    public List<EnumModel> allEnums() {
        return List.copyOf(enums.values());
    }

    /** True when the schema declares enum values. */
    public boolean isEnum(Schema<?> schema) {
        return schema != null && schema.getEnum() != null && !schema.getEnum().isEmpty();
    }

    /** Maps an enum schema to an {@link EnumModel} and registers it (first wins). */
    public EnumModel mapEnum(String name, Schema<?> schema) {
        if (enums.containsKey(name)) return enums.get(name);

        List<EnumValueModel> values = new ArrayList<>();
        int number = 0;
        if (schema.getEnum() != null) {
            for (Object value : schema.getEnum()) {
                String rawName;
                Integer rawNum = null;
                if (value instanceof Number n) {
                    rawNum = n.intValue();
                    rawName = n.toString();
                } else if (value instanceof String s) {
                    rawName = s;
                } else {
                    rawName = "VALUE_" + number;
                }
                String clean = sanitizeEnumName(rawName, number);
                values.add(new EnumValueModel(clean, rawNum != null ? rawNum : number, rawName));
                number++;
            }
        }
        EnumModel model = new EnumModel(sanitizePascal(name), values);
        enums.put(name, model);
        return model;
    }

    /** Looks up a component schema by its {@code $ref} identifier. */
    public Schema<?> resolveComponent(String refId) {
        if (document.getComponents() == null || document.getComponents().getSchemas() == null) return null;
        return document.getComponents().getSchemas().get(refId);
    }

    /**
     * Resolves the generated type name when the schema is a {@code $ref}; otherwise {@code null}.
     */
    public String resolveSchemaName(Schema<?> schema) {
        if (schema == null || schema.get$ref() == null) return null;
        return sanitizePascal(refId(schema.get$ref()));
    }

    /**
     * Maps an object schema (or array of objects) to a {@link MessageModel} and registers it.
     * Mirrors SharpPortico's {@code MapSchemaToMessage}.
     */
    public MessageModel mapSchemaToMessage(String name, Schema<?> schema, boolean isRequest, boolean isResponse) {
        String key = sanitizePascal(name);
        if (messages.containsKey(key)) return messages.get(key);

        List<FieldModel> fields = new ArrayList<>();
        int[] fieldNo = {0};

        // $ref alias: pure reference schema with no inline definitions.
        if (schema.get$ref() != null && countOwnDefinitions(schema) == 0) {
            String targetName = sanitizePascal(refId(schema.get$ref()));
            MessageModel alias = new MessageModel(key, List.of(), isRequest, isResponse);
            messages.put(key, alias);
            return alias;
        }

        List<Schema> allOf = schema.getAllOf();
        List<Schema> oneOf = schema.getOneOf();
        List<Schema> anyOf = schema.getAnyOf();

        if (allOf != null && !allOf.isEmpty()) {
            // allOf with exactly one $ref and nothing else -> alias to that type.
            if (allOf.size() == 1
                    && (schema.getProperties() == null || schema.getProperties().isEmpty())
                    && schema.getType() == null
                    && allOf.get(0).get$ref() != null) {
                String targetName = sanitizePascal(refId(allOf.get(0).get$ref()));
                MessageModel alias = new MessageModel(key, List.of(), isRequest, isResponse);
                messages.put(key, alias);
                return alias;
            }
            for (Schema sub : allOf) mergeSchemaFields(fields, sub, fieldNo);
            mergeSchemaProperties(fields, schema, fieldNo);
        } else if ((oneOf != null && !oneOf.isEmpty()) || (anyOf != null && !anyOf.isEmpty())) {
            List<Schema> choices = (oneOf != null && !oneOf.isEmpty()) ? oneOf : anyOf;
            if (choices.size() == 1) {
                mergeSchemaFields(fields, choices.get(0), fieldNo);
            } else {
                // Add a string discriminator + flatten the first object-valued choice.
                fields.add(new FieldModel("Kind", "kind", ++fieldNo[0], FieldKind.STRING, "String"));
                Schema chosen = choices.stream()
                        .filter(c -> c.getProperties() != null && !c.getProperties().isEmpty())
                        .findFirst().orElse(choices.get(0));
                mergeSchemaFields(fields, chosen, fieldNo);
            }
        } else if ((schema.getProperties() != null && !schema.getProperties().isEmpty()) || "object".equals(schema.getType())) {
            mergeSchemaProperties(fields, schema, fieldNo);
        } else if ("array".equals(schema.getType())) {
            // Top-level array schema -> repeated single field named Values.
            FieldModel field = mapProperty("Values", schema, fieldNo);
            if (field != null) fields.add(field);
        } else {
            // Scalar top-level -> wrap into Value field (rare for components/schemas).
            FieldModel scalar = mapProperty("Value", schema, fieldNo);
            if (scalar != null) fields.add(scalar);
        }

        if (fields.isEmpty()) {
            fields.add(new FieldModel("_HasValue", "has_value", 1, FieldKind.BOOL, "boolean"));
        }

        MessageModel model = new MessageModel(key, fields, isRequest, isResponse);
        messages.put(key, model);
        return model;
    }

    private int countOwnDefinitions(Schema<?> schema) {
        int count = 0;
        if (schema.getProperties() != null) count += schema.getProperties().size();
        if (schema.getAllOf() != null) count += schema.getAllOf().size();
        if (schema.getOneOf() != null) count += schema.getOneOf().size();
        if (schema.getAnyOf() != null) count += schema.getAnyOf().size();
        if (schema.getItems() != null) count += 1;
        return count;
    }

    private void mergeSchemaFields(List<FieldModel> fields, Schema<?> schema, int[] fieldNo) {
        mergeSchemaProperties(fields, schema, fieldNo);
        if (schema.getAllOf() != null) {
            for (Schema sub : schema.getAllOf()) mergeSchemaFields(fields, sub, fieldNo);
        }
        if (schema.getOneOf() != null && !schema.getOneOf().isEmpty()) {
            Schema chosen = schema.getOneOf().stream()
                    .filter(c -> c.getProperties() != null && !c.getProperties().isEmpty())
                    .findFirst().orElse(schema.getOneOf().get(0));
            mergeSchemaFields(fields, chosen, fieldNo);
        } else if (schema.getAnyOf() != null && !schema.getAnyOf().isEmpty()) {
            Schema chosen = schema.getAnyOf().stream()
                    .filter(c -> c.getProperties() != null && !c.getProperties().isEmpty())
                    .findFirst().orElse(schema.getAnyOf().get(0));
            mergeSchemaFields(fields, chosen, fieldNo);
        }
    }

    private void mergeSchemaProperties(List<FieldModel> fields, Schema<?> schema, int[] fieldNo) {
        if (schema.getProperties() == null || schema.getProperties().isEmpty()) return;
        for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
            String propName = entry.getKey();
            Schema propSchema = entry.getValue();
            if (propName == null || propSchema == null) continue;
            FieldModel field = mapProperty(propName, propSchema, fieldNo);
            if (field != null) fields.add(field);
        }
    }

    /**
     * Maps a property/parameter to a {@link FieldModel}. Mirrors SharpPortico's {@code MapProperty}.
     */
    public FieldModel mapProperty(String name, Schema<?> schema, int[] fieldNo) {
        // $ref -> message or enum reference.
        if (schema.get$ref() != null) {
            String refId = refId(schema.get$ref());
            String refName = sanitizePascal(refId);
            Schema<?> target = resolveComponent(refId);
            boolean isEnum = isEnum(target);
            FieldKind kind = isEnum ? FieldKind.ENUM : FieldKind.MESSAGE;
            return new FieldModel(sanitizePascal(name), toProtoName(name), name, ++fieldNo[0], kind, refName, refName, false);
        }

        // array -> repeated
        if ("array".equals(schema.getType())) {
            Schema<?> items = schema.getItems();
            String repeatedName = sanitizePascal(name);
            if (items == null) {
                return new FieldModel(repeatedName, toProtoName(name), name, ++fieldNo[0], FieldKind.STRING, "String", "String", true);
            }
            if (items.get$ref() != null) {
                String refId = refId(items.get$ref());
                String refName = sanitizePascal(refId);
                Schema<?> target = resolveComponent(refId);
                boolean isEnum = isEnum(target);
                FieldKind kind = isEnum ? FieldKind.ENUM : FieldKind.MESSAGE;
                return new FieldModel(repeatedName, toProtoName(name), name, ++fieldNo[0], kind, refName, refName, true);
            }
            if (items.getEnum() != null && !items.getEnum().isEmpty()) {
                String enumName = repeatedName + "Enum";
                mapEnum(enumName, items);
                return new FieldModel(repeatedName, toProtoName(name), name, ++fieldNo[0], FieldKind.ENUM, enumName, enumName, true);
            }
            if ((items.getProperties() != null && !items.getProperties().isEmpty()) || "object".equals(items.getType())) {
                String nestedName = repeatedName;
                if (!messages.containsKey(nestedName)) {
                    mapSchemaToMessage(nestedName, items, false, false);
                }
                return new FieldModel(repeatedName, toProtoName(name), name, ++fieldNo[0], FieldKind.MESSAGE, nestedName, nestedName, true);
            }
            FieldModel scalar = mapScalarField(name, items, fieldNo);
            if (scalar != null) {
                return new FieldModel(scalar.name(), scalar.protoName(), scalar.originalName(), scalar.number(), scalar.kind(),
                        scalar.javaType(), scalar.typeName(), true);
            }
            return new FieldModel(repeatedName, toProtoName(name), name, ++fieldNo[0], FieldKind.STRING, "String", "String", true);
        }

        // inline enum
        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            String enumName = sanitizePascal(name) + "Enum";
            mapEnum(enumName, schema);
            return new FieldModel(sanitizePascal(name), toProtoName(name), name, ++fieldNo[0], FieldKind.ENUM, enumName, enumName, false);
        }

        // inline object / nested message
        if ((schema.getProperties() != null && !schema.getProperties().isEmpty()) || "object".equals(schema.getType())) {
            String nestedName = sanitizePascal(name);
            if (!messages.containsKey(nestedName)) {
                mapSchemaToMessage(nestedName, schema, false, false);
            }
            return new FieldModel(nestedName, toProtoName(name), name, ++fieldNo[0], FieldKind.MESSAGE, nestedName, nestedName, false);
        }

        // scalar
        return mapScalarField(name, schema, fieldNo);
    }

    /** Maps an OpenAPI path/query/header parameter to a request field. */
    public MapParameterResult mapParameter(Parameter parameter, JavaPorticoOptions options, int[] fieldNo) {
        if (parameter.getSchema() == null) return MapParameterResult.none();
        FieldModel field = mapProperty(parameter.getName(), parameter.getSchema(), fieldNo);
        boolean isPagination = options.isDetectPagination()
                && (parameter.getName().equalsIgnoreCase(options.getPaginationPageParameter())
                    || parameter.getName().equalsIgnoreCase(options.getPaginationLimitParameter())
                    || parameter.getName().equalsIgnoreCase(options.getPaginationCursorParameter())
                    || parameter.getName().equalsIgnoreCase(options.getPaginationNextPageTokenParameter()));
        return new MapParameterResult(isPagination, field);
    }

    /** Result of mapping one OpenAPI parameter. */
    public record MapParameterResult(boolean isPagination, FieldModel field) {
        public static MapParameterResult none() {
            return new MapParameterResult(false, null);
        }
    }

    /** True when the schema is a top-level array whose items are inline objects (not $ref). */
    public boolean isArrayOfInlineObject(Schema<?> schema) {
        return "array".equals(schema.getType())
                && schema.getItems() != null
                && schema.getItems().get$ref() == null
                && ((schema.getItems().getProperties() != null && !schema.getItems().getProperties().isEmpty())
                    || "object".equals(schema.getItems().getType()));
    }

    /** Derives a deterministic name for the element message of an inline array schema. */
    public String inlineArrayElementName(Schema<?> arraySchema, String fallbackBase) {
        if (arraySchema.getItems() != null && arraySchema.getItems().get$ref() != null) {
            return sanitizePascal(refId(arraySchema.getItems().get$ref()));
        }
        return sanitizePascal(fallbackBase) + "Item";
    }

    private FieldModel mapScalarField(String name, Schema<?> schema, int[] fieldNo) {
        FieldKind kind = resolveScalarKind(schema);
        String javaType = javaTypeFor(kind);
        return new FieldModel(sanitizePascal(name), toProtoName(name), name, ++fieldNo[0], kind, javaType);
    }

    private FieldKind resolveScalarKind(Schema<?> schema) {
        String type = schema.getType() == null ? "" : schema.getType().toLowerCase();
        String format = schema.getFormat() == null ? "" : schema.getFormat();
        return switch (type) {
            case "string" -> ("binary".equalsIgnoreCase(format) || "byte".equalsIgnoreCase(format))
                    ? FieldKind.BYTES
                    : ("date-time".equalsIgnoreCase(format) ? FieldKind.TIMESTAMP : FieldKind.STRING);
            case "integer" -> "int64".equalsIgnoreCase(format) ? FieldKind.INT64 : FieldKind.INT32;
            case "number" -> "float".equalsIgnoreCase(format) ? FieldKind.FLOAT : FieldKind.DOUBLE;
            case "boolean" -> FieldKind.BOOL;
            case "file" -> FieldKind.BYTES;
            default -> FieldKind.STRING;
        };
    }

    /** Maps a field kind to a Java type used in generated getter calls. */
    public static String javaTypeFor(FieldKind kind) {
        return switch (kind) {
            case STRING -> "String";
            case INT32, UINT32 -> "int";
            case INT64, UINT64 -> "long";
            case FLOAT -> "float";
            case DOUBLE -> "double";
            case BOOL -> "boolean";
            case BYTES -> "com.google.protobuf.ByteString";
            case TIMESTAMP -> "com.google.protobuf.Timestamp";
            default -> null;
        };
    }
}

