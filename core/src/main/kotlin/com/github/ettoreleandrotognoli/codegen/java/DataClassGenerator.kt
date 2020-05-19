package com.github.ettoreleandrotognoli.codegen.java

import com.github.ettoreleandrotognoli.codegen.api.Context
import com.github.ettoreleandrotognoli.codegen.core.AbstractCodeGenerator
import com.github.ettoreleandrotognoli.codegen.data.DataClassSpec
import com.github.ettoreleandrotognoli.codegen.data.ObservableSpec
import com.github.ettoreleandrotognoli.codegen.upperFirst
import com.squareup.javapoet.*
import org.springframework.stereotype.Component
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.lang.model.element.Modifier
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

val OBSERVABLE_COLLECTIONS = ClassName.get("org.jdesktop.observablecollections", "ObservableCollections")

val LIST_TYPE = ClassName.get(List::class.java)
val SET_TYPE = ClassName.get(Set::class.java)
val MAP_TYPE = ClassName.get(Map::class.java)
val COLLECTION_TYPE = ClassName.get(Collection::class.java)

val GENERIC_COLLECTIONS_TYPES = setOf(
        LIST_TYPE,
        SET_TYPE,
        MAP_TYPE,
        COLLECTION_TYPE
)

val OBSERVABLE_COLLECTIONS_MAP = mapOf(
        LIST_TYPE to ClassName.get("org.jdesktop.observablecollections", "ObservableList"),
        MAP_TYPE to ClassName.get("org.jdesktop.observablecollections", "ObservableMap")
)

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
            val observableProperties: List<String> = emptyList(),
            val collectionProperties: List<String>,
            val propertyDtoType: Map<String, TypeName> = emptyMap(),
            val propertyObservableType: Map<String, TypeName> = emptyMap(),
            val resolvedDtoTypes: Map<TypeName, TypeName> = emptyMap(),
            val resolvedObservableTypes: Map<TypeName, TypeName> = emptyMap()
    ) {
        companion object {


            fun from(rawSpec: DataClassSpec, context: Context? = null): ProcessedDataClassSpec {
                val propertyType = rawSpec.properties
                        .map { Pair(it.name, asType(it.type)) }
                        .toMap()

                val collectionProperties = propertyType
                        .entries
                        .filter { it.value is ParameterizedTypeName && GENERIC_COLLECTIONS_TYPES.contains((it.value as ParameterizedTypeName).rawType) }
                        .map { it.key }

                val resolvedDtoTypes = (context?.getReference(ProcessedDataClassSpec::class) ?: emptyList())
                        .groupBy { p -> p.type }
                        .entries
                        .map { Pair(it.key, it.value[0].dtoType) }
                        .toMap(HashMap())

                val resolvedObservableTypes = (context?.getReference(ProcessedDataClassSpec::class) ?: emptyList())
                        .groupBy { p -> p.type }
                        .entries
                        .filter { it.value[0].observable != null }
                        .map { Pair(it.key, it.value[0].observableType) }
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

                val observableProperties = propertyType
                        .entries
                        .filter { resolvedObservableTypes.contains(it.value) }
                        .map { it.key }

                val propertyObservableType = propertyType
                        .entries
                        .map { entry ->
                            val type = entry.value
                            if (type is ParameterizedTypeName) {
                                Pair(entry.key, ParameterizedTypeName.get(OBSERVABLE_COLLECTIONS_MAP.getOrDefault(type.rawType, type.rawType), *type.typeArguments.map { resolvedObservableTypes.getOrDefault(it, it) }.toTypedArray()))
                            } else {
                                Pair(entry.key, (resolvedObservableTypes[entry.value] ?: propertyDtoType[entry.key])!!)
                            }
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
                        observableProperties = observableProperties,
                        collectionProperties = collectionProperties,
                        propertyDtoType = propertyDtoType,
                        propertyObservableType = propertyObservableType,
                        resolvedDtoTypes = resolvedDtoTypes,
                        resolvedObservableTypes = resolvedObservableTypes

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
                                    if (isPrimitive) {
                                        method.addStatement(
                                                "this.$1L = $2L.$3L()",
                                                p,
                                                "source",
                                                propertyGetMethodName[p]
                                        )
                                        return@forEach

                                    }
                                    if (!collectionProperties.contains(p)) {
                                        method.addStatement(
                                                "this.$1L = $3L.$4L() == null ? null : new $2T($3L.$4L())",
                                                p,
                                                propertyDtoType[p],
                                                "source",
                                                propertyGetMethodName[p]
                                        )
                                        return@forEach
                                    }
                                    val itemType = (propertyType[p] as ParameterizedTypeName).typeArguments[0]
                                    method.addStatement(
                                            "this.$1L = $3L.$4L() == null ? null : $3L.$4L().stream().map( it -> new $2T(it) ).collect($5T.$6L())",
                                            p,
                                            resolvedDtoTypes.getOrDefault(itemType, itemType),
                                            "source",
                                            propertyGetMethodName[p],
                                            Collectors::class.java,
                                            if ((propertyType[p] as ParameterizedTypeName).rawType == SET_TYPE) "toSet" else "toList"
                                    )
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
                                    method.addStatement("this.$1L($2L.$3L())", codeSpec.propertySetMethodName[it], "source", codeSpec.propertyGetMethodName[it]!!)
                                    return@forEach
                                }
                                if (!codeSpec.collectionProperties.contains(it)) {
                                    method.addStatement("this.$1L($3L.$4L() == null ? null : new $2T($3L.$4L()))", codeSpec.propertySetMethodName[it], codeSpec.propertyObservableType[it]!!, "source", codeSpec.propertyGetMethodName[it]!!)
                                    return@forEach
                                }
                                val itemType = (codeSpec.propertyType[it] as ParameterizedTypeName).typeArguments[0]
                                val resolvedDto = codeSpec.resolvedDtoTypes.getOrDefault(itemType, itemType)
                                method.addStatement(
                                        "this.$1L($3L.$4L() == null ? null : $2T.$5L($3L.$4L().stream().map( it -> new $8T(it) ).collect($6T.$7L())))",
                                        codeSpec.propertySetMethodName[it],
                                        OBSERVABLE_COLLECTIONS,
                                        "source",
                                        codeSpec.propertyGetMethodName[it]!!,
                                        if ((codeSpec.propertyType[it] as ParameterizedTypeName).rawType == MAP_TYPE) "observableMap" else "observableList",
                                        Collectors::class.java,
                                        "toList",
                                        codeSpec.resolvedObservableTypes.getOrDefault(itemType, resolvedDto)
                                )
                            }
                        }
                        .build()
        )

        observableClassBuilder.superclass(observableSpec.extends)
        observableSpec.implements.forEach {
            observableClassBuilder.addSuperinterface(it)
        }

        observableClassBuilder.addField(
                FieldSpec.builder(PropertyChangeSupport::class.java, "propertyChangeSupport")
                        .addModifiers(Modifier.FINAL, Modifier.PRIVATE, Modifier.TRANSIENT)
                        .initializer("new PropertyChangeSupport(this)").build()
        )

        codeSpec.observableProperties.map {
            FieldSpec.builder(PropertyChangeListener::class.java, "${it}Listener")
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.TRANSIENT)
                    .initializer("$1L",
                            TypeSpec.anonymousClassBuilder("")
                                    .addSuperinterface(PropertyChangeListener::class.java)
                                    .addMethod(
                                            MethodSpec.methodBuilder("propertyChange")
                                                    .addModifiers(Modifier.PUBLIC)
                                                    .addAnnotation(Override::class.java)
                                                    .addParameter(PropertyChangeEvent::class.java, "event")
                                                    .addStatement("$1L.firePropertyChange( $2L + $3S + event.getPropertyName() , event.getOldValue() , event.getNewValue() )", "propertyChangeSupport", "PROP_${asConstName(it)}", ".")
                                                    .build()
                                    )
                                    .build()
                    )
                    .build()
        }.forEach { observableClassBuilder.addField(it) }

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
                .map { buildConcreteGetMethod(it, codeSpec.propertyType[it]!!).addStatement("return ($1T) ($2T) this.$3L", codeSpec.propertyType[it], Object::class.java, it) }
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
                    val old = "old${upperFirst(it)}"
                    val methodBuilder = buildConcreteSetMethod(it, codeSpec.propertyObservableType[it]!!)

                    methodBuilder.addStatement("\$T \$L = this.\$L", codeSpec.propertyObservableType[it]!!, old, it)
                            .addStatement("this.\$L = \$L", it, it)
                            .beginControlFlow("if(!\$T.equals(\$L,\$L))", Objects::class.java, old, it)

                    if (codeSpec.observableProperties.contains(it))
                        methodBuilder
                                .addStatement("if ($1L != null) $1L.removePropertyChangeListener(this.$2L)", old, "${it}Listener")
                                .addStatement("if ($1L != null) $1L.addPropertyChangeListener(this.$2L)", it, "${it}Listener")

                    methodBuilder
                            .addStatement("this.propertyChangeSupport.firePropertyChange(\$L,\$L,\$L)", "PROP_${asConstName(it)}", old, it)
                            .endControlFlow()
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