package io.github.ettoreleandrotognoli.codegen.core

import io.github.ettoreleandrotognoli.codegen.api.CodeGenerator
import io.github.ettoreleandrotognoli.codegen.api.CodeSpec
import io.github.ettoreleandrotognoli.codegen.api.Context
import kotlin.reflect.KClass

abstract class AbstractCodeGenerator<T : CodeSpec>(
        val codeSpecType: KClass<T>
) : CodeGenerator<T> {

    override fun specType(): KClass<T> {
        return codeSpecType
    }

    override fun accept(codeSpec: CodeSpec): Boolean {
        return codeSpecType.isInstance(codeSpec)
    }

    override fun tryGenerate(context: Context, codeSpec: CodeSpec) {
        if (!accept(codeSpec)) {
            return
        }
        generate(context, codeSpec as T)
    }
}