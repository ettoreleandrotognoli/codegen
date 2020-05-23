package com.github.ettoreleandrotognoli.codegen.generator.data

import com.github.ettoreleandrotognoli.codegen.api.BaseSpec
import com.github.ettoreleandrotognoli.codegen.api.BuildContext
import com.github.ettoreleandrotognoli.codegen.generator.asType
import com.github.ettoreleandrotognoli.codegen.generator.fullName
import com.github.ettoreleandrotognoli.codegen.isBooleanType
import com.github.ettoreleandrotognoli.codegen.upperFirst
import com.squareup.javapoet.*
import java.util.*
import java.util.stream.Stream
import javax.lang.model.element.Modifier
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class DataClassSpec(
        val rawSpec: DataClassRawSpec,
        type: ClassName,
        val extends: TypeName,
        val implements: List<TypeName>,
        val dtoType: ClassName,
        val mutableType: ClassName,
        val builderType: ClassName,
        val properties: List<String>,
        val propertyType: Map<String, TypeName>,
        val propertySetMethodName: Map<String, String>,
        val propertyGetMethodName: Map<String, String>,
        val collectionProperties: List<String>,
        val propertyDtoType: Map<String, TypeName> = emptyMap(),
        val resolvedDtoTypes: Map<TypeName, TypeName> = emptyMap()
) : BaseSpec(type) {
    companion object {


        fun from(rawSpec: DataClassRawSpec, context: BuildContext? = null): DataClassSpec {
            val propertyType = rawSpec.properties
                    .map { Pair(it.name, asType(it.type)) }
                    .toMap()

            val collectionProperties = propertyType
                    .entries
                    .filter { it.value is ParameterizedTypeName && GENERIC_COLLECTIONS_TYPES.contains((it.value as ParameterizedTypeName).rawType) }
                    .map { it.key }

            val resolvedDtoTypes: MutableMap<TypeName, TypeName> = (context?.getSpec(DataClassSpec::class)
                    ?: emptyList())
                    .groupBy { p -> p.type }
                    .entries
                    .map { Pair(it.key, it.value[0].dtoType) }
                    .toMap(HashMap())

            resolvedDtoTypes[COLLECTION_TYPE] = ClassName.get(LinkedList::class.java)
            resolvedDtoTypes[SET_TYPE] = ClassName.get(HashSet::class.java)
            resolvedDtoTypes[LIST_TYPE] = ClassName.get(ArrayList::class.java)
            resolvedDtoTypes[MAP_TYPE] = ClassName.get(HashMap::class.java)


            val propertyDtoType = propertyType
                    .entries
                    .map { entry ->
                        val type = entry.value
                        if (type is ParameterizedTypeName) {
                            Pair(entry.key, (resolvedDtoTypes[type.rawType] ?: propertyType[entry.key])!!)
                        } else {
                            Pair(entry.key, (resolvedDtoTypes[entry.value] ?: propertyType[entry.key])!!)
                        }
                    }
                    .toMap()


            return DataClassSpec(
                    rawSpec = rawSpec,
                    type = ClassName.get(rawSpec.packageName, rawSpec.name),
                    dtoType = ClassName.get(rawSpec.packageName, rawSpec.name).nestedClass("DTO"),
                    mutableType = ClassName.get(rawSpec.packageName, rawSpec.name).nestedClass("Mutable"),
                    builderType = ClassName.get(rawSpec.packageName, rawSpec.name).nestedClass("Builder"),
                    extends = asType(rawSpec.extends),
                    implements = rawSpec.implements.map { asType(it) },
                    properties = rawSpec.properties.map { it.name },
                    propertyType = propertyType,
                    propertySetMethodName = rawSpec.properties
                            .map { Pair(it.name, "set${it.name.upperFirst()}") }
                            .toMap(),
                    propertyGetMethodName = rawSpec.properties
                            .map { Pair(it.name, (if (isBooleanType(it.type)) ("is${it.name.upperFirst()}") else ("get${it.name.upperFirst()}"))) }
                            .toMap(),
                    collectionProperties = collectionProperties,
                    propertyDtoType = propertyDtoType,
                    resolvedDtoTypes = resolvedDtoTypes

            )
        }
    }


    fun abstractGetMethods(): Stream<MethodSpec.Builder> {
        return properties.stream().map {
            MethodSpec.methodBuilder(propertyGetMethodName[it])
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(propertyType[it])
        }
    }

    fun abstractSetMethod(): Stream<MethodSpec.Builder> {
        return properties.stream().map {
            MethodSpec.methodBuilder(propertySetMethodName[it])
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(propertyType[it], it)
        }
    }

    fun concreteGetMethods(): Stream<MethodSpec.Builder> {
        return properties.stream().map {
            MethodSpec.methodBuilder(propertyGetMethodName[it])
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override::class.java)
                    .returns(propertyType[it])
                    .addCode("return this.$1L;\n", it)

        }
    }

    fun concreteSetMethods(): Stream<MethodSpec.Builder> {
        return properties.stream().map {
            MethodSpec.methodBuilder(propertySetMethodName[it])
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(propertyType[it], it)
                    .addCode("this.$1L = $1L;\n", it)
        }
    }

    fun builderSetMethods(): Stream<MethodSpec.Builder> {
        return properties.stream().map {
            MethodSpec.methodBuilder(propertySetMethodName[it])
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(propertyType[it], it)
                    .returns(builderType)
                    .addCode("this.$1L.$2L($3L);\n", "prototype", propertySetMethodName[it], it)
                    .addCode("return this;\n")
        }
    }

    fun fields(): Stream<FieldSpec.Builder> {
        return properties.stream().map {
            FieldSpec.builder(propertyType[it], it)
                    .addModifiers(Modifier.PRIVATE)
        }
    }


    fun buildMethod(): MethodSpec.Builder {
        return MethodSpec
                .methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(dtoType.fullName())
                .addCode("return this.\$N.clone();", "prototype")
    }

}
