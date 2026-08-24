package io.github.mpcoredeveloper.javaportico.model;

import java.util.List;

/** A generated protobuf message definition. */
public record MessageModel(
        String name,
        List<FieldModel> fields,
        boolean isRequest,
        boolean isResponse) {

    public MessageModel {
        fields = List.copyOf(fields);
    }

    public int fieldCount() {
        return fields.size();
    }
}
