package io.github.mpcoredeveloper.javaportico.maven;

import io.github.mpcoredeveloper.javaportico.annotations.JavaPorticoOptions;
import io.github.mpcoredeveloper.javaportico.emit.ProtoEmitter;
import io.github.mpcoredeveloper.javaportico.emit.ProxyJavaEmitter;
import io.github.mpcoredeveloper.javaportico.mapping.OpenApiParser;
import io.github.mpcoredeveloper.javaportico.mapping.ParseResult;
import io.github.mpcoredeveloper.javaportico.model.GrpcModel;
import io.github.mpcoredeveloper.javaportico.model.WorkItem;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * {@code javaportico:generate} — reads OpenAPI specs (from the POM configuration or
 * {@code src/main/openapi}) and emits {@code .proto} files plus the gRPC-to-REST proxy Java source.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true)
public class GenerateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /** Explicit {@code <specs><spec>...} configuration. When empty, {@code src/main/openapi} is scanned. */
    @Parameter
    private List<SpecConfig> specs;

    @Parameter(defaultValue = "src/main/openapi", property = "javaportico.specDirectory")
    private String specDirectory;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/protobuf",
            property = "javaportico.protoOutputDirectory")
    private File protoOutputDirectory;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/javaportico",
            property = "javaportico.javaOutputDirectory")
    private File javaOutputDirectory;

    @Parameter(defaultValue = "false", property = "javaportico.skip")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("JavaPortico generation skipped (javaportico.skip=true).");
            return;
        }
        List<SpecConfig> resolved = resolveSpecs();
        if (resolved.isEmpty()) {
            getLog().info("JavaPortico: no OpenAPI specs found.");
            return;
        }
        for (SpecConfig spec : resolved) {
            try {
                generate(spec);
            } catch (IOException e) {
                throw new MojoExecutionException("JavaPortico: failed to generate " + spec.getFile(), e);
            }
        }
    }

    private List<SpecConfig> resolveSpecs() {
        if (specs != null && !specs.isEmpty()) return specs;
        List<SpecConfig> found = new ArrayList<>();
        File dir = new File(project.getBasedir(), specDirectory);
        if (!dir.isDirectory()) return found;
        try (Stream<Path> stream = Files.walk(dir.toPath())) {
            stream.filter(p -> {
                String n = p.getFileName().toString().toLowerCase();
                return n.endsWith(".yaml") || n.endsWith(".yml") || n.endsWith(".json");
            }).forEach(p -> {
                SpecConfig c = new SpecConfig();
                c.setFile(project.getBasedir().toPath().relativize(p).toString().replace('\\', '/'));
                found.add(c);
            });
        } catch (IOException e) {
            getLog().warn("JavaPortico: failed to scan " + dir, e);
        }
        return found;
    }

    private void generate(SpecConfig spec) throws IOException {
        File file = new File(spec.getFile());
        if (!file.isAbsolute()) file = new File(project.getBasedir(), spec.getFile());
        if (!file.isFile()) {
            getLog().warn("JavaPortico: spec file not found: " + file);
            return;
        }
        String content = Files.readString(file.toPath());
        String hint = stripExtension(file.getName());
        JavaPorticoOptions options = spec.toOptions();

        WorkItem item = WorkItem.builder()
                .filePath(spec.getFile())
                .hintName(hint)
                .serviceName(spec.getServiceName())
                .namespaceName(spec.getNamespace())
                .content(content)
                .options(options)
                .build();

        ParseResult result = OpenApiParser.parseAndMap(item);
        if (!result.isSuccess()) {
            getLog().error("JavaPortico: failed to parse " + spec.getFile() + ": " + result.diagnostics());
            return;
        }
        GrpcModel model = result.model();

        if (options.isEmitProtoFile()) {
            String proto = ProtoEmitter.emit(model, item);
            File out = new File(protoOutputDirectory, hint + ".proto");
            Files.createDirectories(out.getParentFile().toPath());
            Files.writeString(out.toPath(), proto);
            getLog().debug("JavaPortico: wrote " + out);
        }

        if (options.isEnableProxyGeneration()) {
            String java = ProxyJavaEmitter.emit(model, item);
            File out = new File(javaOutputDirectory,
                    model.namespace().replace('.', '/') + "/" + model.serviceName() + "Proxy.java");
            Files.createDirectories(out.getParentFile().toPath());
            Files.writeString(out.toPath(), java);
            project.addCompileSourceRoot(javaOutputDirectory.getAbsolutePath());
            getLog().debug("JavaPortico: wrote " + out);
        }

        getLog().info("JavaPortico: generated " + model.serviceName() + " from " + spec.getFile()
                + " (" + model.services().get(0).rpcMethods().size() + " RPCs, "
                + model.messages().size() + " messages"
                + (options.isEnableProxyGeneration() ? ", proxy enabled" : "") + ")");
    }

    private static String stripExtension(String name) {
        int idx = name.lastIndexOf('.');
        return idx > 0 ? name.substring(0, idx) : name;
    }
}
