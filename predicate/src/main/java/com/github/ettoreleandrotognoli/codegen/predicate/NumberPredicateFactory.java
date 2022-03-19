package com.github.ettoreleandrotognoli.codegen.predicate;

import java.util.List;
import java.util.function.Function;

public class NumberPredicateFactory<M, E extends Number> extends DefaultFieldPredicateFactory<M,E> {

    public NumberPredicateFactory(List<String> name, Function<M, E> getField) {
        super(name, getField);
    }

    public NumberPredicateFactory(String name, Function<M, E> getField) {
        super(name, getField);
    }
}
