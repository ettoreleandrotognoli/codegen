package com.github.ettoreleandrotognoli.codegen.predicate;

import lombok.AllArgsConstructor;

import java.util.function.Function;
import java.util.function.Predicate;

@AllArgsConstructor
public class DefaultFieldPredicate<M, T> implements Predicate<M> {

    private final Function<M, T> getField;
    private final Predicate<T> predicate;

    @Override
    public boolean test(M m) {
        T fieldValue = getField.apply(m);
        return predicate.test(fieldValue);
    }
}
