package com.github.ettoreleandrotognoli.codegen.data.plugin;

import com.github.ettoreleandrotognoli.codegen.api.Codegen;
import com.github.ettoreleandrotognoli.codegen.api.Context;
import com.github.ettoreleandrotognoli.codegen.api.Names;
import com.squareup.javapoet.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.ettoreleandrotognoli.codegen.api.Helper.*;

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
        TypeSpec.Builder classBuilder = TypeSpec.interfaceBuilder(baseInterface).addModifiers(Modifier.PUBLIC);
        builder.addType(baseInterface.simpleName(), baseInterface);
        builder.addBuilder(baseInterface, classBuilder);
        builder.addType(mutableInterface.simpleName(), mutableInterface);
        builder.addType(immutableClass.simpleName(), immutableClass);
        builder.addType(dtoClass.simpleName(), dtoClass);
    }

    public MethodSpec.Builder copyConstructorSignature(Context context) {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(baseInterface, OTHER).build());
    }

    public MethodSpec.Builder copyConstructor(Context context) {
        Names names = context.names();
        MethodSpec.Builder builder = copyConstructorSignature(context);
        for (DataSpec.DataField field : spec.getFields()) {
            builder.addStatement(
                    "$N.$N = $N.$N()",
                    THIS, field.getName(), OTHER, names.asGetMethod(field)
            );
        }
        return builder;
    }

    public MethodSpec.Builder dtoCopyConstructor(Context context) {
        Names names = context.names();
        MethodSpec.Builder builder = copyConstructorSignature(context);
        for (DataSpec.DataField field : spec.getFields()) {
            TypeName fieldType = context.resolveType(field.getType());
            if (isImmutable(fieldType)) {
                builder.addStatement(
                        "$N.$N = $N.$N()",
                        THIS, field.getName(), OTHER, names.asGetMethod(field)
                );
                continue;
            }
            boolean isGeneratedByDataClassCodegen = context.getCodegen(DataCodegen.class)
                    .map(DataCodegen::getBaseInterface)
                    .anyMatch(fieldType::equals);
            if (isGeneratedByDataClassCodegen) {
                fieldType = ((ClassName) fieldType).nestedClass("DTO");
            } else {
                fieldType = context.defaultFactory(fieldType);
            }
            TypeName factoryClass = field.getFactory()
                    .map(context::resolveType)
                    .orElse(fieldType);
            builder.addStatement(
                    "$N.$N = $T.ofNullable($N.$N()).map($T::new).orElse(null)",
                    THIS, names.asFieldName(field),
                    Optional.class,
                    OTHER, names.asGetMethod(field),
                    factoryClass
            );
        }
        return builder;
    }

    public MethodSpec.Builder immutableCopyConstructor(Context context) {
        Names names = context.names();
        MethodSpec.Builder builder = copyConstructorSignature(context);
        for (DataSpec.DataField field : spec.getFields()) {
            TypeName fieldType = context.resolveType(field.getType());
            boolean hasAsImmutable = context.getCodegen(DataCodegen.class)
                    .map(DataCodegen::getBaseInterface)
                    .anyMatch(fieldType::equals);
            if (hasAsImmutable) {
                builder.addStatement(
                        "$N.$N = $T.ofNullable($N.$N()).map($T::asImmutable).orElse(null)",
                        THIS, field.getName(),
                        Optional.class,
                        OTHER, names.asGetMethod(field),
                        fieldType
                );
            } else if (isList(fieldType)) {
                builder.addStatement(
                        "$N.$N = $T.ofNullable($N.$N()).map($T::unmodifiableList).orElse(null)",
                        THIS, names.asFieldName(field),
                        Optional.class,
                        OTHER, names.asGetMethod(field),
                        Collections.class
                );
            } else if (isMap(fieldType)) {
                builder.addStatement(
                        "$N.$N = $T.unmodifiableMap($N.$N())",
                        THIS, names.asFieldName(field),
                        Collections.class,
                        OTHER, names.asGetMethod(field)
                );
            } else {
                builder.addStatement(
                        "$N.$N = $N.$N()",
                        THIS, field.getName(), OTHER, names.asGetMethod(field)
                );
            }
        }
        return builder;
    }

    public MethodSpec.Builder immutableOfMethod(Context context) {
        return MethodSpec.methodBuilder(OF)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(baseInterface, SOURCE)
                .returns(immutableClass)
                .addStatement("return new $T($N)", immutableClass, SOURCE);
    }

    public MethodSpec.Builder shallowCopySignature(Context context, TypeName returnType) {
        return MethodSpec.methodBuilder("shallowCopy")
                .addModifiers(Modifier.PUBLIC)
                .returns(returnType)
                .addParameter(ParameterSpec.builder(baseInterface, OTHER).build());
    }

    public MethodSpec.Builder shallowCopyMethod(Context context, TypeName returnType) {
        Names names = context.names();
        MethodSpec.Builder builder = shallowCopySignature(context, returnType);
        for (DataSpec.DataField field : spec.getFields()) {
            builder.addStatement(
                    "$N.$N = $N.$N()",
                    THIS, field.getName(), OTHER, names.asGetMethod(field)
            );
        }
        builder.addStatement("return this");
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

    public String defaultToStringPattern(Context context, ClassName className) {
        Names names = context.names();
        return spec.getFields()
                .stream()
                .map(field -> String.format("%s=$%s", names.asFieldName(field), names.asFieldName(field)))
                .collect(Collectors.joining(", ", className + " { ", " }"));
    }

    public MethodSpec.Builder toStringMethod(Context context, ClassName className) {
        Names names = context.names();
        String pattern = spec.getToString()
                .flatMap(DataSpec.ToString::getPattern)
                .orElseGet(() -> defaultToStringPattern(context, className));
        Matcher matcher = Pattern.compile("\\$([a-zA-Z0-9_]+)").matcher(pattern);
        List<String> fields = new ArrayList<>(spec.getFields().size());
        while (matcher.find()) {
            fields.add(matcher.group(1));
        }
        Object[] codeParams = new Object[fields.size() + 1];
        codeParams[0] = pattern.replaceAll("\\$[a-zA-Z0-9_]+", "%s");
        for (int index = 0; index < fields.size(); index += 1) {
            codeParams[index + 1] = names.asGetMethod(fields.get(index));
        }
        String formatArgs = Stream.concat(Stream.of("$S"), fields.stream().map(it -> "$N()")).collect(Collectors.joining(", "));
        return toStringSignature()
                .addStatement(
                        String.format("return String.format(%s)", formatArgs),
                        codeParams
                );
    }

    public MethodSpec.Builder getSignature(Context context, DataSpec.DataField field) {
        Names names = context.names();
        TypeName fieldType = context.resolveType(field.getType());
        return MethodSpec.methodBuilder(names.asGetMethod(field))
                .addModifiers(Modifier.PUBLIC)
                .returns(fieldType);
    }

    public MethodSpec.Builder getMethod(Context context, DataSpec.DataField field) {
        return getSignature(context, field)
                .addStatement("return $N", field.getName());
    }

    public MethodSpec.Builder setSignature(Context context, DataSpec.DataField field) {
        Names names = context.names();
        TypeName fieldType = context.resolveType(field.getType());
        return MethodSpec.methodBuilder(names.asSetMethod(field))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(fieldType, field.getName()).build())
                .returns(TypeName.VOID);
    }

    public MethodSpec.Builder setMethod(Context context, DataSpec.DataField field) {
        return setSignature(context, field)
                .addStatement("$N.$N = $N", THIS, field.getName(), field.getName());
    }

    public MethodSpec.Builder replaceSignature(Context context, DataSpec.DataField field, TypeName returnType) {
        Names names = context.names();
        TypeName fieldType = context.resolveType(field.getType());
        return MethodSpec.methodBuilder(names.asFieldName(field))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(fieldType, field.getName()).build())
                .returns(returnType);
    }

    public MethodSpec.Builder replaceMethod(Context context, DataSpec.DataField field, TypeName returnType) {
        return replaceSignature(context, field, returnType)
                .addStatement("$N.$N = $N", THIS, field.getName(), field.getName())
                .addStatement("return $N", THIS);
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
                        returnType, THIS
                );
    }

    public MethodSpec.Builder immutableCloneMethod(Context context, TypeName returnType) {
        return cloneSignature(context, returnType)
                .addAnnotation(Override.class)
                .addStatement("return $N", THIS);
    }


    public MethodSpec.Builder asImmutableSignature(Context context) {
        return MethodSpec.methodBuilder("asImmutable")
                .returns(immutableClass)
                .addModifiers(Modifier.PUBLIC);
    }

    public MethodSpec.Builder superAsImmutableMethod(Context context) {
        return asImmutableSignature(context)
                .addModifiers(Modifier.DEFAULT)
                .addStatement("return new $T($N)", immutableClass, THIS);
    }

    public MethodSpec.Builder immutableAsImmutableMethod(Context context) {
        return asImmutableSignature(context)
                .addAnnotation(Override.class)
                .addStatement("return $N", THIS);
    }

    public MethodSpec.Builder asMutableSignature(Context context) {
        return MethodSpec.methodBuilder("asMutable")
                .returns(mutableInterface)
                .addModifiers(Modifier.PUBLIC);
    }

    public MethodSpec.Builder superAsMutableMethod(Context context) {
        return asMutableSignature(context)
                .addModifiers(Modifier.DEFAULT)
                .addStatement("return new $T($N)", dtoClass, THIS);
    }

    public MethodSpec.Builder mutableAsMutableMethod(Context context) {
        return asMutableSignature(context)
                .addModifiers(Modifier.DEFAULT)
                .addStatement("return $N", THIS);
    }

    public MethodSpec.Builder dtoAsMutableMethod(Context context) {
        return MethodSpec.methodBuilder("asMutable")
                .returns(dtoClass)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addStatement("return $N", THIS);
    }

    public MethodSpec.Builder equalsSignature(Context context) {
        return MethodSpec.methodBuilder("equals")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(Object.class, OTHER).build())
                .returns(boolean.class);
    }

    public MethodSpec.Builder equalsMethod(Context context) {
        Names names = context.names();
        String name = names.asFieldName(spec);
        MethodSpec.Builder methodBuilder = equalsSignature(context)
                .addStatement("if ( $N == $N) return true", THIS, OTHER)
                .addStatement("if ( $T.isNull($N) ) return false", Objects.class, OTHER)
                .addStatement("if (!( $N instanceof $T)) return false", OTHER, baseInterface)
                .addStatement("$T $N = ($T) $N", baseInterface, name, baseInterface, OTHER);
        for (DataSpec.DataField field : spec.getFields()) {
            methodBuilder.addStatement(
                    "if ( !$T.equals($N(),$N.$N()) ) return false",
                    Objects.class, names.asGetMethod(field), name, names.asGetMethod(field)
            );
        }
        methodBuilder.addStatement("return true");
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
        classBuilder.addMethod(dtoCopyConstructor(context).build());
        for (DataSpec.DataField field : getSpec().getFields()) {
            TypeName fieldType = context.resolveType(field.getType());
            FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, field.getName())
                    .addModifiers(Modifier.PRIVATE);
            field.getDefaultValue().ifPresent(value -> {
                if (isString(fieldType)) {
                    fieldBuilder.initializer("$S", value);
                } else {
                    fieldBuilder.initializer("$L", value);
                }
            });
            classBuilder.addField(fieldBuilder.build());
        }
        for (DataSpec.DataField field : getSpec().getFields()) {
            MethodSpec getMethod = getMethod(context, field)
                    .addAnnotation(Override.class)
                    .build();
            classBuilder.addMethod(getMethod);
            MethodSpec setMethod = setMethod(context, field)
                    .addAnnotation(Override.class)
                    .build();
            classBuilder.addMethod(setMethod);
            MethodSpec replaceMethod = replaceMethod(context, field, dtoClass)
                    .addAnnotation(Override.class)
                    .build();
            classBuilder.addMethod(replaceMethod);
        }
        classBuilder.addMethod(dtoAsMutableMethod(context).build());
        classBuilder.addMethod(cloneMethod(context, dtoClass).build());
        classBuilder.addMethod(shallowCopyMethod(context, dtoClass).build());

        if (spec.getToString().map(DataSpec.ToString::isEnable).orElse(true)) {
            classBuilder.addMethod(toStringMethod(context, dtoClass).build());
        }
        classBuilder.addMethod(equalsMethod(context).build());
        classBuilder.addMethod(hashCodeMethod(context).build());
        return classBuilder.build();
    }

    public TypeSpec createMutable(Context context) {
        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(mutableInterface)
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addSuperinterface(baseInterface);
        interfaceBuilder.addMethod(mutableAsMutableMethod(context).build());
        for (DataSpec.DataField field : spec.getFields()) {
            interfaceBuilder.addMethod(setSignature(context, field).addModifiers(Modifier.ABSTRACT).build());
        }
        for (DataSpec.DataField field : spec.getFields()) {
            interfaceBuilder.addMethod(replaceSignature(context, field, mutableInterface).addModifiers(Modifier.ABSTRACT).build());
        }
        return interfaceBuilder.build();
    }

    public TypeSpec createImmutable(Context context) {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(immutableClass)
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addSuperinterface(baseInterface);
        classBuilder.addMethod(immutableCopyConstructor(context).build());
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
        classBuilder.addMethod(immutableOfMethod(context).build());
        classBuilder.addMethod(immutableAsImmutableMethod(context).build());
        classBuilder.addMethod(immutableCloneMethod(context, immutableClass).build());
        if (spec.getToString().map(DataSpec.ToString::isEnable).orElse(true)) {
            classBuilder.addMethod(toStringMethod(context, immutableClass).build());
        }
        classBuilder.addMethod(equalsMethod(context).build());
        classBuilder.addMethod(hashCodeMethod(context).build());
        return classBuilder.build();
    }


    @Override
    public void generate(Context context) {
        Objects.requireNonNull(context);
        TypeSpec.Builder classBuilder = context.getBuilder(baseInterface);

        spec.getInterfaces().stream()
                .map(context::resolveType)
                .forEach(classBuilder::addSuperinterface);
        Names propNames = context.names().prefix("PROP");
        for (DataSpec.DataField field : getSpec().getFields()) {
            FieldSpec.Builder fieldBuilder = FieldSpec.builder(String.class, propNames.asConst(field))
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                    .initializer("$S", field.getName());
            classBuilder.addField(fieldBuilder.build());
        }
        for (DataSpec.DataField field : spec.getFields()) {
            classBuilder.addMethod(getSignature(context, field).addModifiers(Modifier.ABSTRACT).build());
        }
        classBuilder.addMethod(superAsImmutableMethod(context).build());
        classBuilder.addMethod(superAsMutableMethod(context).build());
        //classBuilder.addMethod(toStringMethod(context, baseInterface).addModifiers(Modifier.DEFAULT).build());
        //classBuilder.addMethod(cloneSignature(context, baseInterface).addModifiers(Modifier.ABSTRACT).build());

        classBuilder.addType(createImmutable(context));
        classBuilder.addType(createMutable(context));
        classBuilder.addType(createDTO(context));
    }

}
