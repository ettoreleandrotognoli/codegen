package com.github.ettoreleandrotognoli.codegen.core

import com.charleskorn.kaml.Yaml
import com.github.ettoreleandrotognoli.codegen.api.CodeGenerator
import com.github.ettoreleandrotognoli.codegen.api.CodeGeneratorResolver
import com.github.ettoreleandrotognoli.codegen.api.RawCodeSpec
import com.github.ettoreleandrotognoli.codegen.api.CodeSpecClassResolver
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
    fun prepareGenerators(context: CodegenContext, codeSpec: CodegenSpec): List<Pair<RawCodeSpec, CodeGenerator<out RawCodeSpec>>> {
        val rawSpec = context.getSpec(codeSpec)
        return codeSpec.codegen
                .map { codeSpecClassResolver.resolve(it) }
                .map { yaml.parse(it.serializer(), rawSpec) }
                .also { it.forEach { codeSpec -> context.registerRawSpec(codeSpec, rawSpec) } }
                .flatMap { codeSpec -> codeGeneratorResolver.resolve(codeSpec::class).map { Pair(codeSpec, it) } }
    }

    @ImplicitReflectionSerializer
    fun preProcess(context: CodegenContext, codeSpec: CodegenSpec) {
        prepareGenerators(context, codeSpec)
                .filter { it.second.accept(it.first) }
                .forEach { it.second.prepareContext(context, it.first) }
    }

    @ImplicitReflectionSerializer
    fun generate(context: CodegenContext, codeSpec: CodegenSpec) {
        prepareGenerators(context, codeSpec)
                .forEach {
                    it.second.generate(context, it.first)
                }
    }
}