package io.github.ettoreleandrotognoli.codegen.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

@Mojo(name = "codegen", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CodegenMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;


    @Parameter(defaultValue = "${project.build.directory}/generated-sources/codegen", required = true, readonly = true)
    File outputDirectory;

    @Parameter(defaultValue = "${project.baseDir}/src/main/codegen/main.yml")
    File[] codegenFiles;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        
    }
}