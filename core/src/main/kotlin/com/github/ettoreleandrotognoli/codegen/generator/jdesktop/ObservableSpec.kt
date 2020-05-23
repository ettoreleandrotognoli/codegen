package com.github.ettoreleandrotognoli.codegen.generator.jdesktop

import com.github.ettoreleandrotognoli.codegen.api.BaseSpec
import com.github.ettoreleandrotognoli.codegen.api.PreBuildContext
import com.github.ettoreleandrotognoli.codegen.generator.asType
import com.github.ettoreleandrotognoli.codegen.generator.data.DataClassSpec
import com.github.ettoreleandrotognoli.codegen.generator.data.OBSERVABLE_COLLECTIONS_MAP
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import java.util.*
import kotlin.collections.HashMap

class ObservableSpec(
        type: ClassName,
        val of: ClassName,
        val extends: TypeName,
        val implements: List<TypeName>,
        val dataclass: Optional<DataClassSpec>,
        val propertyType: Map<String, TypeName>,
        val observableTypes: Map<ClassName, ClassName>,
        val dtoTypes: Map<ClassName, ClassName>
) : BaseSpec(type) {
    companion object {
        fun from(rawSpec: ObservableRawSpec, dataclass: Optional<DataClassSpec>, context: PreBuildContext?): ObservableSpec {

            if (!dataclass.isPresent) return ObservableSpec(
                    type = ClassName.get(rawSpec.packageName, rawSpec.name).nestedClass("Observable"),
                    of = ClassName.get(rawSpec.packageName, rawSpec.name),
                    extends = asType(rawSpec.observable.extends),
                    implements = rawSpec.observable.implements.map { asType(it) },
                    dataclass = dataclass,
                    propertyType = emptyMap(),
                    observableTypes = emptyMap(),
                    dtoTypes = emptyMap()
            )


            val dc = dataclass.get()
            context!!

            val dtoTypes = context
                    .getSpec(DataClassSpec::class)
                    .groupBy { o -> o.type }
                    .entries
                    .map { Pair(it.key, it.value[0].dtoType) }
                    .toMap()

            val observableTypes = context
                    .getSpec(ObservableSpec::class)
                    .groupBy { o -> o.of }
                    .entries
                    .map { Pair(it.key, it.value[0].type) }
                    .toMap(HashMap())


            val propertyType = dc.propertyType
                    .entries
                    .map { entry ->
                        val type = entry.value
                        if (type is ParameterizedTypeName) {
                            Pair(entry.key, ParameterizedTypeName.get(
                                    OBSERVABLE_COLLECTIONS_MAP.getOrDefault(type.rawType, type.rawType),
                                    *type.typeArguments.map { observableTypes.getOrDefault(it, it) }.toTypedArray())
                            )
                        } else {
                            Pair(entry.key, (observableTypes[entry.value] ?: dtoTypes[entry.value]
                            ?: dc.propertyDtoType[entry.key])!!)
                        }
                    }
                    .toMap()


            return ObservableSpec(
                    type = ClassName.get(rawSpec.packageName, rawSpec.name).nestedClass("Observable"),
                    of = ClassName.get(rawSpec.packageName, rawSpec.name),
                    extends = asType(rawSpec.observable.extends),
                    implements = rawSpec.observable.implements.map { asType(it) },
                    dataclass = dataclass,
                    propertyType = propertyType,
                    observableTypes = observableTypes,
                    dtoTypes = dtoTypes
            )
        }
    }
}
