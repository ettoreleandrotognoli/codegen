package com.github.ettoreleandrotognoli.codegen.predicate;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class StringPredicateFactory<M> extends DefaultFieldPredicateFactory<M, String> {
    public StringPredicateFactory(List<String> name, Function<M, String> getField) {
        super(name, getField);
    }

    public StringPredicateFactory(String name, Function<M, String> getField) {
        super(name, getField);
    }

    public Predicate<M> startsWith(String prefix) {
        return new DefaultFieldPredicate<>(getField, it -> it.startsWith(prefix));
    }

    public Predicate<M> endsWith(String prefix) {
        return new DefaultFieldPredicate<>(getField, it -> it.endsWith(prefix));
    }

    public Predicate<M> contains(String content) {
        return new DefaultFieldPredicate<>(getField, it -> it.contains(content));
    }

    public Predicate<M> isBlank(String content) {
        return new DefaultFieldPredicate<>(getField, String::isBlank);
    }

    public Predicate<M> isEmpty(String content) {
        return new DefaultFieldPredicate<>(getField, String::isEmpty);
    }

    public NumberPredicateFactory<M> length(String content) {
        return new NumberPredicateFactory<M>(nested("length"), getField.andThen(String::length));
    }
}
