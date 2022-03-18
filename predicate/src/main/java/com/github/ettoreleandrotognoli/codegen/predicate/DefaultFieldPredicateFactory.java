package com.github.ettoreleandrotognoli.codegen.predicate;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@AllArgsConstructor
public class DefaultFieldPredicateFactory<M, T> implements FieldPredicateFactory<M, T> {

    protected List<String> name;
    protected Function<M, T> getField;
    protected PredicateFactory predicateFactory;

    public DefaultFieldPredicateFactory(List<String> name, Function<M, T> getField) {
        this(name, getField, DefaultPredicateFactory.getInstance());
    }

    public DefaultFieldPredicateFactory(String name, Function<M, T> getField) {
        this(List.of(name), getField, DefaultPredicateFactory.getInstance());
    }

    public List<String> nested(String name) {
        List<String> nestedName = new ArrayList<>(this.name);
        nestedName.add(name);
        return nestedName;
    }

    public <E> Function<M, E> nested(Function<T, E> getNestedField) {
        return getField.andThen(this::check).andThen(getNestedField);
    }

    protected T check(T value) {
        return Optional.ofNullable(value).orElseThrow(() -> new RuntimeException(String.format("Failed to get field \"%s\"", name)));
    }

    @Override
    public Predicate<M> sameAs(T value) {
        return new DefaultFieldPredicate<>(getField, predicateFactory.sameAs(value));

    }

    @Override
    public Predicate<M> equalsTo(T value) {
        return new DefaultFieldPredicate<>(getField, predicateFactory.sameAs(value));
    }

    @Override
    public Predicate<M> isNull() {
        return new DefaultFieldPredicate<>(getField, predicateFactory.isNull());
    }

    @Override
    public Predicate<M> isNotNull() {
        return new DefaultFieldPredicate<>(getField, predicateFactory.isNotNull());
    }



}
