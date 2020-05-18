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

@SpringBootApplication
@ComponentScan("com.github.ettoreleandrotognoli.codegen")
open class CodegenEngine(
        private val codeGenerator: CodegenGenerator,
        private val yaml: Yaml,
        private val snakeYaml: SnakeYaml
) {

    @ImplicitReflectionSerializer
    fun prepareSpecs(context: CodegenContext, file: File): List<CodegenSpec> {
        val content = file.inputStream().asString()
        val specs = yaml.parse(CodegenSpec.serializer().list, content)
        val rawSpecs = snakeYaml.loadAll(content).toList().first() as List<Any>
        specs.zip(rawSpecs)
                .forEach {
                    val rawSpec = snakeYaml.dump(it.second)
                    context.putRawSpec(it.first, rawSpec)
                }
        return specs
    }


    @ImplicitReflectionSerializer
    fun processFiles(project: Project, files: List<File>) {
        val context = CodegenContext(project)
        val spec = files
                .flatMap { prepareSpecs(context, it) }
        spec.forEach { codeGenerator.preProcess(context, it) }
        spec.forEach { codeGenerator.generate(context, it) }
    }

    companion object {

        fun getInstance(): CodegenEngine {
            val application = SpringApplicationBuilder(CodegenEngine::class.java).build()
            val context = application.run()
            return context.getBean(CodegenEngine::class.java)
        }

    }


}