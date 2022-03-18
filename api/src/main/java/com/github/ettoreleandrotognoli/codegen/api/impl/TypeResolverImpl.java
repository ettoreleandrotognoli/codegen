package com.github.ettoreleandrotognoli.codegen.api.impl;

import com.github.ettoreleandrotognoli.codegen.api.TypeResolver;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import lombok.AllArgsConstructor;

import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
public class TypeResolverImpl implements TypeResolver {

    private Set<String> defaultPackages;
    private Map<String, TypeName> aliases;

    @Override
    public void addType(String name, TypeName type) {
        aliases.put(name, type);
    }

    @Override
    public TypeName resolveType(String name) throws TypeResolveError {
        if (name.contains("<")) {
            return resolveGenericType(name);
        }
        if (aliases.containsKey(name)) {
            return aliases.get(name);
        }
        try {
            return resolveClass(name);
        } catch (IllegalArgumentException illegalArgumentException) {
            try {
                return (TypeName) TypeName.class
                        .getDeclaredField(name.toUpperCase())
                        .get(null);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw TypeResolveError.of(name);
            }
        }
    }

    public ClassName resolveClass(String name) throws TypeResolveError {
        for (String packageName : defaultPackages) {
            try {
                Class<?> type = Class.forName(String.format("%s.%s", packageName, name));
                return ClassName.get(type);
            } catch (ClassNotFoundException e) {

            }
        }
        return ClassName.bestGuess(name);
    }

    public ParameterizedTypeName resolveGenericType(String name) {
        String genericType = name.substring(0, name.indexOf("<"));
        String[] typeParameters = name.substring(name.indexOf("<") + 1, name.lastIndexOf(">")).split(",");
        TypeName[] typeNames = Arrays.stream(typeParameters)
                .map(this::resolveType)
                .collect(Collectors.toList())
                .toArray(TypeName[]::new);
        ClassName genericClass = resolveClass(genericType);
        return ParameterizedTypeName.get(genericClass, typeNames);
    }

    public static TypeResolverImpl createDefault() {
        return new TypeResolverImpl(new HashSet<>(Arrays.asList(
                "java.util",
                "java.lang"
        )), new HashMap<>());
    }


}
