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
import java.util.stream.Stream;

@Mojo(name = "codegen", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CodegenMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;


    @Parameter(defaultValue = "${project.build.directory}/generated-sources/codegen", required = true, readonly = true)
    File outputDirectory;

    @Parameter(defaultValue = "${project.baseDir}/src/main/codegen/main.yml")
    File codegenFiles;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Stream<InputStream> input = Collections.singletonList(codegenFiles).stream().map(file -> {
            try {
                return (InputStream) new FileInputStream(file);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
                return null;
            }
        }).filter(it -> it != null);
        Project project = new Project.DTO(this.project.getBasedir());
        CodegenEngine.Companion.getInstance().process(project, input);
    }
}