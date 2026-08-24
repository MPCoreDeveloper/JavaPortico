package io.github.mpcoredeveloper.javaportico.mapping;

import io.github.mpcoredeveloper.javaportico.model.GrpcModel;
import io.github.mpcoredeveloper.javaportico.model.WorkItem;

import java.util.List;

/** Outcome of parsing and mapping a single OpenAPI work item into the IR. */
public record ParseResult(
        WorkItem item,
        boolean isSuccess,
        GrpcModel model,
        List<String> diagnostics) {

    public ParseResult {
        diagnostics = List.copyOf(diagnostics);
    }

    public static ParseResult success(WorkItem item, GrpcModel model, List<String> diagnostics) {
        return new ParseResult(item, true, model, diagnostics);
    }

    public static ParseResult failure(WorkItem item, String diagnostic) {
        return new ParseResult(item, false, null, List.of(diagnostic));
    }

    public static ParseResult failure(WorkItem item, List<String> diagnostics) {
        return new ParseResult(item, false, null, diagnostics);
    }
}
