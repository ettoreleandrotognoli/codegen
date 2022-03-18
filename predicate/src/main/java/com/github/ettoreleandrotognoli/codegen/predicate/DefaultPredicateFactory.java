package com.github.ettoreleandrotognoli.codegen.predicate;

import java.util.Objects;
import java.util.function.Predicate;

public class DefaultPredicateFactory implements PredicateFactory {

    private static final DefaultPredicateFactory INSTANCE = new DefaultPredicateFactory();

    public static DefaultPredicateFactory getInstance() {
        return INSTANCE;
    }


    public <E> Predicate<E> equalsTo(E value) {
        return it -> Objects.equals(value, it);
    }


    public <T> Predicate<T> sameAs(T value) {
        return it -> it == value;
    }

    @Override
    public <T> Predicate<T> isNull() {
        return Objects::isNull;
    }

    @Override
    public <T> Predicate<T> isNotNull() {
        return Objects::nonNull;
    }
}
