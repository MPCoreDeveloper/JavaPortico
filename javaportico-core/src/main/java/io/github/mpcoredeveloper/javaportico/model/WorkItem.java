package io.github.mpcoredeveloper.javaportico.model;

import io.github.mpcoredeveloper.javaportico.annotations.JavaPorticoOptions;

/**
 * Holds the raw content plus configuration of a single OpenAPI file to process.
 * The options carrier mirrors SharpPortico's {@code OpenApiWorkItem}.
 */
public record WorkItem(
        String filePath,
        String hintName,
        String serviceName,
        String namespaceName,
        String content,
        JavaPorticoOptions options) {

    public WorkItem {
        options = options == null ? JavaPorticoOptions.defaults() : options;
    }

    public static WorkItemBuilder builder() {
        return new WorkItemBuilder();
    }

    /** Fluent builder for {@link WorkItem}. */
    public static final class WorkItemBuilder {
        private String filePath;
        private String hintName;
        private String serviceName;
        private String namespaceName;
        private String content;
        private JavaPorticoOptions options;

        public WorkItemBuilder filePath(String filePath) { this.filePath = filePath; return this; }
        public WorkItemBuilder hintName(String hintName) { this.hintName = hintName; return this; }
        public WorkItemBuilder serviceName(String serviceName) { this.serviceName = serviceName; return this; }
        public WorkItemBuilder namespaceName(String namespaceName) { this.namespaceName = namespaceName; return this; }
        public WorkItemBuilder content(String content) { this.content = content; return this; }
        public WorkItemBuilder options(JavaPorticoOptions options) { this.options = options; return this; }

        public WorkItem build() {
            return new WorkItem(filePath, hintName, serviceName, namespaceName, content, options);
        }
    }
}
