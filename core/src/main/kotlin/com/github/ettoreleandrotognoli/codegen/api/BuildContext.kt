package com.github.ettoreleandrotognoli.codegen.api

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.util.*
import java.util.stream.Stream

interface BuildContext : PreBuildContext {

    fun getSpec(codeSpec: RawCodeSpec): String

    fun getTypeSpecBuilder(type: TypeName): Optional<TypeSpec.Builder>

    fun getAllTypeSpecBuilders(): Stream<Pair<ClassName, TypeSpec.Builder>>

    interface Mutable : BuildContext {

        fun registerFullSpec(spec : Any)

        fun registerTypeSpecBuilder(type: ClassName, builder: TypeSpec.Builder)

    }

}