package com.github.ettoreleandrotognoli.codegen.core

import com.github.ettoreleandrotognoli.codegen.api.CodeSpec
import com.github.ettoreleandrotognoli.codegen.api.CodeSpecClassResolver
import kotlin.reflect.KClass

class DefaultCodeSpecClassResolver(
        val aliases: Map<String, KClass<out CodeSpec>>
) : CodeSpecClassResolver {

    private fun forName(className: String): KClass<CodeSpec> {
        val loadedClass = Class.forName(className).kotlin
        if (!CodeSpec::class.java.isAssignableFrom(loadedClass.java.javaClass)) {
            throw Exception("The class $className is not a CodeSpec")
        }
        return loadedClass as KClass<CodeSpec>
    }

    override fun resolve(className: String): KClass<out CodeSpec> {
        return aliases.getOrElse(className, { forName(className) })
    }
}