package io.gitlab.ettoreleandrotognoli.codegen

interface CodeGenerator<T : CodeSpec> {

    fun accept(codeSpec: CodeSpec): Boolean

    fun generate(project: Project, codeSpec: T)

}