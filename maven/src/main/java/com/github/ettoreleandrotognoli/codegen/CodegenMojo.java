package com.github.ettoreleandrotognoli.codegen;

import com.github.ettoreleandrotognoli.codegen.api.Codegen;
import com.github.ettoreleandrotognoli.codegen.api.Context;
import com.github.ettoreleandrotognoli.codegen.api.impl.CodegenContext;
import com.github.ettoreleandrotognoli.codegen.loader.CodegenReader;
import lombok.SneakyThrows;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Mojo(name = "codegen", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CodegenMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "src/main/codegen/")
    private String sources;

    @Parameter(defaultValue = "target/generated-sources/codegen/")
    private String generatedSources;

    @SneakyThrows
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path sourcesPath = new File(project.getBasedir(), this.sources).toPath();
        List<Codegen> codegenList = new LinkedList<>();
        project.addCompileSourceRoot(generatedSources);
        CodegenReader reader = CodegenReader.createDefault();
        for(Path file : Files.list(sourcesPath).collect(Collectors.toList())) {
            try(InputStream inputStream = new FileInputStream(file.toFile())) {
                reader.read(inputStream).subscribe(codegenList::add);
            }
        }
        Context.Builder contextBuilder = new CodegenContext.Builder(project.getBasedir());
        for (Codegen codegen : codegenList) {
            contextBuilder.register(codegen);
        }
        for (Codegen codegen : codegenList) {
            codegen.prepare(contextBuilder);
        }
        Context context = contextBuilder.build();
        for (Codegen codegen : codegenList) {
            System.out.println(codegen);
            codegen.generate(context);
        }

    }

}
