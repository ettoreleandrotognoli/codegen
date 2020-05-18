package com.github.ettoreleandrotognoli.codegen.core

import com.github.ettoreleandrotognoli.codegen.api.CodeGenerator
import com.github.ettoreleandrotognoli.codegen.api.CodeSpec
import com.github.ettoreleandrotognoli.codegen.api.Context
import kotlin.reflect.KClass

abstract class AbstractCodeGenerator<T : CodeSpec>(
        val codeSpecType: KClass<T>
) : CodeGenerator<T> {

    override fun specType(): KClass<T> {
        return codeSpecType
    }

    override fun preProcess(context: Context, codeSpec: T): List<Any> {
        return emptyList()
    }

    override fun tryPreProcess(context: Context, codeSpec: CodeSpec): List<Any> {
        if (!accept(codeSpec)) {
            return emptyList()
        }
        return preProcess(context, codeSpec as T)
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