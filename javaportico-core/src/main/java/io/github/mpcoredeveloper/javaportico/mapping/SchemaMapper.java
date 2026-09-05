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

    private static final String JAVA_TYPE_STRING = "String";
    private static final String JAVA_TYPE_BOOLEAN = "boolean";
    private static final String SCHEMA_TYPE_OBJECT = "object";
    private static final String SCHEMA_TYPE_ARRAY = "array";

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
                values.add(mapEnumValue(name, value, number));
                number++;
            }
        }
        EnumModel model = new EnumModel(sanitizePascal(name), values);
        enums.put(name, model);
        return model;
    }

    /** Converts one raw enum entry into an {@link EnumValueModel}, enforcing the int32 proto range. */
    private static EnumValueModel mapEnumValue(String enumName, Object value, int number) {
        String rawName;
        Integer rawNum = null;
        switch (value) {
            case Number n -> {
                long wide = n.longValue();
                if (wide < Integer.MIN_VALUE || wide > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("Enum value " + n + " for '" + enumName
                            + "' exceeds the int32 range supported by protobuf enums: " + wide);
                }
                rawNum = (int) wide;
                rawName = n.toString();
            }
            case String s -> rawName = s;
            default -> rawName = "VALUE_" + number;
        }
        String clean = sanitizeEnumName(rawName, number);
        return new EnumValueModel(clean, rawNum != null ? rawNum : number, rawName);
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

        MessageModel alias = aliasMessageIfRefOnly(key, schema, isRequest, isResponse);
        if (alias != null) return alias;

        MessageModel model = new MessageModel(key, collectFields(schema), isRequest, isResponse);
        messages.put(key, model);
        return model;
    }

    /** A pure {@code $ref} or single-$ref {@code allOf} schema becomes an empty alias message. */
    private MessageModel aliasMessageIfRefOnly(String key, Schema<?> schema, boolean isRequest, boolean isResponse) {
        if (schema.get$ref() != null && countOwnDefinitions(schema) == 0) {
            return registerAlias(key, isRequest, isResponse);
        }
        List<Schema> allOf = schema.getAllOf();
        if (allOf != null && allOf.size() == 1
                && (schema.getProperties() == null || schema.getProperties().isEmpty())
                && schema.getType() == null
                && allOf.get(0).get$ref() != null) {
            return registerAlias(key, isRequest, isResponse);
        }
        return null;
    }

    private MessageModel registerAlias(String key, boolean isRequest, boolean isResponse) {
        MessageModel alias = new MessageModel(key, List.of(), isRequest, isResponse);
        messages.put(key, alias);
        return alias;
    }

    /** Builds the field list of a message from the schema's composition ({@code allOf}/{@code oneOf}/...). */
    private List<FieldModel> collectFields(Schema<?> schema) {
        List<FieldModel> fields = new ArrayList<>();
        int[] fieldNo = {0};
        List<Schema> allOf = schema.getAllOf();
        List<Schema> oneOf = schema.getOneOf();
        List<Schema> anyOf = schema.getAnyOf();

        if (allOf != null && !allOf.isEmpty()) {
            for (Schema sub : allOf) mergeSchemaFields(fields, sub, fieldNo);
            mergeSchemaProperties(fields, schema, fieldNo);
        } else if ((oneOf != null && !oneOf.isEmpty()) || (anyOf != null && !anyOf.isEmpty())) {
            collectCompositionFields(fields, (oneOf != null && !oneOf.isEmpty()) ? oneOf : anyOf, fieldNo);
        } else if ((schema.getProperties() != null && !schema.getProperties().isEmpty())
                || SCHEMA_TYPE_OBJECT.equals(schema.getType())) {
            mergeSchemaProperties(fields, schema, fieldNo);
        } else if (SCHEMA_TYPE_ARRAY.equals(schema.getType())) {
            // Top-level array schema -> repeated single field named Values.
            FieldModel field = mapProperty("Values", schema, fieldNo);
            if (field != null) fields.add(field);
        } else {
            // Scalar top-level -> wrap into Value field (rare for components/schemas).
            FieldModel scalar = mapProperty("Value", schema, fieldNo);
            if (scalar != null) fields.add(scalar);
        }

        if (fields.isEmpty()) {
            fields.add(new FieldModel("_HasValue", "has_value", 1, FieldKind.BOOL, JAVA_TYPE_BOOLEAN));
        }
        return fields;
    }

    /** Flattens {@code oneOf}/{@code anyOf}: a single choice is merged directly; otherwise a discriminator is added. */
    private void collectCompositionFields(List<FieldModel> fields, List<Schema> choices, int[] fieldNo) {
        if (choices.size() == 1) {
            mergeSchemaFields(fields, choices.get(0), fieldNo);
        } else {
            // Add a string discriminator + flatten the first object-valued choice.
            fields.add(new FieldModel("Kind", "kind", ++fieldNo[0], FieldKind.STRING, JAVA_TYPE_STRING));
            Schema chosen = choices.stream()
                    .filter(c -> c.getProperties() != null && !c.getProperties().isEmpty())
                    .findFirst().orElse(choices.get(0));
            mergeSchemaFields(fields, chosen, fieldNo);
        }
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
        if (schema.get$ref() != null) return mapRefField(name, schema, fieldNo, false);
        if (SCHEMA_TYPE_ARRAY.equals(schema.getType())) return mapArrayField(name, schema, fieldNo);
        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) return mapInlineEnumField(name, schema, fieldNo, false);
        if ((schema.getProperties() != null && !schema.getProperties().isEmpty())
                || SCHEMA_TYPE_OBJECT.equals(schema.getType())) {
            return mapNestedMessageField(name, schema, fieldNo, false);
        }
        return mapScalarField(name, schema, fieldNo);
    }

    /** Maps a {@code $ref} property/parameter to a message or enum reference field. */
    private FieldModel mapRefField(String name, Schema<?> schema, int[] fieldNo, boolean repeated) {
        String refIdentifier = refId(schema.get$ref());
        String refName = sanitizePascal(refIdentifier);
        Schema<?> target = resolveComponent(refIdentifier);
        FieldKind kind = isEnum(target) ? FieldKind.ENUM : FieldKind.MESSAGE;
        return new FieldModel(sanitizePascal(name), toProtoName(name), name, ++fieldNo[0], kind, refName, refName, repeated);
    }

    /** Maps an inline enum schema (registered by name) to an enum field. */
    private FieldModel mapInlineEnumField(String name, Schema<?> schema, int[] fieldNo, boolean repeated) {
        String enumName = sanitizePascal(name) + "Enum";
        mapEnum(enumName, schema);
        return new FieldModel(sanitizePascal(name), toProtoName(name), name, ++fieldNo[0], FieldKind.ENUM, enumName, enumName, repeated);
    }

    /** Maps an inline object schema (registered by name) to a nested message field. */
    private FieldModel mapNestedMessageField(String name, Schema<?> schema, int[] fieldNo, boolean repeated) {
        String nestedName = sanitizePascal(name);
        if (!messages.containsKey(nestedName)) {
            mapSchemaToMessage(nestedName, schema, false, false);
        }
        return new FieldModel(sanitizePascal(name), toProtoName(name), name, ++fieldNo[0], FieldKind.MESSAGE, nestedName, nestedName, repeated);
    }

    /** Maps an array schema to a repeated field whose items are refs, inline enums, inline objects or scalars. */
    private FieldModel mapArrayField(String name, Schema<?> schema, int[] fieldNo) {
        Schema<?> items = schema.getItems();
        if (items == null) {
            return stringArrayField(name, fieldNo);
        }
        if (items.get$ref() != null) return mapRefField(name, items, fieldNo, true);
        if (items.getEnum() != null && !items.getEnum().isEmpty()) return mapInlineEnumField(name, items, fieldNo, true);
        if ((items.getProperties() != null && !items.getProperties().isEmpty())
                || SCHEMA_TYPE_OBJECT.equals(items.getType())) {
            return mapNestedMessageField(name, items, fieldNo, true);
        }
        FieldModel scalar = mapScalarField(name, items, fieldNo);
        if (scalar != null) {
            return new FieldModel(scalar.name(), scalar.protoName(), scalar.originalName(), scalar.number(),
                    scalar.kind(), scalar.javaType(), scalar.typeName(), true);
        }
        return stringArrayField(name, fieldNo);
    }

    private FieldModel stringArrayField(String name, int[] fieldNo) {
        return new FieldModel(sanitizePascal(name), toProtoName(name), name, ++fieldNo[0],
                FieldKind.STRING, JAVA_TYPE_STRING, JAVA_TYPE_STRING, true);
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
        return SCHEMA_TYPE_ARRAY.equals(schema.getType())
                && schema.getItems() != null
                && schema.getItems().get$ref() == null
                && ((schema.getItems().getProperties() != null && !schema.getItems().getProperties().isEmpty())
                    || SCHEMA_TYPE_OBJECT.equals(schema.getItems().getType()));
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
            case "string" -> mapStringFormat(format);
            case "integer" -> "int64".equalsIgnoreCase(format) ? FieldKind.INT64 : FieldKind.INT32;
            case "number" -> "float".equalsIgnoreCase(format) ? FieldKind.FLOAT : FieldKind.DOUBLE;
            case "boolean" -> FieldKind.BOOL;
            case "file" -> FieldKind.BYTES;
            default -> FieldKind.STRING;
        };
    }

    /** Resolves the field kind for string formats ({@code binary}/{@code byte}/{@code date-time}). */
    private static FieldKind mapStringFormat(String format) {
        if ("binary".equalsIgnoreCase(format) || "byte".equalsIgnoreCase(format)) {
            return FieldKind.BYTES;
        }
        return "date-time".equalsIgnoreCase(format) ? FieldKind.TIMESTAMP : FieldKind.STRING;
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

