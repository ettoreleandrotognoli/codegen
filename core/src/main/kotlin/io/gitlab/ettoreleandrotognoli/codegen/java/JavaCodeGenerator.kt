package io.gitlab.ettoreleandrotognoli.codegen.java

import io.gitlab.ettoreleandrotognoli.codegen.CodeGenerator
import io.gitlab.ettoreleandrotognoli.codegen.CodeSpec
import kotlin.reflect.KClass

abstract class JavaCodeGenerator<T : CodeSpec>(
        val codeSpecType: KClass<T>
) : CodeGenerator<T> {

    override fun accept(codeSpec: CodeSpec): Boolean {
        return codeSpecType.isInstance(codeSpec)
    }
}