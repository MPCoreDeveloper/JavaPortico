package io.github.mpcoredeveloper.javaportico.mapping;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.List;

/**
 * Parses OpenAPI 3.0/3.1 documents (YAML or JSON) into the swagger-parser model.
 */
public final class OpenApiParse {

    /** Outcome of a parse attempt. */
    public record Parsed(OpenAPI document, List<String> messages) {
        public boolean ok() {
            return document != null;
        }
    }

    private OpenApiParse() {
    }

    public static Parsed parse(String content) {
        ParseOptions options = new ParseOptions();
        options.setResolve(false);
        options.setResolveFully(false);
        options.setFlatten(false);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, options);
        if (result != null && result.getOpenAPI() != null) {
            return new Parsed(result.getOpenAPI(), result.getMessages() == null ? List.of() : result.getMessages());
        }
        List<String> messages = result != null && result.getMessages() != null
                ? result.getMessages()
                : List.of("unable to parse document");
        return new Parsed(null, messages);
    }
}
