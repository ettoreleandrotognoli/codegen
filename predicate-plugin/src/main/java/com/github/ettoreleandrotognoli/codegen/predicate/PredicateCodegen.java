package com.github.ettoreleandrotognoli.codegen.predicate;

import com.github.ettoreleandrotognoli.codegen.api.Codegen;
import com.github.ettoreleandrotognoli.codegen.api.Context;
import com.github.ettoreleandrotognoli.codegen.api.Names;
import com.github.ettoreleandrotognoli.codegen.data.plugin.DataCodegen;
import com.github.ettoreleandrotognoli.codegen.data.plugin.DataSpec;
import com.squareup.javapoet.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.github.ettoreleandrotognoli.codegen.api.Helper.isList;
import static com.github.ettoreleandrotognoli.codegen.api.Helper.isString;

@AllArgsConstructor
@Getter
public class PredicateCodegen implements Codegen {

    private static final TypeVariableName M = TypeVariableName.get("M");

    private ClassName baseInterface;
    private ClassName predicateFactoryInterface;
    private ClassName predicateFactoryImpl;
    private PredicateSpec spec;

    public static PredicateCodegen from(PredicateSpec spec) {
        ClassName baseInterface = ClassName.get(spec.getPack(), spec.getName());
        ClassName predicateFactoryInterface = baseInterface.nestedClass("PredicateFactory");
        ClassName predicateFactoryImpl = baseInterface.nestedClass("PredicateFactoryImpl");
        return new PredicateCodegen(
                baseInterface,
                predicateFactoryInterface,
                predicateFactoryImpl,
                spec
        );
    }

    @Override
    public void prepare(Context.Builder builder) {
    }

    public TypeName concretePredicateTypeFor(Context context, TypeName typeName, TypeName genericType) {
        Optional<PredicateCodegen> predicateCodegen = context.getCodegen(PredicateCodegen.class)
                .filter(it -> typeName.equals(it.getBaseInterface()))
                .findAny();

        boolean hasPredicateCodegen = predicateCodegen.isPresent();
        if (hasPredicateCodegen) {
            return ParameterizedTypeName.get(predicateCodegen.get().getPredicateFactoryImpl(), genericType);
        }
        return isString(typeName) ? ParameterizedTypeName.get(ClassName.get(StringPredicateFactory.class), genericType)
                : isList(typeName) ? ParameterizedTypeName.get(ClassName.get(ListPredicateFactory.class), genericType, ((ParameterizedTypeName)typeName).typeArguments.get(0))
                : ClassName.get(DefaultFieldPredicateFactory.class);
    }

    public TypeName concretePredicateTypeFor(Context context, TypeName typeName) {
        return concretePredicateTypeFor(context, typeName, M);
    }

    public TypeName interfacePredicateTypeFor(Context context, TypeName typeName) {
        Optional<PredicateCodegen> predicateCodegen = context.getCodegen(PredicateCodegen.class)
                .filter(it -> it.getBaseInterface().equals(typeName))
                .findAny();
        boolean hasPredicateCodegen = predicateCodegen.isPresent();
        if (hasPredicateCodegen) {
            return ParameterizedTypeName.get(predicateCodegen.get().getPredicateFactoryInterface(), baseInterface);
        }
        return isString(typeName) ? ParameterizedTypeName.get(ClassName.get(StringPredicateFactory.class), baseInterface)
                : isList(typeName) ? ParameterizedTypeName.get(ClassName.get(ListPredicateFactory.class), baseInterface, ((ParameterizedTypeName)typeName).typeArguments.get(0))
                : ParameterizedTypeName.get(ClassName.get(FieldPredicateFactory.class), baseInterface, typeName);
    }

    public TypeName genericPredicateTypeFor(Context context, TypeName typeName) {
        Optional<PredicateCodegen> predicateCodegen = context.getCodegen(PredicateCodegen.class)
                .filter(it -> it.getBaseInterface().equals(typeName))
                .findAny();
        boolean hasPredicateCodegen = predicateCodegen.isPresent();
        if (hasPredicateCodegen) {
            return ParameterizedTypeName.get(predicateCodegen.get().getPredicateFactoryInterface(), M);
        }
        return isString(typeName) ? ParameterizedTypeName.get(ClassName.get(StringPredicateFactory.class), M)
                : isList(typeName) ? ParameterizedTypeName.get(ClassName.get(ListPredicateFactory.class), M, ((ParameterizedTypeName)typeName).typeArguments.get(0))
                : ParameterizedTypeName.get(ClassName.get(FieldPredicateFactory.class), M, typeName);
    }

    @Override
    public void generate(Context context) {
        Names names = context.names();
        Names propName = names.prefix("PROP");
        Optional<DataCodegen> any = context.getCodegen(DataCodegen.class)
                .filter(it -> baseInterface.equals(it.getBaseInterface()))
                .findAny();
        if (any.isEmpty()) {
            return;
        }
        DataCodegen dataCodegen = any.get();
        TypeSpec.Builder builder = context.getBuilder(baseInterface);
        for (DataSpec.DataField field : dataCodegen.getSpec().getFields()) {
            TypeName fieldType = context.resolveType(field.getType());
            TypeName returnType = interfacePredicateTypeFor(context, fieldType);
            TypeName concretePredicateFactory = concretePredicateTypeFor(context, fieldType, baseInterface);

            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(field.getName())
                    .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                    .returns(returnType)
                    //.addStatement("throw new $T()", UnsupportedOperationException.class)
                    .addStatement(
                            "$T<$T, $T> $N = $T::$N",
                            Function.class, baseInterface, fieldType,
                            names.asGetMethod(field),
                            baseInterface, names.asGetMethod(field)
                    )
                    .addStatement(
                            "return new $T( $N, $N )",
                            concretePredicateFactory, propName.asConst(field), names.asGetMethod(field)
                    );
            builder.addMethod(methodBuilder.build());
        }


        TypeSpec.Builder predicateFactory = TypeSpec.interfaceBuilder(predicateFactoryInterface)
                .addTypeVariable(M)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(FieldPredicateFactory.class), M, baseInterface))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        for (DataSpec.DataField field : dataCodegen.getSpec().getFields()) {
            TypeName fieldType = context.resolveType(field.getType());
            TypeName returnType = genericPredicateTypeFor(context, fieldType);
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(field.getName())
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(returnType);
            predicateFactory.addMethod(methodBuilder.build());
        }

        builder.addType(predicateFactory.build());


        TypeSpec.Builder predicateImplFactory = TypeSpec.classBuilder(predicateFactoryImpl)
                .superclass(ParameterizedTypeName.get(ClassName.get(DefaultFieldPredicateFactory.class), M, baseInterface))
                .addTypeVariable(M)
                .addSuperinterface(ParameterizedTypeName.get(predicateFactoryInterface, M))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        ParameterizedTypeName stringList = ParameterizedTypeName.get(List.class, String.class);
        ParameterizedTypeName getFieldFunction = ParameterizedTypeName.get(
                ClassName.get(Function.class),
                M,
                baseInterface
        );

        predicateImplFactory.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(String.class, "name")
                        .addParameter(getFieldFunction, "getField")
                        .addStatement("super(name, getField)")
                        .build()
        );
        predicateImplFactory.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(stringList, "name")
                        .addParameter(getFieldFunction, "getField")
                        .addStatement("super(name, getField)")
                        .build()
        );

        for (DataSpec.DataField field : dataCodegen.getSpec().getFields()) {
            TypeName fieldType = context.resolveType(field.getType());
            TypeName returnType = genericPredicateTypeFor(context, fieldType);
            TypeName concreteType = concretePredicateTypeFor(context, fieldType);
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(field.getName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(returnType)
                    .addStatement(
                            "return new $T( nested($N), nested($T::$N) )",
                            concreteType, propName.asConst(field),
                            baseInterface, names.asGetMethod(field)
                    );
            predicateImplFactory.addMethod(methodBuilder.build());
        }

        builder.addType(predicateImplFactory.build());


    }
}
