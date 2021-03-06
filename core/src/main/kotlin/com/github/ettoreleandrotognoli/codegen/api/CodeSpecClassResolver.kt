package com.github.ettoreleandrotognoli.codegen.api

import kotlin.reflect.KClass

interface CodeSpecClassResolver {
    fun resolve(className: String): KClass<out RawCodeSpec>
}