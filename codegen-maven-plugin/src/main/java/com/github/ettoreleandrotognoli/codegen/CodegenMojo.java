package com.github.ettoreleandrotognoli.codegen;

import com.github.ettoreleandrotognoli.codegen.loader.Engine;
import lombok.SneakyThrows;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mojo(
        name = "codegen",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyCollection = ResolutionScope.TEST,
        requiresDependencyResolution = ResolutionScope.TEST
)
public class CodegenMojo extends AbstractMojo {

    @Parameter(defaultValue = "${codegen.skip}")
    private boolean skip = false;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "src/main/codegen/")
    private String sources;

    @Parameter(defaultValue = "src/test/codegen/")
    private String testSources;

    @Parameter(defaultValue = "target/generated-sources/codegen/")
    private String generatedSources;

    @Parameter(defaultValue = "target/generated-test-sources/codegen/")
    private String generatedTestSources;

    @Parameter()
    private Map<String, Class<?>> factories = null;

    public void generateTestSources() {
        Path sourcesPath = new File(project.getBasedir(), testSources).toPath();
        if (!sourcesPath.toFile().exists()) return;
        Path outputPath = new File(project.getBasedir(), generatedTestSources).toPath();
        Engine.builder()
                .sourcesPath(sourcesPath)
                .outputPath(outputPath)
                .factories(factories)
                .build()
                .run();
    }

    public void generateSources() {
        Path sourcesPath = new File(project.getBasedir(), sources).toPath();
        if (!sourcesPath.toFile().exists()) return;
        Path outputPath = new File(project.getBasedir(), generatedSources).toPath();
        Engine.builder()
                .sourcesPath(sourcesPath)
                .outputPath(outputPath)
                .factories(factories)
                .build()
                .run();
    }

    @SneakyThrows
    @Override
    public void execute() {
        if (skip) return;
        project.addCompileSourceRoot(generatedSources);
        project.addTestCompileSourceRoot(generatedTestSources);
        if (factories == null) {
            factories = new HashMap<>();
            factories.put(List.class.getCanonicalName(), ArrayList.class);
            factories.put(Map.class.getCanonicalName(), HashMap.class);
        }
        generateSources();
        generateTestSources();
    }

}
