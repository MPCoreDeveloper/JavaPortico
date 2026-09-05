package io.github.mpcoredeveloper.javaportico.cli;

import picocli.CommandLine;

/**
 * Entry point for the {@code javaportico} command-line tool.
 */
@CommandLine.Command(
        name = "javaportico",
        mixinStandardHelpOptions = true,
        version = "JavaPortico 0.1.1-SNAPSHOT",
        description = "Command-line companion for the JavaPortico OpenAPI -> gRPC generator.",
        subcommands = {GenerateCommand.class})
@SuppressWarnings("java:S106") // CLI console output (stdout/stderr) is intentional.
public final class CliMain implements Runnable {

    @Override
    public void run() {
        System.err.println("usage: javaportico generate <openapi.yaml|json> [--out DIR]");
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CliMain()).execute(args);
        System.exit(exitCode);
    }
}
