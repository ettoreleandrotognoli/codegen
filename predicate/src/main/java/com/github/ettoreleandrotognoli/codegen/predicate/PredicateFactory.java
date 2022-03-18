package com.github.ettoreleandrotognoli.codegen.predicate;

import java.util.function.Predicate;

public interface PredicateFactory {

    <T> Predicate<T> equalsTo(T value);

    <T> Predicate<T> sameAs(T value);

    <T> Predicate<T> isNull();

    <T> Predicate<T> isNotNull();


}
