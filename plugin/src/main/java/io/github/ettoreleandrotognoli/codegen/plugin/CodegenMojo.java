package io.github.ettoreleandrotognoli.codegen.plugin;

import io.github.ettoreleandrotognoli.codegen.api.Project;
import io.github.ettoreleandrotognoli.codegen.core.CodegenEngine;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Mojo(name = "codegen", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CodegenMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;


    @Parameter(defaultValue = "target/generated-sources/codegen/", required = true, readonly = true)
    String outputDirectory;

    @Parameter(defaultValue = "src/main/codegen/main.yml")
    String codegenFiles;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        System.out.println(this.project.getBasedir());
        System.out.println(this.codegenFiles);
        File outputDirectory = new File(project.getBasedir(), this.outputDirectory);
        outputDirectory.mkdirs();
        project.addCompileSourceRoot(outputDirectory.getPath());
        System.out.println(outputDirectory);
        List<File> codegenFiles = Collections.singletonList(
                new File(this.project.getBasedir(), this.codegenFiles)
        );
        Stream<InputStream> input = codegenFiles.stream().map(file -> {
            try {
                return (InputStream) new FileInputStream(file);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
                return null;
            }
        }).filter(it -> it != null);
        Project project = new Project.DTO(
                this.project.getBasedir(),
                new File(this.project.getBasedir(),"target"),
                outputDirectory
        );
        CodegenEngine.Companion.getInstance().process(project, input);
    }
}