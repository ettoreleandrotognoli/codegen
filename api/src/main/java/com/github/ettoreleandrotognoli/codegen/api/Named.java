package com.github.ettoreleandrotognoli.codegen.api;

@FunctionalInterface
public interface Named {

    String getName();

    static Named of(String name) {
        return () -> name;
    }
}
