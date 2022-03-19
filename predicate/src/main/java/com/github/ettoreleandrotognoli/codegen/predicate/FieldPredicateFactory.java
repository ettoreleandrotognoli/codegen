package com.github.ettoreleandrotognoli.codegen.predicate;

import java.util.function.Predicate;

public interface FieldPredicateFactory<M, T> {

    Predicate<M> sameAs(T value);

    Predicate<M> equalsTo(T value);

    Predicate<M> isNull();

    Predicate<M> isNotNull();

    Predicate<M> matchesWith(Predicate<T> predicate);
}
