package com.github.ettoreleandrotognoli.codegen.api

import kotlin.reflect.KClass

interface CodeGeneratorResolver {

    fun <T : RawCodeSpec> resolve(codeSpecType: KClass<T>): List<CodeGenerator<T>>
}