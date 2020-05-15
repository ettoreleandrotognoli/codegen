package com.github.ettoreleandrotognoli.codegen.api

import com.github.ettoreleandrotognoli.codegen.api.CodeGenerator
import kotlin.reflect.KClass

interface CodeGeneratorResolver {

    fun <T : CodeSpec> resolve(codeSpecType: KClass<T>): List<CodeGenerator<T>>
}