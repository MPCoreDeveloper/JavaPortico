package io.github.mpcoredeveloper.javaportico;

import io.github.mpcoredeveloper.javaportico.annotations.JavaPorticoOptions;
import io.github.mpcoredeveloper.javaportico.emit.ProtoEmitter;
import io.github.mpcoredeveloper.javaportico.emit.ProxyJavaEmitter;
import io.github.mpcoredeveloper.javaportico.mapping.OpenApiParser;
import io.github.mpcoredeveloper.javaportico.mapping.ParseResult;
import io.github.mpcoredeveloper.javaportico.model.GrpcModel;
import io.github.mpcoredeveloper.javaportico.model.WorkItem;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the generator's pure mapping/emission pipeline directly - the same functions the Maven plugin and
 * the annotation processor call - so a test can assert on the emitted descriptor and proxy text.
 * Ported from SharpPortico's {@code Infrastructure/GeneratorTestDriver}.
 */
final class GeneratorTestDriver {

    private GeneratorTestDriver() {
    }

    /**
     * A run's diagnostics, and the emitted text when mapping succeeded.
     *
     * @param diagnostics what the pipeline reported
     * @param proto       the .proto descriptor, or {@code null} when the contract was refused
     * @param proxy       the proxy source, or {@code null} when the contract was refused
     */
    record RunResult(List<String> diagnostics, String proto, String proxy) {

        /** True when the contract mapped and produced text. */
        boolean isSuccess() {
            return proto != null;
        }
    }

    /**
     * Maps and emits a specification, failing the test when the contract is refused.
     *
     * @param content     the specification
     * @param serviceName the service name to generate
     * @return the run's diagnostics and emitted text
     */
    static RunResult run(String content, String serviceName) {
        RunResult result = tryRun(content, serviceName);
        assertTrue(result.isSuccess(), () -> "mapping failed: " + result.diagnostics());
        return result;
    }

    /**
     * Runs the same pipeline without failing, so a specification that has to be refused can be asserted on:
     * the diagnostics say why, and no text is produced.
     *
     * @param content     the specification
     * @param serviceName the service name to generate
     * @return the run's diagnostics, and {@code null} text when it was refused
     */
    static RunResult tryRun(String content, String serviceName) {
        ParseResult parsed = OpenApiParser.parseAndMap(item(content, serviceName));
        if (!parsed.isSuccess() || parsed.model() == null) {
            return new RunResult(parsed.diagnostics(), null, null);
        }
        return new RunResult(parsed.diagnostics(), ProtoEmitter.emit(parsed.model()),
                ProxyJavaEmitter.emit(parsed.model()));
    }

    /**
     * Maps a specification to the IR, failing the test when the contract is refused.
     *
     * @param content     the specification
     * @param serviceName the service name to generate
     * @return the mapped model
     */
    static GrpcModel map(String content, String serviceName) {
        ParseResult parsed = OpenApiParser.parseAndMap(item(content, serviceName));
        assertTrue(parsed.isSuccess(), () -> "parse failed: " + parsed.diagnostics());
        return parsed.model();
    }

    /**
     * The .proto descriptor as emitted, for a test that has to assert on proto syntax rather than on the proxy.
     *
     * @param content     the specification
     * @param serviceName the service name to generate
     * @return the descriptor text
     */
    static String proto(String content, String serviceName) {
        return ProtoEmitter.emit(map(content, serviceName));
    }

    /**
     * How many times a fragment appears in some text.
     *
     * @param text     the text to search
     * @param fragment the fragment to count
     * @return the number of non-overlapping occurrences
     */
    static int occurrences(String text, String fragment) {
        int count = 0;
        for (int at = text.indexOf(fragment); at >= 0; at = text.indexOf(fragment, at + fragment.length())) {
            count++;
        }
        return count;
    }

    /**
     * The work item a test's specification is run as, with the proxy generation these tests assert on enabled.
     *
     * @param content     the specification
     * @param serviceName the service name to generate
     * @return the work item
     */
    static WorkItem item(String content, String serviceName) {
        return WorkItem.builder()
                .filePath("openapi/spec.yaml")
                .hintName("spec")
                .serviceName(Objects.requireNonNull(serviceName, "serviceName"))
                .content(Objects.requireNonNull(content, "content"))
                .options(JavaPorticoOptions.defaults()
                        .setEnableProxyGeneration(true)
                        .setProxyBaseUrl("http://localhost:5099"))
                .build();
    }
}
