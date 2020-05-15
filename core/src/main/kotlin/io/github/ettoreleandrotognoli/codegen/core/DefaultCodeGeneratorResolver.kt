package io.github.ettoreleandrotognoli.codegen.core

import io.github.ettoreleandrotognoli.codegen.api.CodeGenerator
import io.github.ettoreleandrotognoli.codegen.api.CodeGeneratorResolver
import io.github.ettoreleandrotognoli.codegen.api.CodeSpec
import org.springframework.stereotype.Component
import java.util.*
import kotlin.reflect.KClass

class DefaultCodeGeneratorResolver(
        private val generators: Map<KClass<out CodeSpec>, List<CodeGenerator<*>>>
) : CodeGeneratorResolver {

    override fun <T : CodeSpec> resolve(codeSpecType: KClass<T>): List<CodeGenerator<T>> {
        return generators.getOrDefault(codeSpecType, Collections.emptyList()) as List<CodeGenerator<T>>
    }
}