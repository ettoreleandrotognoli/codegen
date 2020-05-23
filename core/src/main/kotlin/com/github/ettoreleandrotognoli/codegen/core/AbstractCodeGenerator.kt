package com.github.ettoreleandrotognoli.codegen.core

import com.github.ettoreleandrotognoli.codegen.api.BuildContext
import com.github.ettoreleandrotognoli.codegen.api.CodeGenerator
import com.github.ettoreleandrotognoli.codegen.api.RawCodeSpec
import com.github.ettoreleandrotognoli.codegen.api.PreBuildContext
import kotlin.reflect.KClass


abstract class AbstractCodeGenerator<T : RawCodeSpec>(
        private val codeSpecType: KClass<T>
) : CodeGenerator<T> {

    override fun specType(): KClass<T> {
        return codeSpecType
    }

    override fun accept(codeSpec: RawCodeSpec): Boolean {
        return codeSpecType.isInstance(codeSpec)
    }

    @Suppress("UNCHECKED_CAST")
    fun ensureSpecType(codeSpec: RawCodeSpec): T {
        if (accept(codeSpec)) return codeSpec as T
        throw Exception("The generator '${javaClass.name}' can't process the spec '${codeSpec.javaClass.name}'")
    }

    protected abstract fun typedPrepareContext(context: PreBuildContext.Mutable, rawSpec: T)


    override fun prepareContext(context: PreBuildContext.Mutable, codeSpec: RawCodeSpec) {
        typedPrepareContext(context, ensureSpecType(codeSpec))
    }

    protected abstract fun typedGenerate(context: BuildContext.Mutable, rawSpec: T)

    override fun generate(context: BuildContext.Mutable, codeSpec: RawCodeSpec) {
        typedGenerate(context, ensureSpecType(codeSpec))
    }
}