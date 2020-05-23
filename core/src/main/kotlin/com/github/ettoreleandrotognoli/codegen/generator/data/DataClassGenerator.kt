package com.github.ettoreleandrotognoli.codegen.generator.data

import com.github.ettoreleandrotognoli.codegen.*
import com.github.ettoreleandrotognoli.codegen.api.BuildContext
import com.github.ettoreleandrotognoli.codegen.api.PreBuildContext
import com.github.ettoreleandrotognoli.codegen.core.AbstractCodeGenerator
import com.github.ettoreleandrotognoli.codegen.generator.fullName
import com.github.ettoreleandrotognoli.codegen.generator.makeEquals
import com.github.ettoreleandrotognoli.codegen.generator.makeHashCode
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import org.springframework.stereotype.Component
import javax.lang.model.element.Modifier

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

@Component
class DataClassGenerator : AbstractCodeGenerator<DataClassRawSpec>(DataClassRawSpec::class) {

    override fun typedPrepareContext(context: PreBuildContext.Mutable, rawSpec: DataClassRawSpec) {
        val spec = DataClassSpec.from(rawSpec)
        context.registerIntention(spec.type)
        context.registerPreSpec(spec)
    }

    override fun typedGenerate(context: BuildContext.Mutable, rawSpec: DataClassRawSpec) {

        val spec = DataClassSpec.from(rawSpec, context)
        context.registerFullSpec(spec)

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

        constructors(spec.type, spec.propertyType).forEach {
            dtoClassBuilder.addMethod(it.build())
        }

        spec.concreteGetMethods().forEach {
            dtoClassBuilder.addMethod(it.build())
        }

        spec.concreteSetMethods().forEach {
            dtoClassBuilder.addMethod(it.addAnnotation(Override::class.java).build())
        }

        dtoClassBuilder.addMethod(copyMethod(spec.type, spec.dtoType, default = spec.rawSpec.copy.default).build())

        dtoClassBuilder.addMethod(cloneMethod(spec.dtoType).build())

        dtoClassBuilder.addMethod(shallowCopyMethod(spec.type, spec.dtoType, spec.propertyType).build())

        dtoClassBuilder.addMethod(shallowCloneMethod(spec.dtoType).build())

        dtoClassBuilder.addMethod(deepCopyMethod(spec.type, spec.dtoType, spec.propertyType, spec.resolvedDtoTypes).build())

        dtoClassBuilder.addMethod(deepCloneMethod(spec.dtoType).build())

        dtoClassBuilder.superclass(spec.extends)

        val builderClassBuilder = TypeSpec.classBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addField(FieldSpec.builder(spec.dtoType.fullName(), "prototype", Modifier.PRIVATE, Modifier.FINAL).initializer("new \$T()", spec.dtoType.fullName()).build())
                .addMethod(spec.buildMethod().build())



        spec.builderSetMethods().forEach {
            builderClassBuilder.addMethod(it.build())
        }

        if (rawSpec.toString.enable) {
            dtoClassBuilder.addMethod(makeToString(spec.dtoType, spec.propertyType, rawSpec.toString.pattern))
        }

        if (rawSpec.hashCode.enable) {
            val fields = rawSpec.hashCode.fields ?: rawSpec.properties.map { it.name }
            dtoClassBuilder.addMethod(makeHashCode(rawSpec, fields))
        }

        if (rawSpec.equals.enable) {
            val fields = rawSpec.equals.fields ?: rawSpec.properties.map { it.name }
            dtoClassBuilder.addMethod(makeEquals(spec, fields))
        }

        val mutableInterface = mutableInterfaceBuilder.build()
        val dtoClass = dtoClassBuilder.build()
        val builderClass = builderClassBuilder.build()

        val mainInterfaceBuilder =
                TypeSpec.interfaceBuilder(rawSpec.name)
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


        if (spec.rawSpec.jackson != null) {
            AnnotationSpec
                    .builder(ClassName.bestGuess(spec.rawSpec.jackson.deserializeAnnotation))
                    .addMember("as", "$1T.class", spec.dtoType.fullName())
                    .build()
                    .also {
                        mainInterfaceBuilder.addAnnotation(it)
                    }
        }

        context.registerTypeSpecBuilder(spec.type, mainInterfaceBuilder)
    }

}