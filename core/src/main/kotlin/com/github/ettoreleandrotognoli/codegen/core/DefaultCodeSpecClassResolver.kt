package com.github.ettoreleandrotognoli.codegen.core

import com.github.ettoreleandrotognoli.codegen.api.RawCodeSpec
import com.github.ettoreleandrotognoli.codegen.api.CodeSpecClassResolver
import kotlin.reflect.KClass

class DefaultCodeSpecClassResolver(
        val aliases: Map<String, KClass<out RawCodeSpec>>
) : CodeSpecClassResolver {

    private fun forName(className: String): KClass<RawCodeSpec> {
        val loadedClass = Class.forName(className).kotlin
        if (!RawCodeSpec::class.java.isAssignableFrom(loadedClass.java.javaClass)) {
            throw Exception("The class $className is not a CodeSpec")
        }
        return loadedClass as KClass<RawCodeSpec>
    }

    override fun resolve(className: String): KClass<out RawCodeSpec> {
        return aliases.getOrElse(className, { forName(className) })
    }
}