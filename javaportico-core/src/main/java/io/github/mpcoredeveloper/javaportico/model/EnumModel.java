package io.github.mpcoredeveloper.javaportico.model;

import java.util.List;

/** A generated protobuf enum definition. */
public record EnumModel(String name, List<EnumValueModel> values) {
    public EnumModel {
        values = List.copyOf(values);
    }
}
