package com.github.ettoreleandrotognoli.codegen.core

import com.github.ettoreleandrotognoli.codegen.api.CodeGenerator
import com.github.ettoreleandrotognoli.codegen.api.CodeGeneratorResolver
import com.github.ettoreleandrotognoli.codegen.api.RawCodeSpec
import java.util.*
import kotlin.reflect.KClass

class DefaultCodeGeneratorResolver(
        private val generators: Map<KClass<out RawCodeSpec>, List<CodeGenerator<*>>>
) : CodeGeneratorResolver {

    override fun <T : RawCodeSpec> resolve(codeSpecType: KClass<T>): List<CodeGenerator<T>> {
        return generators.getOrDefault(codeSpecType, Collections.emptyList()) as List<CodeGenerator<T>>
    }
}