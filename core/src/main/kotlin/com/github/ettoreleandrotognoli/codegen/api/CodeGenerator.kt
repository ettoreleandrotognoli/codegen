package com.github.ettoreleandrotognoli.codegen.api

import kotlin.reflect.KClass

interface CodeGenerator<T : CodeSpec> {

    fun specType(): KClass<T>

    fun accept(codeSpec: CodeSpec): Boolean

    fun preProcess(context: Context, codeSpec: T): List<Any>

    fun tryPreProcess(context: Context, codeSpec: CodeSpec): List<Any>

    fun generate(context: Context, codeSpec: T)

    fun tryGenerate(context: Context, codeSpec: CodeSpec)

}