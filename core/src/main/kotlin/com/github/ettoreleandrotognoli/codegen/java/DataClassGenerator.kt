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
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet


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

/**
 * workaround to avoid conflict names when there are inheritance between dataclasses
 */
fun ClassName.fullName(): ClassName {
    return ClassName.get(this.packageName(), this.simpleNames().joinToString(separator = "."))
}

@Component
class DataClassGenerator : AbstractCodeGenerator<DataClassSpec>(DataClassSpec::class) {

    class ProcessedObservableSpec(
            val extends: TypeName,
            val implements: List<TypeName>

    ) {
        companion object {
            fun from(rawSpec: ObservableSpec?): ProcessedObservableSpec? {
                if (rawSpec == null) return null
                return ProcessedObservableSpec(
                        extends = asType(rawSpec.extends),
                        implements = rawSpec.implements.map { asType(it) }
                )
            }
        }
    }

    class ProcessedDataClassSpec(
            val rawSpec: DataClassSpec,
            val type: TypeName,
            val extends: TypeName,
            val implements: List<TypeName>,
            val dtoType: ClassName,
            val mutableType: ClassName,
            val builderType: ClassName,
            val observableType: ClassName,
            val properties: List<String>,
            val propertyType: Map<String, TypeName>,
            val propertySetMethodName: Map<String, String>,
            val propertyGetMethodName: Map<String, String>,
            val observable: ProcessedObservableSpec?,
            val propertyDtoType: Map<String, TypeName> = emptyMap(),
            val propertyObservableType: Map<String, TypeName> = emptyMap()

    ) {
        companion object {


            fun from(rawSpec: DataClassSpec, context: Context? = null): ProcessedDataClassSpec {
                val propertyType = rawSpec.properties
                        .map { Pair(it.name, asType(it.type)) }
                        .toMap()

                val dtoType = (context?.getReference(ProcessedDataClassSpec::class) ?: emptyList())
                        .groupBy { p -> p.type }
                        .entries
                        .map { Pair(it.key, it.value[0].dtoType) }
                        .toMap(HashMap())

                val observableType = (context?.getReference(ProcessedDataClassSpec::class) ?: emptyList())
                        .groupBy { p -> p.type }
                        .entries
                        .filter { it.value[0].observable != null }
                        .map { Pair(it.key, it.value[0].observableType) }
                        .toMap(HashMap())

                dtoType[TypeName.get(Collections::class.java)] = ClassName.get(LinkedList::class.java)
                dtoType[TypeName.get(Set::class.java)] = ClassName.get(HashSet::class.java)
                dtoType[TypeName.get(List::class.java)] = ClassName.get(ArrayList::class.java)
                dtoType[TypeName.get(Map::class.java)] = ClassName.get(HashMap::class.java)

                val propertyDtoType = propertyType
                        .entries
                        .map { entry ->
                            val type = entry.value
                            if (type is ParameterizedTypeName) {
                                Pair(entry.key, (dtoType[type.rawType] ?: propertyType[entry.key])!!)
                            } else {
                                Pair(entry.key, (dtoType[entry.value] ?: propertyType[entry.key])!!)
                            }
                        }
                        .toMap()

                val propertyObservableType = propertyType
                        .entries
                        .map { entry ->
                            Pair(entry.key, (observableType[entry.value] ?: propertyDtoType[entry.key])!!)
                        }
                        .toMap()

                return ProcessedDataClassSpec(
                        rawSpec = rawSpec,
                        type = ClassName.get(rawSpec.packageName, rawSpec.name),
                        dtoType = ClassName.get(rawSpec.packageName, rawSpec.name).nestedClass("DTO"),
                        mutableType = ClassName.get(rawSpec.packageName, rawSpec.name).nestedClass("Mutable"),
                        builderType = ClassName.get(rawSpec.packageName, rawSpec.name).nestedClass("Builder"),
                        observableType = ClassName.get(rawSpec.packageName, rawSpec.name).nestedClass("Observable"),
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
                        observable = ProcessedObservableSpec.from(rawSpec.observable),
                        propertyDtoType = propertyDtoType,
                        propertyObservableType = propertyObservableType
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
                    it.addCode("this.$1L = $1L;\n", p)
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
                it.addStatement("this.$1L($2L)", "copy", "source");
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
            val copyMethod = "${rawSpec.copy.default}Copy"
            return MethodSpec.methodBuilder("copy")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(type, "source")
                    .returns(type)
                    .also {
                        it.addStatement("return this.$1L($2L)", copyMethod, "source")
                    }
        }

        fun shallowCopyMethod(): MethodSpec.Builder {
            return MethodSpec.methodBuilder("shallowCopy")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(type, "source")
                    .returns(dtoType.fullName())
                    .also {
                        properties.forEach { p ->
                            it.addCode("this.$1L = $2L.$3L();\n", p, "source", propertyGetMethodName[p])
                        }
                        it.addCode("return this;\n")
                    }
        }

        fun deepCopyMethod(): MethodSpec.Builder {
            return MethodSpec.methodBuilder("deepCopy")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(type, "source")
                    .returns(dtoType.fullName())
                    .also { method ->
                        properties
                                .forEach { p ->
                                    val isPrimitive = propertyType[p]?.isPrimitive!!
                                    if (!isPrimitive) {
                                        method.addCode(
                                                "this.$1L = $3L.$4L() == null ? null : new $2T($3L.$4L());\n",
                                                p,
                                                propertyDtoType[p],
                                                "source",
                                                propertyGetMethodName[p]
                                        )

                                    } else {
                                        method.addCode(
                                                "this.$1L = $2L.$3L();\n",
                                                p,
                                                "source",
                                                propertyGetMethodName[p]
                                        )
                                    }
                                }
                        method.addCode("return this;\n")
                    }
        }

        fun cloneMethod(): MethodSpec.Builder {
            return MethodSpec.methodBuilder("clone")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(dtoType.fullName())
                    .also {
                        it.addCode("return new $1T(this);\n", dtoType.fullName())
                    }
        }

        fun shallowCloneMethod(): MethodSpec.Builder {
            return MethodSpec.methodBuilder("shallowClone")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(dtoType.fullName())
                    .also {
                        it.addCode("return new $1T().shallowCopy(this);\n", dtoType.fullName())
                    }
        }

        fun deepCloneMethod(): MethodSpec.Builder {
            return MethodSpec.methodBuilder("deepClone")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(dtoType.fullName())
                    .also {
                        it.addCode("return new $1T().deepCopy(this);\n", dtoType.fullName())
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

    val camelCaseRegex = Pattern.compile("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")

    fun upperFirst(string: String): String {
        return string.upperFirst()
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
                .addParameter(parameter)
    }


    fun buildConcreteGetMethod(propertyName: String, type: TypeName): MethodSpec.Builder {
        return MethodSpec
                .methodBuilder("get${upperFirst(propertyName)}")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .returns(type)
    }


    fun makeToString(codeSpec: DataClassSpec): MethodSpec {
        val pattern = codeSpec.toString.pattern
                ?: codeSpec.properties.joinToString(prefix = "${codeSpec.name} {", separator = ", ", postfix = "}") { "${it.name}=\$${it.name}" }
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


    fun observableExtension(codeSpec: ProcessedDataClassSpec, observableSpec: ProcessedObservableSpec, mainInterfaceBuilder: TypeSpec.Builder) {
        val observableClassBuilder = TypeSpec.classBuilder("Observable")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(codeSpec.type)

        observableClassBuilder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build())

        observableClassBuilder.addMethod(
                MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                        .addParameter(codeSpec.type, "source")
                        .also { method ->
                            codeSpec.properties.forEach {
                                if (codeSpec.propertyObservableType[it]!!.isPrimitive) {
                                    method.addStatement("this.$1L = $2L.$3L()", it, "source", codeSpec.propertyGetMethodName[it]!!)
                                } else {
                                    method.addStatement("this.$1L = $3L.$4L() == null ? null : new $2T($3L.$4L())", it, codeSpec.propertyObservableType[it]!!, "source", codeSpec.propertyGetMethodName[it]!!)
                                }
                            }
                        }
                        .build()
        )

        observableClassBuilder.superclass(observableSpec.extends)
        observableSpec.implements.forEach {
            observableClassBuilder.addSuperinterface(it)
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
                .map { FieldSpec.builder(String::class.java, "PROP_${asConstName(it)}").addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer("\$S", it) }
                .forEach { observableClassBuilder.addField(it.build()) }

        codeSpec.properties
                .map { buildConcreteGetMethod(it, codeSpec.propertyObservableType[it]!!).addStatement("return this.$1L", it) }
                .forEach { observableClassBuilder.addMethod(it.build()) }

        codeSpec.properties
                .map {
                    FieldSpec
                            .builder(codeSpec.propertyObservableType[it]!!, it)
                            .addModifiers(Modifier.PRIVATE)
                            .build()
                }
                .forEach { observableClassBuilder.addField(it) }

        codeSpec.properties
                .map {
                    buildConcreteSetMethod(it, codeSpec.propertyObservableType[it]!!)
                            .addStatement("\$T \$L = this.\$L", codeSpec.propertyObservableType[it]!!, "old${upperFirst(it)}", it)
                            .addStatement("this.\$L = \$L", it, it)
                            .addCode("if(!\$T.equals(\$L,\$L)) this.propertyChangeSupport.firePropertyChange(\$L,\$L,\$L);\n", Objects::class.java, "old${upperFirst(it)}", it, "PROP_${asConstName(it)}", "old${upperFirst(it)}", it)
                }
                .forEach { observableClassBuilder.addMethod(it.build()) }

        observableClassBuilder.addMethod(makeToString(codeSpec.rawSpec))


        mainInterfaceBuilder.addType(observableClassBuilder.build())
    }

    override fun preProcess(context: Context, codeSpec: DataClassSpec): List<Any> {
        return listOf(ProcessedDataClassSpec.from(codeSpec))
    }

    override fun generate(context: Context, codeSpec: DataClassSpec) {

        val spec = ProcessedDataClassSpec.from(codeSpec, context)

        val mutableInterfaceBuilder = TypeSpec.interfaceBuilder(spec.mutableType)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(spec.type);

        spec.abstractSetMethod().forEach {
            mutableInterfaceBuilder.addMethod(it.build())
        }

        val dtoClassBuilder = TypeSpec.classBuilder(spec.dtoType)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(spec.mutableType.fullName())

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
            dtoClassBuilder.addMethod(it.addAnnotation(Override::class.java).build())
        }

        dtoClassBuilder.addMethod(spec.copyMethod().build())

        dtoClassBuilder.addMethod(spec.cloneMethod().build())

        dtoClassBuilder.addMethod(spec.shallowCopyMethod().build())

        dtoClassBuilder.addMethod(spec.shallowCloneMethod().build())

        dtoClassBuilder.addMethod(spec.deepCopyMethod().build())

        dtoClassBuilder.addMethod(spec.deepCloneMethod().build())

        dtoClassBuilder.superclass(spec.extends)

        val builderClassBuilder = TypeSpec.classBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addField(FieldSpec.builder(spec.dtoType.fullName(), "prototype", Modifier.PRIVATE, Modifier.FINAL).initializer("new \$T()", spec.dtoType.fullName()).build())
                .addMethod(spec.buildMethod().build())



        spec.builderSetMethods().forEach {
            builderClassBuilder.addMethod(it.build())
        }

        if (codeSpec.toString.enable) {
            dtoClassBuilder.addMethod(makeToString(codeSpec))
        }

        if (codeSpec.hashCode.enable) {
            val fields = codeSpec.hashCode.fields ?: codeSpec.properties.map { it.name }
            dtoClassBuilder.addMethod(makeHashCode(codeSpec, fields))
        }

        if (codeSpec.equals.enable) {
            val fields = codeSpec.equals.fields ?: codeSpec.properties.map { it.name }
            dtoClassBuilder.addMethod(makeEquals(spec, fields))
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

        spec.implements.forEach {
            mainInterfaceBuilder.addSuperinterface(it)
        }

        spec.abstractGetMethods().forEach {
            mainInterfaceBuilder.addMethod(it.build())
        }

        if (spec.observable != null) {
            observableExtension(spec, spec.observable, mainInterfaceBuilder)
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
        methodSpecBuilder.addCode("return \$T.hash(${fields.joinToString(separator = ", ")});\n", Objects::class.java);
        return methodSpecBuilder.build()
    }

    private fun makeEquals(codeSpec: ProcessedDataClassSpec, fields: List<String>): MethodSpec {
        val methodSpecBuilder = MethodSpec.methodBuilder("equals")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .addParameter(Object::class.java, "obj")
                .returns(TypeName.BOOLEAN)
        methodSpecBuilder.addCode("if (this == obj) return true;\n");
        methodSpecBuilder.addCode("if (obj == null) return false;\n");
        methodSpecBuilder.addCode("if (!(obj instanceof \$T)) return false;\n", codeSpec.type)
        methodSpecBuilder.addCode("$1T other = ($1T) obj;\n", codeSpec.type)
        fields.forEach {
            methodSpecBuilder.addCode("if(!\$T.equals(\$L, \$L.\$L())) return false;\n", Objects::class.java, it, "other", codeSpec.propertyGetMethodName[it]!!)
        }
        methodSpecBuilder.addCode("return true;\n")
        return methodSpecBuilder.build()
    }
}