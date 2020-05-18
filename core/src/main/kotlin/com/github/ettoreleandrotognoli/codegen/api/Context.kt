package com.github.ettoreleandrotognoli.codegen.api

import kotlin.reflect.KClass

interface Context {

    val project: Project

    fun getRawSpec(codeSpec: CodeSpec): String

    fun <T : Any> getReference(type: KClass<T>): List<T>
}