package com.github.ettoreleandrotognoli.codegen.generator.builder

import com.github.ettoreleandrotognoli.codegen.api.BuildContext
import com.github.ettoreleandrotognoli.codegen.api.PreBuildContext
import com.github.ettoreleandrotognoli.codegen.core.AbstractCodeGenerator
import com.github.ettoreleandrotognoli.codegen.generator.data.DataClassSpec
import com.github.ettoreleandrotognoli.codegen.generator.fullName
import com.squareup.javapoet.*
import org.springframework.stereotype.Component
import java.util.stream.Stream
import javax.lang.model.element.Modifier

@Component
class BuilderGenerator : AbstractCodeGenerator<BuilderRawSpec>(BuilderRawSpec::class) {

    override fun typedPrepareContext(context: PreBuildContext.Mutable, rawSpec: BuilderRawSpec) {
        val spec: BuilderSpec = BuilderSpec.from(rawSpec)
        context.registerPreSpec(spec)
        context.registerIntention(spec.type)
        context.registerIntention(spec.concreteType)
    }

    fun prototypeField(concreteType: ClassName): FieldSpec.Builder {
        return FieldSpec.builder(
                concreteType,
                "prototype",
                Modifier.PRIVATE,
                Modifier.FINAL
        ).initializer("new \$T()", concreteType)
    }

    fun buildMethodSignature(type: ClassName): MethodSpec.Builder {
        return MethodSpec
                .methodBuilder("build")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(type)
    }


    fun buildMethod(dtoType: ClassName): MethodSpec.Builder {
        return MethodSpec
                .methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(dtoType)
                .addCode("return this.\$N.clone();", "prototype")

    }

    fun builderSetMethodsSignatures(
            builderType: TypeName,
            properties: Map<String, TypeName>
    ): Stream<MethodSpec.Builder> {
        return properties.entries.stream().map {
            MethodSpec.methodBuilder(it.key)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(it.value, it.key)
                    .returns(builderType)
        }
    }

    fun builderSetMethods(
            builderType: TypeName,
            properties: Map<String, TypeName>,
            propertySetMethod: Map<String, String>
    ): Stream<MethodSpec.Builder> {
        return properties.entries.stream().map {
            MethodSpec.methodBuilder(it.key)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(it.value, it.key)
                    .returns(builderType)
                    .addStatement("this.$1L.$2L($3L)", "prototype", propertySetMethod[it.key], it.key)
                    .addStatement("return this")
        }

    }

    fun builderFactoryMethod(
            builderType: TypeName,
            concreteBuilderType: TypeName
    ): MethodSpec.Builder {
        return MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(builderType)
                .addStatement("return new \$T()", concreteBuilderType)
    }


    override fun typedGenerate(context: BuildContext.Mutable, rawSpec: BuilderRawSpec) {
        val spec: BuilderSpec = BuilderSpec.from(rawSpec, context)
        val dataclass: DataClassSpec = spec.dataclass.get()

        val builderInterfaceBuilder = TypeSpec.interfaceBuilder(spec.type)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addMethod(buildMethodSignature(spec.`for`).build())
                .addSuperinterfaces(spec.implements)

        builderSetMethodsSignatures(spec.type, dataclass.propertyType)
                .forEach { builderInterfaceBuilder.addMethod(it.build()) }

        val builderClassBuilder = TypeSpec.classBuilder(spec.concreteType)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .superclass(spec.extends)
                .addSuperinterface(spec.type)
                .addField(prototypeField(dataclass.dtoType.fullName()).build())
                .addMethod(buildMethod(dataclass.dtoType.fullName()).build())

        builderSetMethods(spec.concreteType.fullName(), dataclass.propertyType, dataclass.propertySetMethodName)
                .map { it.addAnnotation(Override::class.java) }
                .forEach { builderClassBuilder.addMethod(it.build()) }

        val dataclassBuilder = context.getTypeSpecBuilder(spec.`for`).get()

        dataclassBuilder
                .addMethod(builderFactoryMethod(spec.type.fullName(), spec.concreteType.fullName()).build())

        builderInterfaceBuilder.addType(builderClassBuilder.build());

        if (spec.nestedInterface) {
            dataclassBuilder.addType(builderInterfaceBuilder.build())
        } else {
            context.registerTypeSpecBuilder(spec.type, builderInterfaceBuilder);
        }

    }
}
