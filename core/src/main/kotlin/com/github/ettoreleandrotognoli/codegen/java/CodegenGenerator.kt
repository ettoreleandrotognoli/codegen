package com.github.ettoreleandrotognoli.codegen.java

import com.charleskorn.kaml.Yaml
import com.github.ettoreleandrotognoli.codegen.api.CodeGeneratorResolver
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
    fun generate(context: CodegenContext, codeSpec: CodegenSpec) {
        val rawSpec = context.getRawSpec(codeSpec)
        codeSpec.codegen
                .map { codeSpecClassResolver.resolve(it) }
                .map { yaml.parse(it.serializer(), rawSpec) }
                .also { it.forEach { codeSpec -> context.putRawSpec(codeSpec, rawSpec) } }
                .flatMap { codeSpec -> codeGeneratorResolver.resolve(codeSpec::class).map { Pair(codeSpec, it) } }
                .forEach {
                    it.second.tryGenerate(context, it.first)
                }
    }
}