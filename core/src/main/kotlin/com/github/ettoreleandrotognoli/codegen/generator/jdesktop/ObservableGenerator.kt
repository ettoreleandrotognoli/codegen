package com.github.ettoreleandrotognoli.codegen.generator.jdesktop

import com.github.ettoreleandrotognoli.codegen.api.BuildContext
import com.github.ettoreleandrotognoli.codegen.api.PreBuildContext
import com.github.ettoreleandrotognoli.codegen.buildConcreteGetMethod
import com.github.ettoreleandrotognoli.codegen.buildConcreteSetMethod
import com.github.ettoreleandrotognoli.codegen.core.AbstractCodeGenerator
import com.github.ettoreleandrotognoli.codegen.generator.asConstName
import com.github.ettoreleandrotognoli.codegen.generator.data.DataClassSpec
import com.github.ettoreleandrotognoli.codegen.generator.data.MAP_TYPE
import com.github.ettoreleandrotognoli.codegen.generator.data.OBSERVABLE_COLLECTIONS
import com.github.ettoreleandrotognoli.codegen.makeToString
import com.github.ettoreleandrotognoli.codegen.upperFirst
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import org.springframework.stereotype.Component
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.util.*
import java.util.stream.Collectors
import javax.lang.model.element.Modifier

@Component
class ObservableGenerator : AbstractCodeGenerator<ObservableRawSpec>(ObservableRawSpec::class) {


    fun process(spec: ObservableRawSpec, context: PreBuildContext? = null): ObservableSpec {
        val dataClassSpec = Optional.ofNullable(context)
                .map { it.getSpec(DataClassSpec::class) }
                .map { it.first { dc -> dc.type.packageName() == spec.packageName && dc.type.simpleName() == spec.name } }

        return ObservableSpec.from(spec, dataClassSpec, context)
    }

    override fun typedPrepareContext(context: PreBuildContext.Mutable, rawSpec: ObservableRawSpec) {
        val spec = process(rawSpec)
        context.registerPreSpec(spec)
        context.registerIntention(spec.type)
    }

    override fun typedGenerate(context: BuildContext.Mutable, rawSpec: ObservableRawSpec) {
        val spec = process(rawSpec, context)
        val dataclass = spec.dataclass.orElseThrow { Exception("A dataclass for ${spec.type} was not defined") }
        val observableClassBuilder = TypeSpec.classBuilder(spec.type)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(dataclass.type)

        observableClassBuilder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build())
        observableClassBuilder.superclass(spec.extends)
        spec.implements.forEach {
            observableClassBuilder.addSuperinterface(it)
        }


        context.registerTypeSpecBuilder(
                spec.type,
                observableClassBuilder
        )
        observableClassBuilder.addMethod(makeToString(spec.type, dataclass.propertyType, dataclass.rawSpec.toString.pattern))

        observableClassBuilder.addField(
                FieldSpec.builder(PropertyChangeSupport::class.java, "propertyChangeSupport")
                        .addModifiers(Modifier.FINAL, Modifier.PRIVATE, Modifier.TRANSIENT)
                        .initializer("new PropertyChangeSupport(this)").build())

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


        dataclass.properties
                .map { FieldSpec.builder(String::class.java, "PROP_${it.asConstName()}").addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer("\$S", it) }
                .forEach { observableClassBuilder.addField(it.build()) }

        dataclass.properties
                .map {
                    FieldSpec
                            .builder(spec.propertyType[it]!!, it)
                            .addModifiers(Modifier.PRIVATE)
                            .build()
                }
                .forEach { observableClassBuilder.addField(it) }


        dataclass.properties
                .filter { dataclass.collectionProperties.contains(it) }
                .map { buildConcreteGetMethod(it, dataclass.propertyType[it]!!).addStatement("return ($1T) ($2T) this.$3L", dataclass.propertyType[it], Object::class.java, it) }
                .forEach { observableClassBuilder.addMethod(it.build()) }

        dataclass.properties
                .filter { !dataclass.collectionProperties.contains(it) }
                .map { buildConcreteGetMethod(it, spec.observableTypes.getOrDefault(dataclass.propertyType[it], dataclass.propertyType[it])!!).addStatement("return this.$1L", it) }
                .forEach { observableClassBuilder.addMethod(it.build()) }


        observableClassBuilder.addMethod(
                MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                        .addParameter(dataclass.type, "source")
                        .also { method ->
                            dataclass.properties.forEach {
                                if (spec.propertyType[it]!!.isPrimitive) {
                                    method.addStatement("this.$1L($2L.$3L())", dataclass.propertySetMethodName[it], "source", dataclass.propertyGetMethodName[it]!!)
                                    return@forEach
                                }
                                if (!dataclass.collectionProperties.contains(it)) {
                                    method.addStatement("this.$1L($3L.$4L() == null ? null : new $2T($3L.$4L()))", dataclass.propertySetMethodName[it], spec.propertyType[it]!!, "source", dataclass.propertyGetMethodName[it]!!)
                                    return@forEach
                                }
                                val itemType = (dataclass.propertyType[it] as ParameterizedTypeName).typeArguments[0]
                                val resolvedDto = spec.dtoTypes[itemType]
                                        ?: dataclass.propertyDtoType.getOrDefault(it, itemType)
                                method.addStatement(
                                        "this.$1L($3L.$4L() == null ? null : $2T.$5L($3L.$4L().stream().map( it -> new $8T(it) ).collect($6T.$7L())))",
                                        dataclass.propertySetMethodName[it],
                                        OBSERVABLE_COLLECTIONS,
                                        "source",
                                        dataclass.propertyGetMethodName[it]!!,
                                        if ((dataclass.propertyType[it] as ParameterizedTypeName).rawType == MAP_TYPE) "observableMap" else "observableList",
                                        Collectors::class.java,
                                        "toList",
                                        spec.observableTypes.getOrDefault(itemType, spec.dtoTypes.getOrDefault(itemType, itemType))
                                )
                            }
                        }
                        .build())

        val observableProperties = dataclass.properties

        observableProperties.map {
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
                                                    .addStatement("$1L.firePropertyChange( $2L + $3S + event.getPropertyName() , event.getOldValue() , event.getNewValue() )", "propertyChangeSupport", "PROP_${it.asConstName()}", ".")
                                                    .build()
                                    )
                                    .build()
                    )
                    .build()
        }.forEach { observableClassBuilder.addField(it) }


        dataclass.properties
                .map {
                    val old = "old${it.upperFirst()}"
                    val methodBuilder = buildConcreteSetMethod(it, spec.propertyType[it]!!)

                    methodBuilder.addStatement("\$T \$L = this.\$L", spec.propertyType[it]!!, old, it)
                            .addStatement("this.\$L = \$L", it, it)
                            .beginControlFlow("if(!\$T.equals(\$L,\$L))", Objects::class.java, old, it)

                    if (spec.observableTypes.containsKey(dataclass.propertyType[it]))
                        methodBuilder
                                .addStatement("if ($1L != null) $1L.removePropertyChangeListener(this.$2L)", old, "${it}Listener")
                                .addStatement("if ($1L != null) $1L.addPropertyChangeListener(this.$2L)", it, "${it}Listener")

                    methodBuilder
                            .addStatement("this.propertyChangeSupport.firePropertyChange(\$L,\$L,\$L)", "PROP_${it.asConstName()}", old, it)
                            .endControlFlow()
                }
                .forEach { observableClassBuilder.addMethod(it.build()) }


        val dataclassBuilder = context.getTypeSpecBuilder(dataclass.type).get()
        dataclassBuilder.addType(observableClassBuilder.build())
    }

}