package com.github.ettoreleandrotognoli.codegen.generator

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName

fun asType(rawType: String): TypeName {
    if (rawType.contains("<")) {
        val baseRawType = rawType.substring(0, rawType.indexOf("<"))
        val baseType = ClassName.bestGuess(baseRawType)
        val rawParameterizedTypes = rawType.substring(rawType.indexOf("<") + 1, rawType.length - 1)
        val parameterizedTypes = rawParameterizedTypes
                .split(",")
                .map { it.trim() }
                .map { asType(it) }
                .toTypedArray()
        return ParameterizedTypeName.get(baseType, *parameterizedTypes)
    }
    val baseTypes = listOf(
            ClassName.BOOLEAN,
            ClassName.BYTE,
            ClassName.CHAR,
            ClassName.FLOAT,
            ClassName.DOUBLE,
            ClassName.INT,
            ClassName.VOID,
            ClassName.LONG,
            ClassName.SHORT
    )
            .map { it.toString() to it }.toMap().toMutableMap()
    baseTypes["Object"] = ClassName.OBJECT
    return baseTypes.getOrElse(rawType, { ClassName.bestGuess(rawType) })
}