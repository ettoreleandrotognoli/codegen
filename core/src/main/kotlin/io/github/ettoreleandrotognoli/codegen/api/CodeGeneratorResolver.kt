package io.github.ettoreleandrotognoli.codegen.api

import kotlin.reflect.KClass

interface CodeGeneratorResolver {

    fun <T : CodeSpec> resolve(codeSpecType: KClass<T>): List<CodeGenerator<T>>
}