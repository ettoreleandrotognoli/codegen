package com.github.ettoreleandrotognoli.codegen.api

import com.squareup.javapoet.ClassName
import kotlin.reflect.KClass

interface PreBuildContext {


    fun getIntentions(): List<ClassName>

    fun <T : Any> getSpec(infoType: KClass<T>): List<T>


    interface Mutable : PreBuildContext {

        fun registerIntention(type: ClassName)

        fun registerPreSpec(specInfo: Any)

    }

}