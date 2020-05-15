package com.github.ettoreleandrotognoli.codegen.core

import com.charleskorn.kaml.Yaml
import com.github.ettoreleandrotognoli.codegen.SnakeYaml
import com.github.ettoreleandrotognoli.codegen.api.Project
import com.github.ettoreleandrotognoli.codegen.asString
import com.github.ettoreleandrotognoli.codegen.java.CodegenGenerator
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.builtins.list
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.ComponentScan
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.stream.Stream

@SpringBootApplication
@ComponentScan("com.github.ettoreleandrotognoli.codegen")
open class CodegenEngine(
        private val codeGenerator: CodegenGenerator,
        private val yaml: Yaml,
        private val snakeYaml: SnakeYaml
) {

    @ImplicitReflectionSerializer
    fun processFile(context: CodegenContext, file: InputStream) {
        val content = file.asString()
        val specs = yaml.parse(CodegenSpec.serializer().list, content)
        val rawSpecs = snakeYaml.loadAll(content).toList().first() as List<Any>
        specs.zip(rawSpecs)
                .forEach {
                    val rawSpec = snakeYaml.dump(it.second)
                    context.putRawSpec(it.first, rawSpec)
                    codeGenerator.generate(context, it.first)
                }

    }

    @ImplicitReflectionSerializer
    fun process(project: Project, files: Stream<InputStream>) {
        val context = CodegenContext(project)
        files.forEach {
            processFile(context, it)
        }
    }

    @ImplicitReflectionSerializer
    fun process(project: Project, files: List<File>) {
        val filesStream = files.stream().map { FileInputStream(it) as InputStream }
        process(project, filesStream)
    }

    companion object {

        fun getInstance(): CodegenEngine {
            val application = SpringApplicationBuilder(CodegenEngine::class.java).build()
            val context = application.run()
            return context.getBean(CodegenEngine::class.java)
        }

    }


}