package com.github.ettoreleandrotognoli.codegen.api.impl;

import com.github.ettoreleandrotognoli.codegen.api.Codegen;
import com.github.ettoreleandrotognoli.codegen.api.Context;
import com.github.ettoreleandrotognoli.codegen.api.Names;
import com.github.ettoreleandrotognoli.codegen.api.TypeResolver;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import lombok.AllArgsConstructor;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

@AllArgsConstructor
public class CodegenContext implements Context {

    private TypeResolver typeResolver;
    private Map<String, Class<?>> factories;
    private Map<ClassName, TypeSpec.Builder> builders;
    private List<Codegen> codegenList;
    private Names name;


    public static class Builder implements Context.Builder {
        private Map<String, Class<?>> factories;
        private Map<ClassName, TypeSpec.Builder> builders = new HashMap<>();
        private TypeResolver typeResolver = TypeResolverImpl.createDefault();
        private List<Codegen> codegenList = new LinkedList<>();

        @Override
        public void addType(String name, TypeName type) {
            typeResolver.addType(name, type);
        }

        public Builder(Map<String, Class<?>> factories) {
            this.factories = factories;
        }

        @Override
        public void addBuilder(ClassName type, TypeSpec.Builder builder) {
            builders.put(type, builder);
        }

        @Override
        public void register(Codegen codegen) {
            codegenList.add(codegen);
        }

        @Override
        public CodegenContext build() {
            return new CodegenContext(
                    typeResolver,
                    factories,
                    new HashMap<>(builders),
                    Collections.unmodifiableList(codegenList),
                    new NameImpl()
            );
        }

    }

    @Override
    public TypeSpec.Builder getBuilder(ClassName typeName) {
        if (builders.containsKey(typeName)) {
            return builders.get(typeName);
        }
        TypeSpec.Builder builder = TypeSpec.classBuilder(typeName.toString());
        builders.put(typeName, builder);
        return builder;
    }

    @Override
    public Stream<Codegen> getCodegen() {
        return codegenList.stream();
    }

    public TypeName resolveType(String name) {
        return typeResolver.resolveType(name);
    }

    @Override
    public Stream<Map.Entry<ClassName, TypeSpec.Builder>> getBuilders() {
        return builders.entrySet().stream();
    }

    public Names names() {
        return name;
    }

    @Override
    public TypeName defaultFactory(TypeName fieldType) {
        String canonicalName = fieldType.toString();
        canonicalName = canonicalName.contains("<") ? canonicalName.substring(0, canonicalName.indexOf("<")) : canonicalName;
        if (factories.containsKey(canonicalName)) {
            return ClassName.get(factories.get(canonicalName));
        }
        return fieldType;
    }
}
