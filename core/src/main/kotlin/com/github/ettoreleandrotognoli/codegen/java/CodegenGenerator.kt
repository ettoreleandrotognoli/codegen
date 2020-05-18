package com.github.ettoreleandrotognoli.codegen.java

import com.charleskorn.kaml.Yaml
import com.github.ettoreleandrotognoli.codegen.api.CodeGenerator
import com.github.ettoreleandrotognoli.codegen.api.CodeGeneratorResolver
import com.github.ettoreleandrotognoli.codegen.api.CodeSpec
import com.github.ettoreleandrotognoli.codegen.api.CodeSpecClassResolver
import com.github.ettoreleandrotognoli.codegen.core.CodegenContext
import com.github.ettoreleandrotognoli.codegen.core.CodegenSpec
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.serializer
import org.springframework.stereotype.Component

@Component
class CodegenGenerator(
        private val codeSpecClassResolver: CodeSpecClassResolver,
        private val codeGeneratorResolver: CodeGeneratorResolver,
        private val yaml: Yaml
) {

    @ImplicitReflectionSerializer
    fun prepareGenerators(context: CodegenContext, codeSpec: CodegenSpec): List<Pair<CodeSpec, CodeGenerator<out CodeSpec>>> {
        val rawSpec = context.getRawSpec(codeSpec)
        return codeSpec.codegen
                .map { codeSpecClassResolver.resolve(it) }
                .map { yaml.parse(it.serializer(), rawSpec) }
                .also { it.forEach { codeSpec -> context.putRawSpec(codeSpec, rawSpec) } }
                .flatMap { codeSpec -> codeGeneratorResolver.resolve(codeSpec::class).map { Pair(codeSpec, it) } }
    }

    @ImplicitReflectionSerializer
    fun preProcess(context: CodegenContext, codeSpec: CodegenSpec) {
        prepareGenerators(context, codeSpec)
                .flatMap { it.second.tryPreProcess(context, it.first) }
                .forEach(context::putReference)
    }

    @ImplicitReflectionSerializer
    fun generate(context: CodegenContext, codeSpec: CodegenSpec) {
        prepareGenerators(context, codeSpec)
                .forEach {
                    it.second.tryGenerate(context, it.first)
                }
    }
}