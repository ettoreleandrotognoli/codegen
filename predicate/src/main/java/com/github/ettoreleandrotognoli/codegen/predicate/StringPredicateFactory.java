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

    public Predicate<M> isBlank() {
        return new DefaultFieldPredicate<>(getField, String::isBlank);
    }

    public Predicate<M> isEmpty() {
        return new DefaultFieldPredicate<>(getField, String::isEmpty);
    }


    public NumberPredicateFactory<M, Integer> length() {
        return new NumberPredicateFactory<>(nested("length"), nested(String::length));
    }

    public StringPredicateFactory<M> charAt(int index) {
        String field = String.format("charAt(%d)", index);
        return new StringPredicateFactory<M>(nested(field), nested(it -> it.substring(index, index +1)));
    }

}
