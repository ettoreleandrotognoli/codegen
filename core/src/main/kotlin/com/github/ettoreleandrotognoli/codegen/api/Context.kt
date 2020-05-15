package com.github.ettoreleandrotognoli.codegen.api

interface Context {

    val project: Project

    fun getRawSpec(codeSpec: CodeSpec): String
}