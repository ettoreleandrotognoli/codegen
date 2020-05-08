package io.github.ettoreleandrotognoli.codegen.java

import com.squareup.javapoet.*
import io.github.ettoreleandrotognoli.codegen.Project
import io.github.ettoreleandrotognoli.codegen.data.DataClassSpec
import javax.lang.model.element.Modifier

class DataClassGenerator : JavaCodeGenerator<DataClassSpec>(DataClassSpec::class) {


    fun asType(className: String): ClassName {
        return ClassName.bestGuess(className)
    }

    fun upperFirst(string: String): String {
        return string[0].toUpperCase() + string.substring(1)
    }

    fun makeAbstractSetMethod(propertyName: String, type: TypeName): MethodSpec {
        val parameter = ParameterSpec.builder(type, propertyName).build()
        return MethodSpec
                .methodBuilder("set${upperFirst(propertyName)}")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(parameter)
                .build()

    }

    fun makeConcreteSetMethod(propertyName: String, type: TypeName): MethodSpec {
        val parameter = ParameterSpec.builder(type, propertyName).build()
        return MethodSpec
                .methodBuilder("set${upperFirst(propertyName)}")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .addParameter(parameter)
                .addCode("this.$1L = $1L;", propertyName)
                .build()
    }

    fun makeAsbtractGetMethod(propertyName: String, type: TypeName): MethodSpec {
        return MethodSpec
                .methodBuilder("get${upperFirst(propertyName)}")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(type)
                .build()
    }

    fun makeConcreteGetMethod(propertyName: String, type: TypeName): MethodSpec {
        return MethodSpec
                .methodBuilder("get${upperFirst(propertyName)}")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .returns(type)
                .addCode("return this.$1L;", propertyName)
                .build()
    }

    fun makeField(propertyName: String, type: TypeName): FieldSpec {
        return FieldSpec.builder(type, propertyName, Modifier.FINAL, Modifier.PRIVATE)
                .build()
    }

    fun makeCopyMethod(returnType: TypeName, parameterType: TypeName, properties: Collection<String>): MethodSpec {
        val parameter = ParameterSpec.builder(parameterType, "source").build()
        val methodSpecBuilder = MethodSpec
                .methodBuilder("copy")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(parameter)
                .returns(returnType)
        properties.forEach {
            methodSpecBuilder.addCode("this.$1N = $2N.get${upperFirst(it)}();\n", it, "other")
        }
        return methodSpecBuilder.addCode("return this;")
                .build()

    }

    fun makeCloneMethod(returnType: TypeName, concreteType: TypeName): MethodSpec {
        val methodSpecBuilder = MethodSpec
                .methodBuilder("clone")
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addCode("return new \$T().copy(this);", concreteType)
        return methodSpecBuilder.build()
    }

    fun makeBuilderSetMethod(builderType: TypeName, propertyName: String, propertyType: TypeName): MethodSpec {
        val parameter = ParameterSpec.builder(propertyType, propertyName).build()
        val methodSpecBuilder = MethodSpec
                .methodBuilder("set${upperFirst(propertyName)}")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(parameter)
                .returns(builderType)
                .addCode("this.\$N.set${upperFirst(propertyName)}(\$N);\n", "prototype", propertyName)
                .addCode("return this;")
        return methodSpecBuilder.build()
    }

    fun makeBuildMethod(returnType: TypeName): MethodSpec {
        val methodSpecBuilder = MethodSpec
                .methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addCode("return this.\$N.clone();", "prototype")
        return methodSpecBuilder.build()
    }


    override fun generate(project: Project, codeSpec: DataClassSpec) {
        val mainInterfaceClassName = ClassName.get(codeSpec.packageName, codeSpec.name)
        val mutableInterfaceClassName = ClassName.get(codeSpec.packageName, codeSpec.name + ".Mutable")
        val dtoClassName = ClassName.get(codeSpec.packageName, codeSpec.name + ".DTO")
        val builderClassName = ClassName.get(codeSpec.packageName, codeSpec.name + ".Builder")
        val mutableInterfaceBuilder = TypeSpec.interfaceBuilder("Mutable")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(mainInterfaceClassName);

        codeSpec.properties.forEach { mutableInterfaceBuilder.addMethod(makeAbstractSetMethod(it.name, asType(it.type))) }

        val dtoClassBuilder = TypeSpec.classBuilder("DTO")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(mutableInterfaceClassName)

        codeSpec.properties.forEach { dtoClassBuilder.addField(makeField(it.name, asType(it.type))) }
        codeSpec.properties.forEach { dtoClassBuilder.addMethod(makeConcreteGetMethod(it.name, asType(it.type))) }
        codeSpec.properties.forEach { dtoClassBuilder.addMethod(makeConcreteSetMethod(it.name, asType(it.type))) }
        dtoClassBuilder.addMethod(makeCopyMethod(dtoClassName, mainInterfaceClassName, codeSpec.properties.map { it.name }))
        dtoClassBuilder.addMethod(makeCloneMethod(dtoClassName, dtoClassName))

        val builderClassBuilder = TypeSpec.classBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addField(FieldSpec.builder(dtoClassName, "prototype", Modifier.PRIVATE, Modifier.FINAL).initializer("new \$T()", dtoClassName).build())
                .addMethod(makeBuildMethod(dtoClassName))

        codeSpec.properties.forEach { builderClassBuilder.addMethod(makeBuilderSetMethod(builderClassName, it.name, asType(it.type))) }

        val mainInterfaceBuilder =
                TypeSpec.interfaceBuilder(codeSpec.name)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addType(mutableInterfaceBuilder.build())
                        .addType(dtoClassBuilder.build())
                        .addType(builderClassBuilder.build())

        codeSpec.properties.forEach { mainInterfaceBuilder.addMethod(makeAsbtractGetMethod(it.name, asType(it.type))) }

        val javaFile = JavaFile.builder(codeSpec.packageName, mainInterfaceBuilder.build())
                .build()
        javaFile.writeTo(project.generatedSourcePath)
    }
}