package io.github.mpcoredeveloper.javaportico.annotations;

/** Naming conventions applied to generated identifiers. */
public enum NamingConvention {
    /** PascalCase (C# convention). */
    PASCAL_CASE,

    /** camelCase. */
    CAMEL_CASE,

    /** snake_case (protobuf convention). */
    SNAKE_CASE,

    /** Preserve the original OpenAPI identifier verbatim. */
    ORIGINAL
}
