package io.github.mpcoredeveloper.javaportico.emit;

/**
 * Small indentation-aware code writer used by the emitters.
 */
public final class CodeWriter {

    private final StringBuilder sb = new StringBuilder();
    private int indent;

    public CodeWriter line(String s) {
        sb.append("  ".repeat(Math.max(0, indent))).append(s).append('\n');
        return this;
    }

    public CodeWriter line() {
        sb.append('\n');
        return this;
    }

    /** Writes a block: {@code header { ... }}. */
    public CodeWriter block(String header, Runnable body) {
        line(header + " {");
        indent++;
        body.run();
        indent--;
        line("}");
        return this;
    }

    /** Writes an indented body section (used with {@link #closeBlock()}). */
    public CodeWriter openBlock() {
        indent++;
        return this;
    }

    public CodeWriter closeBlock() {
        indent--;
        return this;
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
