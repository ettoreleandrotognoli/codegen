package io.github.ettoreleandrotognoli.codegen.java

import com.squareup.javapoet.*
import io.github.ettoreleandrotognoli.codegen.api.Context
import io.github.ettoreleandrotognoli.codegen.core.AbstractCodeGenerator
import io.github.ettoreleandrotognoli.codegen.data.DataClassSpec
import io.github.ettoreleandrotognoli.codegen.data.ObservableSpec
import org.springframework.stereotype.Component
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.util.*
import java.util.regex.Pattern
import javax.lang.model.element.Modifier

@Component
class DataClassGenerator : AbstractCodeGenerator<DataClassSpec>(DataClassSpec::class) {


    val camelCaseRegex = Pattern.compile("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")

    fun asType(className: String): ClassName {
        return ClassName.bestGuess(className)
    }

    fun upperFirst(string: String): String {
        return string[0].toUpperCase() + string.substring(1)
    }

    fun asConstName(string: String): String {
        return camelCaseRegex.split(string)
                .map { it.toUpperCase() }
                .joinToString(separator = "_")
    }

    fun makeAbstractSetMethod(propertyName: String, type: TypeName): MethodSpec {
        val parameter = ParameterSpec.builder(type, propertyName).build()
        return MethodSpec
                .methodBuilder("set${upperFirst(propertyName)}")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(parameter)
                .build()

    }

    fun makeSimpleConcreteSetMethod(propertyName: String, type: TypeName): MethodSpec {
        val parameter = ParameterSpec.builder(type, propertyName).build()
        return buildConcreteSetMethod(propertyName, type)
                .addCode("this.$1L = $1L;", propertyName)
                .build()
    }

    fun buildConcreteSetMethod(propertyName: String, type: TypeName): MethodSpec.Builder {
        val parameter = ParameterSpec.builder(type, propertyName).build()
        return MethodSpec
                .methodBuilder("set${upperFirst(propertyName)}")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .addParameter(parameter)
    }


    fun makeAsbtractGetMethod(propertyName: String, type: TypeName): MethodSpec {
        return MethodSpec
                .methodBuilder("get${upperFirst(propertyName)}")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(type)
                .build()
    }

    fun buildConcreteGetMethod(propertyName: String, type: TypeName): MethodSpec.Builder {
        return MethodSpec
                .methodBuilder("get${upperFirst(propertyName)}")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .returns(type)
    }


    fun makeSimpleConcreteGetMethod(propertyName: String, type: TypeName): MethodSpec {
        return buildConcreteGetMethod(propertyName, type)
                .addCode("return this.$1L;", propertyName)
                .build()
    }

    fun makeField(propertyName: String, type: TypeName): FieldSpec {
        return FieldSpec.builder(type, propertyName, Modifier.PRIVATE)
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
            methodSpecBuilder.addCode("this.$1N = $2N.get${upperFirst(it)}();\n", it, "source")
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

        observableClassBuilder.addField(FieldSpec.builder(PropertyChangeSupport::class.java, "propertyChangeSupport").addModifiers(Modifier.FINAL, Modifier.PRIVATE).initializer("new PropertyChangeSupport(this)").build())

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


        mainInterfaceBuilder.addType(observableClassBuilder.build())
    }


    override fun generate(context: Context, codeSpec: DataClassSpec) {
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
        codeSpec.properties.forEach { dtoClassBuilder.addMethod(makeSimpleConcreteGetMethod(it.name, asType(it.type))) }
        codeSpec.properties.forEach { dtoClassBuilder.addMethod(makeSimpleConcreteSetMethod(it.name, asType(it.type))) }
        dtoClassBuilder.addMethod(makeCopyMethod(dtoClassName, mainInterfaceClassName, codeSpec.properties.map { it.name }))
        dtoClassBuilder.addMethod(makeCloneMethod(dtoClassName, dtoClassName))
        dtoClassBuilder.superclass(asType(codeSpec.extends))

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

        codeSpec.implements.forEach {
            mainInterfaceBuilder.addSuperinterface(asType(it))
        }

        codeSpec.properties.forEach { mainInterfaceBuilder.addMethod(makeAsbtractGetMethod(it.name, asType(it.type))) }

        if (codeSpec.observable != null) {
            observableExtension(codeSpec, codeSpec.observable, mainInterfaceBuilder)
        }

        val javaFile = JavaFile.builder(codeSpec.packageName, mainInterfaceBuilder.build())
                .build()
        javaFile.writeTo(context.project.generatedSourcePath)
    }
}