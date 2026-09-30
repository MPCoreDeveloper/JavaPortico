package io.github.mpcoredeveloper.javaportico.model;

import java.util.Map;

/**
 * The well-known protobuf types a field can reference, and what the emitters need for them.
 *
 * <p>A field whose type name is one of these carries a type that is declared outside every file the
 * generator writes: the descriptor needs the {@code .proto} import that declares it, and generated
 * Java needs the Java class protobuf compiles it into (which is not the proto name). Both are derived
 * from the type name here rather than assumed. Ported from SharpPortico's {@code ProtoEmitter} import
 * table.
 * </p>
 */
public final class WellKnownTypes {

    /** Type name of protobuf's free-form JSON object, which a free-form OpenAPI object maps to. */
    public static final String PROTO_STRUCT = "google.protobuf.Struct";

    /** Type name emitted for a {@code date-time} string. */
    public static final String PROTO_TIMESTAMP = "google.protobuf.Timestamp";

    /** Type name emitted for a message field with no resolved type name. */
    public static final String PROTO_EMPTY = "google.protobuf.Empty";

    /** The Java class protobuf compiles {@link #PROTO_STRUCT} into. */
    public static final String JAVA_STRUCT = "com.google.protobuf.Struct";

    /** The import that names each well-known type. */
    private static final Map<String, String> IMPORTS = Map.ofEntries(
            Map.entry("google.protobuf.Any", "google/protobuf/any.proto"),
            Map.entry("google.protobuf.BoolValue", "google/protobuf/wrappers.proto"),
            Map.entry("google.protobuf.BytesValue", "google/protobuf/wrappers.proto"),
            Map.entry("google.protobuf.DoubleValue", "google/protobuf/wrappers.proto"),
            Map.entry("google.protobuf.Duration", "google/protobuf/duration.proto"),
            Map.entry(PROTO_EMPTY, "google/protobuf/empty.proto"),
            Map.entry("google.protobuf.FieldMask", "google/protobuf/field_mask.proto"),
            Map.entry("google.protobuf.FloatValue", "google/protobuf/wrappers.proto"),
            Map.entry("google.protobuf.Int32Value", "google/protobuf/wrappers.proto"),
            Map.entry("google.protobuf.Int64Value", "google/protobuf/wrappers.proto"),
            Map.entry("google.protobuf.ListValue", "google/protobuf/struct.proto"),
            Map.entry("google.protobuf.NullValue", "google/protobuf/struct.proto"),
            Map.entry("google.protobuf.StringValue", "google/protobuf/wrappers.proto"),
            Map.entry(PROTO_STRUCT, "google/protobuf/struct.proto"),
            Map.entry(PROTO_TIMESTAMP, "google/protobuf/timestamp.proto"),
            Map.entry("google.protobuf.UInt32Value", "google/protobuf/wrappers.proto"),
            Map.entry("google.protobuf.UInt64Value", "google/protobuf/wrappers.proto"),
            Map.entry("google.protobuf.Value", "google/protobuf/struct.proto"));

    private WellKnownTypes() {
    }

    /**
     * The import that declares a type.
     *
     * @param typeName a proto type name carried by a field
     * @return the import path, or {@code null} when the name is a generated type
     */
    public static String protoImport(String typeName) {
        return typeName == null ? null : IMPORTS.get(typeName);
    }

    /**
     * The Java type generated code uses for a field referencing a type.
     *
     * @param protoTypeName a proto type name carried by a field
     * @return the Java type: the protobuf own class for a well-known type, the proto name otherwise
     */
    public static String javaType(String protoTypeName) {
        return PROTO_STRUCT.equals(protoTypeName) ? JAVA_STRUCT : protoTypeName;
    }
}
