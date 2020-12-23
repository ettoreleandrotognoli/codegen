package com.github.ettoreleandrotognoli.codegen.generator.entity

import com.github.ettoreleandrotognoli.codegen.api.BaseSpec
import com.github.ettoreleandrotognoli.codegen.api.PreBuildContext
import com.github.ettoreleandrotognoli.codegen.generator.asType
import com.github.ettoreleandrotognoli.codegen.generator.data.DataClassSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import java.util.*
import kotlin.collections.HashMap

class EntitySpec(
        type: ClassName,
        val of: ClassName,
        val extends: TypeName,
        val implements: List<TypeName>,
        val primaryKey: List<String>,
        val dataclass: Optional<DataClassSpec> = Optional.empty(),
        val propertyTypes: Map<String, TypeName>,
        val concreteTypes: Map<TypeName, TypeName>,
        val name: String
) : BaseSpec(type) {

    companion object {

        fun from(rawSpec: EntityRawSpec, context: PreBuildContext? = null): EntitySpec {
            val type = ClassName.get(rawSpec.packageName, rawSpec.name).nestedClass("Entity")
            val of = ClassName.get(rawSpec.packageName, rawSpec.name)
            val extends = asType(rawSpec.entity.extends)
            val implements = rawSpec.entity.implements.map { asType(it) }
            if (context == null) {
                return EntitySpec(
                        type = type,
                        of = of,
                        extends = extends,
                        implements = implements,
                        primaryKey = rawSpec.primaryKey,
                        propertyTypes = emptyMap(),
                        concreteTypes = emptyMap(),
                        name = rawSpec.entity.name ?: rawSpec.name
                )
            }

            val optionalDataclass = context.getSpec(DataClassSpec::class)
                    .filter { it.type.packageName() == rawSpec.packageName }
                    .filter { it.type.simpleName() == rawSpec.name }
                    .firstOrNull()
                    .let { Optional.ofNullable(it) }

            val dataclass = optionalDataclass.orElseThrow { Exception("A dataclass for $of was not defined") }

            val entityTypes: MutableMap<TypeName, TypeName> = context.getSpec(EntitySpec::class)
                    .map { Pair(it.of, it.type) }
                    .toMap(HashMap())

            context.getSpec(DataClassSpec::class)
                    .forEach { entityTypes.putIfAbsent(it.type, it.dtoType) }

            val propertyTypes = dataclass.properties
                    .map { Pair(it, entityTypes.getOrDefault(dataclass.propertyType[it]!!, dataclass.propertyType[it]!!)) }
                    .toMap(HashMap())

            propertyTypes
                    .entries
                    .filter { it.value is ParameterizedTypeName }
                    .map { Pair(it.key, it.value as ParameterizedTypeName) }
                    .map { Pair(it.first, ParameterizedTypeName.get(it.second.rawType, *it.second.typeArguments.map { t -> entityTypes.getOrDefault(t, t) }.toTypedArray())) }
                    .toMap()
                    .let { propertyTypes.putAll(it) }


            return EntitySpec(
                    type = type,
                    of = of,
                    extends = extends,
                    implements = implements,
                    dataclass = optionalDataclass,
                    primaryKey = rawSpec.primaryKey,
                    propertyTypes = propertyTypes,
                    concreteTypes = entityTypes,
                    name = rawSpec.entity.name ?: rawSpec.name
            )
        }

    }

}