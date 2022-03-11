package com.github.ettoreleandrotognoli.codegen.api.impl;

import com.github.ettoreleandrotognoli.codegen.api.Names;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@NoArgsConstructor
public class NameImpl implements Names {

    List<String> prefixes = new ArrayList<>();
    List<String> suffixes = new ArrayList<>();


    public Stream<String> split(String name) {
        return Stream.concat(
                Stream.concat(
                        prefixes.stream(),
                        Stream.of(name
                                .replaceAll("([^A-Z0-9])([A-Z0-9])", "$1 $2")
                                .replaceAll("[-_\\.]", " ")
                                .split("\\s"))
                ), suffixes.stream());
    }

    @Override
    public String asUpperSnakeCase(String name) {
        return split(name)
                .map(String::toUpperCase)
                .collect(Collectors.joining("_"));
    }

    @Override
    public String asUpperCamelCase(String name) {
        return split(name)
                .map(String::toLowerCase)
                .map( it -> Character.toUpperCase(it.charAt(0)) + it.substring(1))
                .collect(Collectors.joining());
    }

    @Override
    public String asLowerCamelCase(String name) {
        String upperCamelCase = asUpperCamelCase(name);
        return Character.toLowerCase(upperCamelCase.charAt(0)) + upperCamelCase.substring(1);
    }

    @Override
    public Names prefix(String prefix) {
        List<String> prefixes = new ArrayList<>(this.prefixes);
        prefixes.add(prefix);
        return new NameImpl(prefixes, new ArrayList<>(suffixes));
    }

    @Override
    public Names suffix(String suffix) {
        List<String> suffixes = new ArrayList<>(this.suffixes);
        suffixes.add(suffix);
        return new NameImpl(new ArrayList<>(prefixes), new ArrayList<>(suffixes));
    }
}
