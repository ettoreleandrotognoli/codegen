package com.github.ettoreleandrotognoli.codegen.predicate;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class ListPredicateFactory<M, E> extends DefaultFieldPredicateFactory<M, List<E>> {

    public ListPredicateFactory(List<String> name, Function<M, List<E>> getField) {
        super(name, getField);
    }

    public ListPredicateFactory(String name, Function<M, List<E>> getField) {
        super(name, getField);
    }

    public Predicate<M> isEmpty() {
        return new DefaultFieldPredicate<>(getField, List::isEmpty);
    }

    public NumberPredicateFactory<M, Integer> size() {
        return new NumberPredicateFactory<>(nested("size"), nested(List::size));
    }

    public FieldPredicateFactory<M, E> elementAt(int index) {
        String field = String.format("elementAt(%d)", index);
        return new DefaultFieldPredicateFactory(nested(field), nested(list -> list.get(index)));
    }

    public Predicate<M> contains(Predicate<E> matcher) {
        return new DefaultFieldPredicate<>(getField, list -> list.stream().anyMatch(matcher));
    }


}
