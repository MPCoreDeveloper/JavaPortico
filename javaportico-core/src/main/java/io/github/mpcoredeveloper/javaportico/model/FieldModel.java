package io.github.mpcoredeveloper.javaportico.model;

/**
 * One field of a generated message.
 *
 * @param name         sanitized Pascal identifier (used for Java getters: {@code get<Name>()})
 * @param protoName    snake_case protobuf field name
 * @param originalName raw OpenAPI parameter/property name (used for query/path keys)
 * @param number       protobuf field number
 * @param kind         field kind
 * @param javaType     Java type for scalar fields (e.g. {@code long})
 * @param typeName     referenced message/enum type name for MESSAGE/ENUM fields
 * @param repeated     true when the field maps to a {@code repeated} field
 */
public record FieldModel(
        String name,
        String protoName,
        String originalName,
        int number,
        FieldKind kind,
        String javaType,
        String typeName,
        boolean repeated) {

    public FieldModel {
        if (kind == FieldKind.MESSAGE || kind == FieldKind.ENUM || kind == FieldKind.TIMESTAMP) {
            javaType = typeName != null ? typeName : javaType;
        }
    }

    /** Convenience constructor for fields without an original-name override. */
    public FieldModel(String name, String protoName, int number, FieldKind kind, String javaType, String typeName, boolean repeated) {
        this(name, protoName, protoName, number, kind, javaType, typeName, repeated);
    }

    /** Convenience constructor for scalar fields. */
    public FieldModel(String name, String protoName, int number, FieldKind kind, String javaType) {
        this(name, protoName, protoName, number, kind, javaType, null, false);
    }

    /** Convenience constructor for scalar fields carrying the original OpenAPI name. */
    public FieldModel(String name, String protoName, String originalName, int number, FieldKind kind, String javaType) {
        this(name, protoName, originalName, number, kind, javaType, null, false);
    }
}
