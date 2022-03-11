package com.github.ettoreleandrotognoli.codegen.api;

import com.squareup.javapoet.TypeName;

public interface TypeResolver {

    void addType(String name, TypeName type);

    TypeName resolveType(String name) throws TypeResolveError;

    class TypeResolveError extends RuntimeException {
        private Object typeIdentifier;

        private TypeResolveError(String message) {
            super(message);
        }

        public static TypeResolveError of(Object typeIdentifier) {
            String message = String.format("Fail to resolve \"%s\" into a type", typeIdentifier);
            TypeResolveError error = new TypeResolveError(message);
            error.typeIdentifier = typeIdentifier;
            return error;
        }

    }
}
