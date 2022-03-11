package com.github.ettoreleandrotognoli.codegen;

import com.github.ettoreleandrotognoli.codegen.api.Codegen;
import com.github.ettoreleandrotognoli.codegen.api.Context;
import com.github.ettoreleandrotognoli.codegen.api.impl.CodegenContext;
import com.github.ettoreleandrotognoli.codegen.loader.CodegenReader;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import lombok.SneakyThrows;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Mojo(
        name = "codegen",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyCollection = ResolutionScope.TEST,
        requiresDependencyResolution = ResolutionScope.TEST

)
public class CodegenMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "src/main/codegen/")
    private String sources;

    @Parameter(defaultValue = "target/generated-sources/codegen/")
    private String generatedSources;

    @Parameter()
    private Map<String, Class<?>> factories = null;

    @SneakyThrows
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (factories == null) {
            factories = new HashMap<>();
            factories.put(List.class.getCanonicalName(), ArrayList.class);
            factories.put(Map.class.getCanonicalName(), HashMap.class);
        }
        Path sourcesPath = new File(project.getBasedir(), this.sources).toPath();
        Path outputPath = new File(project.getBasedir(), this.generatedSources).toPath();
        List<Codegen> codegenList = new LinkedList<>();
        project.addCompileSourceRoot(generatedSources);
        CodegenReader reader = CodegenReader.createDefault();
        for (Path file : Files.list(sourcesPath).collect(Collectors.toList())) {
            try (InputStream inputStream = new FileInputStream(file.toFile())) {
                reader.read(inputStream).subscribe(codegenList::add);
            }
        }
        Context.Builder contextBuilder = new CodegenContext.Builder(project.getBasedir(), factories);
        for (Codegen codegen : codegenList) {
            contextBuilder.register(codegen);
        }
        for (Codegen codegen : codegenList) {
            codegen.prepare(contextBuilder);
        }
        Context context = contextBuilder.build();
        for (Codegen codegen : codegenList) {
            codegen.generate(context);
        }
        context.getBuilders()
                .forEach(entry -> {
                    ClassName className = entry.getKey();
                    TypeSpec.Builder classBuilder = entry.getValue();
                    JavaFile.Builder fileBuilder = JavaFile.builder(className.packageName(), classBuilder.build());
                    JavaFile javaFile = fileBuilder.skipJavaLangImports(true).build();
                    try {
                        javaFile.writeTo(outputPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

    }

}
