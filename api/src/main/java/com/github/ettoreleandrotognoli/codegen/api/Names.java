package com.github.ettoreleandrotognoli.codegen.api;

import javax.naming.Name;

public interface Names {

    default String asFieldName(Named named) {
        return asFieldName(named.getName());
    }

    default String asFieldName(String name) {
        return asLowerCamelCase(name);
    }

    default String asClassName(Named named) {
        return asClassName(named.getName());
    }

    default String asClassName(String name) {
        return asUpperCamelCase(name);
    }

    default String asConst(Named named) {
        return asConst(named.getName());
    }

    default String asConst(String name) {
        return asUpperSnakeCase(name);
    }

    default String asUpperSnakeCase(Named named) {
        return asUpperSnakeCase(named.getName());
    }

    String asUpperSnakeCase(String name);

    default String asLowerCamelCase(Named named) {
        return asLowerCamelCase(named.getName());
    }

    String asLowerCamelCase(String name);

    default String asUpperCamelCase(Named named) {
        return asUpperCamelCase(named.getName());
    }

    String asUpperCamelCase(String name);

    default String asGetMethod(String name) {
        return prefix("get").asLowerCamelCase(name);
    }

    default String asGetMethod(Named named) {
        return asGetMethod(named.getName());
    }

    default String asSetMethod(String name) {
        return prefix("set").asLowerCamelCase(name);
    }

    default String asSetMethod(Named named) {
        return asSetMethod(named.getName());
    }

    Names prefix(String prefix);

    Names suffix(String suffix);
}
