package com.github.ettoreleandrotognoli.codegen.java

import com.github.ettoreleandrotognoli.codegen.api.Context
import com.github.ettoreleandrotognoli.codegen.core.AbstractCodeGenerator
import com.github.ettoreleandrotognoli.codegen.data.DataClassSpec
import com.github.ettoreleandrotognoli.codegen.data.ObservableSpec
import com.github.ettoreleandrotognoli.codegen.upperFirst
import com.squareup.javapoet.*
import org.springframework.stereotype.Component
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Stream
import javax.lang.model.element.Modifier

@Component
class DataClassGenerator : AbstractCodeGenerator<DataClassSpec>(DataClassSpec::class) {


    class ProcessedDataClassSpec(
            val rawSpec: DataClassSpec,
            val type: TypeName,
            val extends: TypeName,
            val implements: List<TypeName>,
            val dtoType: ClassName,
            val mutableType: ClassName,
            val builderType: ClassName,
            val properties: List<String>,
            val propertyType: Map<String, TypeName>,
            val propertySetMethodName: Map<String, String>,
            val propertyGetMethodName: Map<String, String>

    ) {
        companion object {

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
                val primitiveMap = listOf(
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
                        .map { it.toString() to it }.toMap()
                return primitiveMap.getOrElse(rawType, { ClassName.bestGuess(rawType) })
            }

            fun isBooleanType(rawType: String): Boolean {
                return listOf("Boolean", "boolean").contains(rawType)
            }

            fun from(rawSpec: DataClassSpec): ProcessedDataClassSpec {

                return ProcessedDataClassSpec(
                        rawSpec = rawSpec,
                        type = ClassName.get(rawSpec.packageName, rawSpec.name),
                        dtoType = ClassName.get(rawSpec.packageName, rawSpec.name).nestedClass("DTO"),
                        mutableType = ClassName.get(rawSpec.packageName, rawSpec.name).nestedClass("Mutable"),
                        builderType = ClassName.get(rawSpec.packageName, rawSpec.name).nestedClass("Builder"),
                        extends = asType(rawSpec.extends),
                        implements = rawSpec.implements.map { asType(it) },
                        properties = rawSpec.properties.map { it.name },
                        propertyType = rawSpec.properties
                                .map { Pair(it.name, asType(it.type)) }
                                .toMap(),
                        propertySetMethodName = rawSpec.properties
                                .map { Pair(it.name, "set${it.name.upperFirst()}") }
                                .toMap(),
                        propertyGetMethodName = rawSpec.properties
                                .map { Pair(it.name, (if (isBooleanType(it.type)) ("is${it.name.upperFirst()}") else ("get${it.name.upperFirst()}"))) }
                                .toMap()
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
                        .addAnnotation(Override::class.java)
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

        fun emptyConstructor(): MethodSpec.Builder {
            return MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
        }

        fun fullConstructorSignature(): MethodSpec.Builder {
            return MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .also {
                        properties.forEach { p ->
                            it.addParameter(propertyType[p], p)
                        }
                    }
        }

        fun fullConstructor(): MethodSpec.Builder {
            return fullConstructorSignature().also {
                properties.forEach { p ->
                    it.addCode("this.$1L = $1L", p)
                }
            }
        }

        fun copyConstructorSignature(): MethodSpec.Builder {
            return MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(type, "source")
        }

        fun copyConstructor(): MethodSpec.Builder {
            return copyConstructorSignature().also {
                properties.forEach { p ->
                    it.addCode("this.$1L = $2L.$3L();\n", p, "other", propertyGetMethodName[p])
                }
            }
        }

        fun constructors(): Stream<MethodSpec.Builder> {
            return Stream.of(
                    emptyConstructor(),
                    fullConstructor(),
                    copyConstructor()
            )
        }

        fun copyMethod(): MethodSpec.Builder {
            return MethodSpec.methodBuilder("copy")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(type, "source")
                    .returns(type)
                    .also {
                        properties.forEach { p ->
                            it.addCode("this.$1L = $2L.$3L();\n", p, "source", propertyGetMethodName[p])
                        }
                        it.addCode("return this;\n")
                    }
        }

        fun cloneMethod(): MethodSpec.Builder {
            return MethodSpec.methodBuilder("clone")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(type)
                    .also {
                        it.addCode("return new $1T(this);\n", dtoType)
                    }
        }

    }


    val camelCaseRegex = Pattern.compile("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")

    fun asType(className: String): ClassName {
        return ClassName.bestGuess(className)
    }

    fun upperFirst(string: String): String {
        return string.upperFirst()
    }

    fun asGetMethodName(propertyName: String): String {
        return "get${upperFirst(propertyName)}"
    }

    fun asConstName(string: String): String {
        return camelCaseRegex.split(string)
                .map { it.toUpperCase() }
                .joinToString(separator = "_")
    }

    fun buildConcreteSetMethod(propertyName: String, type: TypeName): MethodSpec.Builder {
        val parameter = ParameterSpec.builder(type, propertyName).build()
        return MethodSpec
                .methodBuilder("set${upperFirst(propertyName)}")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .addParameter(parameter)
    }


    fun buildConcreteGetMethod(propertyName: String, type: TypeName): MethodSpec.Builder {
        return MethodSpec
                .methodBuilder("get${upperFirst(propertyName)}")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .returns(type)
    }


    fun makeBuildMethod(returnType: TypeName): MethodSpec {
        val methodSpecBuilder = MethodSpec
                .methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addCode("return this.\$N.clone();", "prototype")
        return methodSpecBuilder.build()
    }

    fun makeToString(codeSpec: DataClassSpec, pattern: String): MethodSpec {
        val methodSpecBuilder = MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .returns(String::class.java)
        methodSpecBuilder.addCode(
                "return String.format(\$S, ${codeSpec.properties.joinToString(separator = ", ") { it.name }} );\n",
                codeSpec.properties.fold(pattern, { s, p -> s.replace("\$${p.name}", "%s") })
        );
        return methodSpecBuilder.build();
    }


    fun observableExtension(codeSpec: DataClassSpec, observableSpec: ObservableSpec, mainInterfaceBuilder: TypeSpec.Builder) {
        val mutable = ClassName.get(codeSpec.packageName, codeSpec.name + ".Mutable")
        val observableClassBuilder = TypeSpec.classBuilder("Observable")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(mutable)
                .addField(FieldSpec.builder(mutable, "origin").addModifiers(Modifier.PRIVATE, Modifier.FINAL).build())
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).addParameter(mutable, "origin").addCode("this.$1N = $1N;", "origin").build())

        observableClassBuilder.superclass(asType(observableSpec.extends))
        observableSpec.implements.forEach {
            observableClassBuilder.addSuperinterface(asType(it))
        }

        observableClassBuilder.addField(FieldSpec.builder(PropertyChangeSupport::class.java, "propertyChangeSupport").addModifiers(Modifier.FINAL, Modifier.PRIVATE, Modifier.TRANSIENT).initializer("new PropertyChangeSupport(this)").build())

        observableClassBuilder.addMethod(MethodSpec.methodBuilder("addPropertyChangeListener")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(PropertyChangeListener::class.java, "listener")
                .addCode("this.\$L.\$L(\$L);", "propertyChangeSupport", "addPropertyChangeListener", "listener")
                .build())
        observableClassBuilder.addMethod(MethodSpec.methodBuilder("addPropertyChangeListener")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(String::class.java, "propertyName")
                .addParameter(PropertyChangeListener::class.java, "listener")
                .addCode("this.\$L.\$L(\$L, \$L);", "propertyChangeSupport", "addPropertyChangeListener", "propertyName", "listener")
                .build())
        observableClassBuilder.addMethod(MethodSpec.methodBuilder("removePropertyChangeListener")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(PropertyChangeListener::class.java, "listener")
                .addCode("this.\$L.\$L(\$L);", "propertyChangeSupport", "removePropertyChangeListener", "listener")
                .build())
        observableClassBuilder.addMethod(MethodSpec.methodBuilder("removePropertyChangeListener")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(String::class.java, "propertyName")
                .addParameter(PropertyChangeListener::class.java, "listener")
                .addCode("this.\$L.\$L(\$L, \$L);", "propertyChangeSupport", "removePropertyChangeListener", "propertyName", "listener")
                .build())

        codeSpec.properties
                .map { it.name }
                .map { FieldSpec.builder(String::class.java, "PROP_${asConstName(it)}").addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer("\$S", it) }
                .forEach { observableClassBuilder.addField(it.build()) }

        codeSpec.properties
                .map { buildConcreteGetMethod(it.name, asType(it.type)).addCode("return this.$1L.$2L();", "origin", "get${upperFirst(it.name)}") }
                .forEach { observableClassBuilder.addMethod(it.build()) }

        codeSpec.properties
                .map {
                    buildConcreteSetMethod(it.name, asType(it.type))
                            .addCode("\$T \$L = this.\$L.\$L();\n", asType(it.type), "old${upperFirst(it.name)}", "origin", "get${upperFirst(it.name)}")
                            .addCode("this.\$L.\$L(\$L);\n", "origin", "set${upperFirst(it.name)}", it.name)
                            .addCode("if(!\$T.equals(\$L,\$L)) this.propertyChangeSupport.firePropertyChange(\$L,\$L,\$L);\n", Objects::class.java, "old${upperFirst(it.name)}", it.name, "PROP_${asConstName(it.name)}", "old${upperFirst(it.name)}", it.name)
                }
                .forEach { observableClassBuilder.addMethod(it.build()) }


        observableClassBuilder.addMethod(MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .returns(String::class.java)
                .addCode("return String.format(\$S,\$L.\$L());\n", "Observable( %s )", "origin", "toString")
                .build())

        mainInterfaceBuilder.addType(observableClassBuilder.build())
    }


    override fun generate(context: Context, codeSpec: DataClassSpec) {
        val spec = ProcessedDataClassSpec.from(codeSpec)

        val mutableInterfaceBuilder = TypeSpec.interfaceBuilder(spec.mutableType)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(spec.type);

        spec.abstractSetMethod().forEach {
            mutableInterfaceBuilder.addMethod(it.build())
        }

        val dtoClassBuilder = TypeSpec.classBuilder(spec.dtoType)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(spec.mutableType)

        spec.fields().forEach {
            dtoClassBuilder.addField(it.build())
        }

        spec.constructors().forEach {
            dtoClassBuilder.addMethod(it.build())
        }

        spec.concreteGetMethods().forEach {
            dtoClassBuilder.addMethod(it.build())
        }

        spec.concreteSetMethods().forEach {
            dtoClassBuilder.addMethod(it.build())
        }

        dtoClassBuilder.addMethod(spec.copyMethod().build())

        dtoClassBuilder.addMethod(spec.cloneMethod().build())

        dtoClassBuilder.superclass(spec.extends)

        val builderClassBuilder = TypeSpec.classBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addField(FieldSpec.builder(spec.dtoType, "prototype", Modifier.PRIVATE, Modifier.FINAL).initializer("new \$T()", spec.dtoType).build())
                .addMethod(makeBuildMethod(spec.dtoType))

        spec.builderSetMethods().forEach {
            builderClassBuilder.addMethod(it.build())
        }

        if (codeSpec.toString.enable) {
            val toStringPattern = codeSpec.toString.pattern
                    ?: codeSpec.properties.joinToString(prefix = "${codeSpec.name} {", separator = ", ", postfix = "}") { "${it.name}=\$${it.name}" }
            dtoClassBuilder.addMethod(makeToString(codeSpec, toStringPattern))
        }

        if (codeSpec.hashCode.enable) {
            val fields = codeSpec.hashCode.fields ?: codeSpec.properties.map { it.name }
            dtoClassBuilder.addMethod(makeHashCode(codeSpec, fields))
        }

        if (codeSpec.equals.enable) {
            val fields = codeSpec.equals.fields ?: codeSpec.properties.map { it.name }
            dtoClassBuilder.addMethod(makeEquals(codeSpec, fields))
        }

        val mutableInterface = mutableInterfaceBuilder.build()
        val dtoClass = dtoClassBuilder.build()
        val builderClass = builderClassBuilder.build()

        val mainInterfaceBuilder =
                TypeSpec.interfaceBuilder(codeSpec.name)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addType(mutableInterface)
                        .addType(dtoClass)
                        .addType(builderClass)

        codeSpec.implements.forEach {
            mainInterfaceBuilder.addSuperinterface(asType(it))
        }

        spec.abstractGetMethods().forEach {
            mainInterfaceBuilder.addMethod(it.build())
        }

        if (codeSpec.observable != null) {
            observableExtension(codeSpec, codeSpec.observable, mainInterfaceBuilder)
        }

        val javaFile = JavaFile.builder(codeSpec.packageName, mainInterfaceBuilder.build())
                .build()
        javaFile.writeTo(context.project.generatedSourcePath)
    }

    private fun makeHashCode(codeSpec: DataClassSpec, fields: List<String>): MethodSpec {
        val methodSpecBuilder = MethodSpec.methodBuilder("hashCode")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .returns(TypeName.INT)
        methodSpecBuilder.addCode("return Objects.hash(${fields.joinToString(separator = ", ")});\n");
        return methodSpecBuilder.build()
    }

    private fun makeEquals(codeSpec: DataClassSpec, fields: List<String>): MethodSpec {
        val myType = ClassName.get(codeSpec.packageName, codeSpec.name)
        val methodSpecBuilder = MethodSpec.methodBuilder("equals")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .addParameter(Object::class.java, "obj")
                .returns(TypeName.BOOLEAN)
        methodSpecBuilder.addCode("if (this == obj) return true;\n");
        methodSpecBuilder.addCode("if (obj == null) return false;\n");
        methodSpecBuilder.addCode("if (!(obj instanceof \$T)) return false;\n", myType)
        methodSpecBuilder.addCode("$1T other = ($1T) obj;\n", myType)
        fields.forEach {
            methodSpecBuilder.addCode("if(!Objects.equals(\$L, \$L.\$L())) return false;\n", it, "other", asGetMethodName(it))
        }
        methodSpecBuilder.addCode("return true;\n")
        return methodSpecBuilder.build()
    }
}