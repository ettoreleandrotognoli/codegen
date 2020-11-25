package com.github.ettoreleandrotognoli.codegen

import com.github.ettoreleandrotognoli.codegen.generator.data.LIST_TYPE
import com.github.ettoreleandrotognoli.codegen.generator.data.MAP_TYPE
import com.github.ettoreleandrotognoli.codegen.generator.data.SET_TYPE
import com.github.ettoreleandrotognoli.codegen.generator.fullName
import com.squareup.javapoet.*
import java.io.InputStream
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.lang.model.element.Modifier

typealias SnakeYaml = org.yaml.snakeyaml.Yaml

fun InputStream.asString(): String {
    val stringBuilder = StringBuilder()
    val buffer = ByteArray(this.available())
    this.read(buffer)
    stringBuilder.append(String(buffer))
    return stringBuilder.toString()
}


fun isBooleanType(rawType: String): Boolean {
    return listOf("Boolean", "boolean").contains(rawType)
}


fun String.upperFirst(): String {
    return this[0].toUpperCase() + this.substring(1)
}

fun String.asGetMethod(type: TypeName): String {
    return "get${this.upperFirst()}"
}

fun String.asSetMethod(type: TypeName): String {
    return "set${this.upperFirst()}"
}


fun buildConcreteSetMethod(propertyName: String, type: TypeName): MethodSpec.Builder {
    val parameter = ParameterSpec.builder(type, propertyName).build()
    return MethodSpec
            .methodBuilder(propertyName.asSetMethod(type))
            .addModifiers(Modifier.PUBLIC)
            .addParameter(parameter)
}


fun buildConcreteGetMethod(propertyName: String, type: TypeName): MethodSpec.Builder {
    return MethodSpec
            .methodBuilder(propertyName.asGetMethod(type))
            .addModifiers(Modifier.PUBLIC)
            .returns(type)
}


fun makeToString(type: ClassName, properties: Map<String, TypeName>, pattern: String? = null): MethodSpec {
    val finalPattern = pattern ?: properties.map { it.key }
            .joinToString(prefix = "${type.fullName().simpleName()} {", separator = ", ", postfix = "}") { "${it}=\$${it}" }
    val methodSpecBuilder = MethodSpec.methodBuilder("toString")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .returns(String::class.java)
    methodSpecBuilder.addCode(
            "return String.format(\$S, ${properties.map { it.key }.joinToString(separator = ", ") { it }} );\n",
            properties.map { it.key }.fold(finalPattern, { s, p -> s.replace("\$$p", "%s") })
    );
    return methodSpecBuilder.build();
}


fun shallowCopyMethod(sourceType: TypeName, targetType: ClassName, properties: Map<String, TypeName>): MethodSpec.Builder {
    return MethodSpec.methodBuilder("shallowCopy")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(sourceType, "source")
            .returns(targetType.fullName())
            .also {
                properties.forEach { p ->
                    it.addCode("this.$1L = $2L.$3L();\n", p.key, "source", p.key.asGetMethod(p.value))
                }
                it.addCode("return this;\n")
            }
}


fun shallowCloneMethod(targetType: ClassName, concreteType: ClassName? = null): MethodSpec.Builder {
    val finalConcreteType = concreteType ?: targetType
    return MethodSpec.methodBuilder("shallowClone")
            .addModifiers(Modifier.PUBLIC)
            .returns(targetType.fullName())
            .also {
                it.addCode("return new $1T().shallowCopy(this);\n", finalConcreteType.fullName())
            }
}

fun emptyConstructor(): MethodSpec.Builder {
    return MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
}

fun fullConstructorSignature(properties: Map<String, TypeName>): MethodSpec.Builder {
    return MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .also {
                properties.forEach { p ->
                    it.addParameter(p.value, p.key)
                }
            }
}

fun fullConstructor(properties: Map<String, TypeName>): MethodSpec.Builder {
    return fullConstructorSignature(properties).also {
        properties
                .keys
                .forEach { p ->
                    it.addCode("this.$1L = $1L;\n", p)
                }
    }
}

fun copyConstructorSignature(sourceType: TypeName): MethodSpec.Builder {
    return MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(sourceType, "source")
}

fun copyConstructor(sourceType: TypeName): MethodSpec.Builder {
    return copyConstructorSignature(sourceType).also {
        it.addStatement("this.$1L($2L)", "copy", "source");
    }
}

fun constructors(sourceType: TypeName, properties: Map<String, TypeName>): Stream<MethodSpec.Builder> {
    return Stream.of(
            emptyConstructor(),
            fullConstructor(properties),
            copyConstructor(sourceType)
    )
}


fun copyMethod(sourceType: TypeName, targetType: ClassName, default: String = "deep"): MethodSpec.Builder {
    val copyMethod = "${default}Copy"
    return MethodSpec.methodBuilder("copy")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(sourceType, "source")
            .returns(targetType.fullName())
            .also {
                it.addStatement("return this.$1L($2L)", copyMethod, "source")
            }
}

fun cloneMethod(concreteTargetType: ClassName): MethodSpec.Builder {
    return MethodSpec.methodBuilder("clone")
            .addModifiers(Modifier.PUBLIC)
            .returns(concreteTargetType.fullName())
            .also {
                it.addCode("return new $1T(this);\n", concreteTargetType.fullName())
            }
}

fun deepCloneMethod(concreteTargetType: ClassName): MethodSpec.Builder {
    return MethodSpec.methodBuilder("deepClone")
            .addModifiers(Modifier.PUBLIC)
            .returns(concreteTargetType.fullName())
            .also {
                it.addCode("return new $1T().deepCopy(this);\n", concreteTargetType.fullName())
            }
}


fun deepCopyMethod(sourceType: TypeName, targetType: ClassName, properties: Map<String, TypeName>, concreteTypes: Map<TypeName, TypeName> = emptyMap()): MethodSpec.Builder {
    return MethodSpec.methodBuilder("deepCopy")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(sourceType, "source")
            .returns(targetType.fullName())
            .also { method ->
                properties
                        .forEach { p ->
                            val type = p.value
                            val doNotClone = type.isPrimitive || type == (TypeName.OBJECT)
                            if (doNotClone) {
                                method.addStatement(
                                        "this.$1L = $2L.$3L()",
                                        p.key,
                                        "source",
                                        p.key.asGetMethod(p.value)
                                )
                                return@forEach

                            }

                            if (type !is ParameterizedTypeName) {
                                method.addStatement(
                                        "this.$1L = $3L.$4L() == null ? null : new $2T($3L.$4L())",
                                        p.key,
                                        concreteTypes.getOrDefault(type, type),
                                        "source",
                                        p.key.asGetMethod(p.value)
                                )
                                return@forEach
                            }

                            if (type.rawType == SET_TYPE || type.rawType == LIST_TYPE) {
                                val itemType = type.typeArguments[0]
                                method.addStatement(
                                        "this.$1L = $3L.$4L() == null ? null : $3L.$4L().stream().map( it -> new $2T(it) ).collect($5T.$6L())",
                                        p.key,
                                        concreteTypes.getOrDefault(itemType, itemType),
                                        "source",
                                        p.key.asGetMethod(p.value),
                                        Collectors::class.java,
                                        if (type.rawType == SET_TYPE) "toSet" else "toList"
                                )
                            }
                            if (type.rawType == MAP_TYPE) {
                                val keyType = type.typeArguments[0]
                                val valueType = type.typeArguments[1]
                                method.addStatement(
                                        "this.$1L = $2L.$3L() == null ? null : $2L.$3L().entrySet().stream().collect($4T.$5L( it -> new $6T( it.getKey() ) , it -> new $7T( it.getValue() ) ))",
                                        p.key,
                                        "source",
                                        p.key.asGetMethod(p.value),
                                        Collectors::class.java,
                                        "toMap",
                                        concreteTypes.getOrDefault(keyType, keyType),
                                        concreteTypes.getOrDefault(valueType, valueType)
                                )
                            }
                        }
                method.addCode("return this;\n")
            }
}