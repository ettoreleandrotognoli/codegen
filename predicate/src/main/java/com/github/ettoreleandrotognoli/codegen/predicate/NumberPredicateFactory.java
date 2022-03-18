package com.github.ettoreleandrotognoli.codegen.predicate;

import java.util.List;
import java.util.function.Function;

public class NumberPredicateFactory<M> extends DefaultFieldPredicateFactory<M, Number> {

    public NumberPredicateFactory(List<String> name, Function<M, Number> getField) {
        super(name, getField);
    }

    public NumberPredicateFactory(String name, Function<M, Number> getField) {
        super(name, getField);
    }
}
