package com.github.ettoreleandrotognoli.codegen.api;

import com.squareup.javapoet.TypeName;

import java.io.File;
import java.util.stream.Stream;

public interface Context {

    TypeName resolveType(String name);

    Stream<Codegen> getCodegen();

    File resolveFile(String path);

    interface Builder {

        void register(Codegen codegen);

        Context build();

    }
}
