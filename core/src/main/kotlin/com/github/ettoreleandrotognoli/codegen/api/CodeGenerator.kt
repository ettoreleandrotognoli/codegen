package com.github.ettoreleandrotognoli.codegen.api

import kotlin.reflect.KClass

interface CodeGenerator<T : RawCodeSpec> {

    fun specType(): KClass<T>

    fun accept(codeSpec: RawCodeSpec): Boolean

    fun prepareContext(context: PreBuildContext.Mutable, codeSpec: RawCodeSpec)

    fun generate(context: BuildContext.Mutable, codeSpec: RawCodeSpec)


}