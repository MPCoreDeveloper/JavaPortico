package io.github.mpcoredeveloper.javaportico.cli;

import io.github.mpcoredeveloper.javaportico.mapping.OpenApiParse;
import io.swagger.v3.oas.models.OpenAPI;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * {@code javaportico generate <file> [--out DIR]} — parses an OpenAPI 3.0/3.1 spec (YAML/JSON)
 * and prints what JavaPortico would generate: the service surface, operation count and schema count.
 * Mirrors the SharpPortico.Cli tool.
 */
@Command(name = "generate",
        description = "Parses an OpenAPI spec and reports what JavaPortico would generate.")
public final class GenerateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "OpenAPI specification file (YAML or JSON)")
    private String file;

    @Option(names = "--out", description = "Optional output directory for a preview copy")
    private String outDir;

    @Override
    public Integer call() {
        File f = new File(file);
        if (!f.isFile()) {
            System.err.println("file not found: " + file);
            return 2;
        }
        String content;
        try {
            content = Files.readString(f.toPath());
        } catch (IOException ex) {
            System.err.println("error: " + ex.getMessage());
            return 1;
        }

        OpenApiParse.Parsed parsed = OpenApiParse.parse(content);
        if (!parsed.ok()) {
            for (String m : parsed.messages()) {
                System.err.println("  " + m);
            }
            return 1;
        }
        OpenAPI document = parsed.document();
        if (document.getInfo() == null) {
            System.err.println("document has no info section; not an OpenAPI 3.x document?");
            return 1;
        }

        System.out.println("OpenAPI: " + document.getInfo().getTitle() + " " + document.getInfo().getVersion());
        System.out.println("Operations: " + countOperations(document));
        System.out.println("Schemas: " + countSchemas(document));

        if (outDir != null) {
            try {
                Files.createDirectories(Path.of(outDir));
                String target = outDir + File.separator + stripExtension(f.getName()) + ".generated.proto.txt";
                Files.writeString(Path.of(target), content);
                System.out.println("preview written to " + target);
            } catch (IOException ex) {
                System.err.println("error: " + ex.getMessage());
                return 1;
            }
        }
        return 0;
    }

    private static int countOperations(OpenAPI document) {
        if (document.getPaths() == null) return 0;
        int count = 0;
        for (io.swagger.v3.oas.models.PathItem pathItem : document.getPaths().values()) {
            if (pathItem == null) continue;
            count += pathItem.readOperationsMap().size();
        }
        return count;
    }

    private static int countSchemas(OpenAPI document) {
        if (document.getComponents() == null || document.getComponents().getSchemas() == null) return 0;
        return document.getComponents().getSchemas().size();
    }

    private static String stripExtension(String name) {
        int idx = name.lastIndexOf('.');
        return idx > 0 ? name.substring(0, idx) : name;
    }
}
