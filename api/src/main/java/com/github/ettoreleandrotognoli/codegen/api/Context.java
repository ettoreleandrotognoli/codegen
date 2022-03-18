package com.github.ettoreleandrotognoli.codegen.api;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.Map;
import java.util.stream.Stream;

public interface Context {

    Names names();

    TypeName resolveType(String name);

    TypeSpec.Builder getBuilder(ClassName typeName);

    Stream<Codegen> getCodegen();

    default <T extends Codegen> Stream<T> getCodegen(Class<T> codegenType) {
        return getCodegen()
                .filter(codegenType::isInstance)
                .map(codegenType::cast);
    }

    Stream<Map.Entry<ClassName, TypeSpec.Builder>> getBuilders();

    TypeName defaultFactory(TypeName fieldType);

    interface Builder {

        void register(Codegen codegen);

        void addType(String name, TypeName type);

        void addBuilder(ClassName type, TypeSpec.Builder builder);

        Context build();

    }
}
