package io.github.ettoreleandrotognoli.codegen

interface CodeGenerator<T : CodeSpec> {

    fun accept(codeSpec: CodeSpec): Boolean

    fun generate(project: Project, codeSpec: T)

}