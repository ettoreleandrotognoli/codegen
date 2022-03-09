package com.github.ettoreleandrotognoli.codegen.data.plugin;

import com.github.ettoreleandrotognoli.codegen.api.Codegen;
import com.github.ettoreleandrotognoli.codegen.api.Context;
import com.squareup.javapoet.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
@Builder
@Getter
public class DataCodegen implements Codegen {

    private ClassName baseInterface;
    private ClassName mutableInterface;
    private ClassName immutableClass;
    private ClassName dtoClass;
    private DataSpec spec;

    public static DataCodegen from(DataSpec spec) {
        ClassName baseInterface = ClassName.get(spec.getPack(), spec.getName());
        ClassName mutableInterface = baseInterface.nestedClass("Mutable");
        ClassName immutableClass = baseInterface.nestedClass("Immutable");
        ClassName dtoClass = baseInterface.nestedClass("DTO");
        return DataCodegen.builder()
                .baseInterface(baseInterface)
                .dtoClass(dtoClass)
                .mutableInterface(mutableInterface)
                .immutableClass(immutableClass)
                .spec(spec)
                .build();
    }

    @Override
    public void prepare(Context.Builder builder) {

    }

    public MethodSpec.Builder copyConstructorSignature(Context context) {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(baseInterface, "other").build());
    }

    public MethodSpec.Builder copyConstructor(Context context) {
        MethodSpec.Builder builder = copyConstructorSignature(context);
        for (DataSpec.DataField field : spec.getFields()) {
            builder.addStatement(
                    "$N.$N = $N.$N()",
                    "this", field.getName(), "other", field.getMethod()
            );
        }
        return builder;
    }

    public MethodSpec.Builder emptyConstructorSignature(Context context) {
        return MethodSpec.constructorBuilder();
    }

    public MethodSpec.Builder emptyConstructor(Context context) {
        return emptyConstructorSignature(context).addModifiers(Modifier.PUBLIC);
    }

    public MethodSpec.Builder toStringSignature() {
        return MethodSpec.methodBuilder("toString")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class);
    }

    public MethodSpec.Builder toStringMethod() {
        return toStringSignature()
                .addStatement(
                        "return String.format($S, getClass().getSimpleName() )",
                        "%s {}"
                );
    }

    public MethodSpec.Builder getSignature(Context context, DataSpec.DataField field) {
        TypeName fieldType = context.resolveType(field.getType());
        return MethodSpec.methodBuilder(field.getMethod())
                .addModifiers(Modifier.PUBLIC)
                .returns(fieldType);
    }

    public MethodSpec.Builder getMethod(Context context, DataSpec.DataField field) {
        return getSignature(context, field)
                .addStatement("return $N", field.getName());
    }

    public MethodSpec.Builder setSignature(Context context, DataSpec.DataField field, TypeName returnType) {
        TypeName fieldType = context.resolveType(field.getType());
        return MethodSpec.methodBuilder(field.setMethod())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(fieldType, field.getName()).build())
                .returns(returnType);
    }

    public MethodSpec.Builder setMethod(Context context, DataSpec.DataField field, TypeName returnType) {
        return setSignature(context, field, returnType)
                .addStatement("$N.$N = $N", "this", field.getName(), field.getName())
                .addStatement("return $N", "this");
    }

    public MethodSpec.Builder cloneSignature(Context context, TypeName returnType) {
        return MethodSpec.methodBuilder("clone")
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType);
    }

    public MethodSpec.Builder cloneMethod(Context context, TypeName returnType) {
        return cloneSignature(context, returnType)
                .addStatement(
                        "return new $T($N)",
                        returnType, "this"
                );
    }

    public MethodSpec.Builder equalsSignature(Context context) {
        return MethodSpec.methodBuilder("equals")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(Object.class, "other").build())
                .returns(boolean.class);
    }

    public MethodSpec.Builder equalsMethod(Context context) {
        String name = Character.toLowerCase(spec.getName().charAt(0)) + spec.getName().substring(1);
        MethodSpec.Builder methodBuilder = equalsSignature(context)
                .addStatement("if ( $N == $N) return true", "this", "other")
                .addStatement("if ( $T.isNull($N) ) return false", Objects.class, "other")
                .addStatement("if (!( $N instanceof $T)) return false", "other", baseInterface)
                .addStatement("$T $N = ($T) $N", baseInterface, name, baseInterface, "other");
        for (DataSpec.DataField field : spec.getFields()) {
            methodBuilder.addStatement(
                    "if ( !$T.equals($N(),$N.$N()) ) return false",
                    Objects.class, field.getMethod(), name, field.getMethod()
            );
        }
        methodBuilder.addStatement("return false");
        return methodBuilder;
    }

    public MethodSpec.Builder hashCodeSignature(Context context) {
        return MethodSpec.methodBuilder("hashCode")
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class);
    }

    public MethodSpec.Builder hashCodeMethod(Context context) {
        String fields = spec.getFields()
                .stream()
                .map(DataSpec.DataField::getName)
                .collect(Collectors.joining(", "));
        return hashCodeSignature(context)
                .addStatement("return $T.hash(" + fields + ")", Objects.class);
    }


    public TypeSpec createDTO(Context context) {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(dtoClass)
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addSuperinterface(mutableInterface);
        classBuilder.addMethod(emptyConstructor(context).build());
        classBuilder.addMethod(copyConstructor(context).build());
        for (DataSpec.DataField field : getSpec().getFields()) {
            TypeName fieldType = context.resolveType(field.getType());
            FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, field.getName())
                    .addModifiers(Modifier.PRIVATE);
            classBuilder.addField(fieldBuilder.build());
        }
        for (DataSpec.DataField field : getSpec().getFields()) {
            MethodSpec getMethod = getMethod(context, field)
                    .addAnnotation(Override.class)
                    .build();
            classBuilder.addMethod(getMethod);
            MethodSpec setMethod = setMethod(context, field, dtoClass)
                    .addAnnotation(Override.class)
                    .build();
            classBuilder.addMethod(setMethod);
        }
        classBuilder.addMethod(cloneMethod(context, dtoClass).build());
        classBuilder.addMethod(toStringMethod().build());
        classBuilder.addMethod(equalsMethod(context).build());
        classBuilder.addMethod(hashCodeMethod(context).build());
        return classBuilder.build();
    }

    public TypeSpec createMutable(Context context) {
        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(mutableInterface)
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addSuperinterface(baseInterface);
        for (DataSpec.DataField field : spec.getFields()) {
            interfaceBuilder.addMethod(setSignature(context, field, mutableInterface).addModifiers(Modifier.ABSTRACT).build());
        }
        interfaceBuilder.addMethod(cloneSignature(context, mutableInterface).addModifiers(Modifier.ABSTRACT).build());
        return interfaceBuilder.build();
    }

    public TypeSpec createImmutable(Context context) {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(immutableClass)
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addSuperinterface(baseInterface);
        classBuilder.addMethod(copyConstructor(context).build());
        for (DataSpec.DataField field : getSpec().getFields()) {
            TypeName fieldType = context.resolveType(field.getType());
            FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, field.getName())
                    .addModifiers(Modifier.FINAL, Modifier.PRIVATE);
            classBuilder.addField(fieldBuilder.build());
        }
        for (DataSpec.DataField field : getSpec().getFields()) {
            MethodSpec method = getMethod(context, field)
                    .addAnnotation(Override.class)
                    .build();
            classBuilder.addMethod(method);
        }
        classBuilder.addMethod(cloneMethod(context, immutableClass).build());
        classBuilder.addMethod(toStringMethod().build());
        classBuilder.addMethod(equalsMethod(context).build());
        classBuilder.addMethod(hashCodeMethod(context).build());
        return classBuilder.build();
    }


    @Override
    public void generate(Context context) {
        TypeSpec.Builder classBuilder = TypeSpec.interfaceBuilder(baseInterface)
                .addModifiers(Modifier.PUBLIC);
        spec.getParent()
                .map(context::resolveType)
                .ifPresent(classBuilder::superclass);
        spec.getInterfaces().stream()
                .map(context::resolveType)
                .forEach(classBuilder::addSuperinterface);

        for (DataSpec.DataField field : spec.getFields()) {
            classBuilder.addMethod(getSignature(context, field).addModifiers(Modifier.ABSTRACT).build());
        }
        //classBuilder.addMethod(toStringMethod().addModifiers(Modifier.DEFAULT).build());
        classBuilder.addMethod(cloneSignature(context, baseInterface).addModifiers(Modifier.ABSTRACT).build());

        classBuilder.addType(createImmutable(context));
        classBuilder.addType(createMutable(context));
        classBuilder.addType(createDTO(context));

        TypeSpec typeSpec = classBuilder.build();
        JavaFile.Builder javaFile = JavaFile.builder(spec.getPack(), typeSpec);
        try {
            Path outputPath = context.resolveFile(spec.getOutput()).toPath();
            javaFile.build().writeTo(outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
