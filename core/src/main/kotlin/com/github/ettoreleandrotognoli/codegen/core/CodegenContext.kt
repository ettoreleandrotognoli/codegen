package com.github.ettoreleandrotognoli.codegen.core

import com.github.ettoreleandrotognoli.codegen.api.CodeSpec
import com.github.ettoreleandrotognoli.codegen.api.Context
import com.github.ettoreleandrotognoli.codegen.api.Project
import kotlin.reflect.KClass

class CodegenContext(
        override val project: Project,
        private val specs: MutableMap<CodeSpec, String> = HashMap(),
        private val references: MutableMap<KClass<*>, MutableList<Any>> = HashMap()
) : Context {

    override fun getRawSpec(codeSpec: CodeSpec): String {
        return specs[codeSpec]!!
    }

    fun putRawSpec(codeSpec: CodeSpec, rawSpec: String) {
        specs[codeSpec] = rawSpec
    }

    override fun <T : Any> getReference(type: KClass<T>): List<T> {
        return references.getOrDefault(type, emptyList<T>()) as List<T>
    }

    fun putReference(reference: Any) {
        val references = this.references.getOrPut(reference::class, { mutableListOf() })
        references.add(reference)
    }
}