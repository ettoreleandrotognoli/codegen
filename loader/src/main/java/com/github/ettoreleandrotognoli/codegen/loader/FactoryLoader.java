package com.github.ettoreleandrotognoli.codegen.loader;

import com.github.ettoreleandrotognoli.codegen.api.CodegenFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Stream;

public class FactoryLoader {


    public Stream<CodegenFactory<?, ?>> codegenFactoryStream() {
        return ServiceLoader.load(CodegenFactory.class).stream().map(
                ServiceLoader.Provider::get
        );
    }

    public Map<String, CodegenFactory<?, ?>> codegenFactoryMap() {
        Map<String, CodegenFactory<?, ?>> map = new HashMap<>();
        for (Iterator<CodegenFactory<?, ?>> it = codegenFactoryStream().iterator(); it.hasNext(); ) {
            CodegenFactory<?, ?> codegenFactory = it.next();
            map.put(codegenFactory.getClass().getCanonicalName(), codegenFactory);
            for (String alias : codegenFactory.aliases()) {
                map.put(alias, codegenFactory);
            }
        }
        return map;
    }

    public static void main(String... args) {
        new FactoryLoader().codegenFactoryStream()
                .forEach(System.out::println);
    }
}
