package com.github.ettoreleandrotognoli.codegen.core

import com.github.ettoreleandrotognoli.codegen.api.BuildContext
import com.github.ettoreleandrotognoli.codegen.api.PreBuildContext
import com.github.ettoreleandrotognoli.codegen.api.RawCodeSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.util.*
import java.util.stream.Stream
import kotlin.collections.HashMap
import kotlin.reflect.KClass

class CodegenContext(
        private val intentions: MutableList<ClassName> = LinkedList(),
        private val rawSpec: MutableMap<RawCodeSpec, String> = HashMap(),
        private val specs: MutableMap<KClass<*>, MutableSet<Any>> = HashMap(),
        private val typeSpecBuilders: MutableMap<ClassName, TypeSpec.Builder> = HashMap()
) : BuildContext.Mutable, PreBuildContext.Mutable {

    override fun getSpec(codeSpec: RawCodeSpec): String {
        return rawSpec[codeSpec]!!
    }

    fun registerRawSpec(codeSpec: RawCodeSpec, rawSpec: String) {
        this.rawSpec[codeSpec] = rawSpec
    }

    override fun getIntentions(): List<ClassName> {
        return intentions
    }

    override fun registerIntention(type: ClassName) {
        intentions.add(type)
    }

    override fun <T : Any> getSpec(type: KClass<T>): List<T> {
        return specs.getOrDefault(type, emptySet<T>()).toList() as List<T>
    }

    override fun registerPreSpec(reference: Any) {
        val references = this.specs.getOrPut(reference::class, { mutableSetOf() })
        references.add(reference)
    }

    override fun getTypeSpecBuilder(type: TypeName): Optional<TypeSpec.Builder> {
        return Optional.ofNullable(typeSpecBuilders[type])
    }

    override fun registerTypeSpecBuilder(type: ClassName, builder: TypeSpec.Builder) {
        typeSpecBuilders[type] = builder
    }

    override fun getAllTypeSpecBuilders(): Stream<Pair<ClassName, TypeSpec.Builder>> {
        return typeSpecBuilders.entries.map { Pair(it.key, it.value) }.stream()
    }

    override fun registerFullSpec(spec: Any) {
        registerPreSpec(spec)
    }
}