package com.github.ettoreleandrotognoli.codegen.api.impl;

import com.github.ettoreleandrotognoli.codegen.api.Codegen;
import com.github.ettoreleandrotognoli.codegen.api.Context;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import lombok.AllArgsConstructor;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
public class CodegenContext implements Context {

    private File baseDir;
    private Map<String, TypeName> types;
    private List<Codegen> codegenList;


    public static class Builder implements Context.Builder {
        private File baseDir;
        private Map<String, TypeName> types = new HashMap<>();
        private List<Codegen> codegenList = new LinkedList<>();

        public Builder(File baseDir) {
            this.baseDir = baseDir;
        }

        @Override
        public void register(Codegen codegen) {
            codegenList.add(codegen);
        }

        @Override
        public CodegenContext build() {
            return new CodegenContext(
                    baseDir,
                    Collections.unmodifiableMap(types),
                    Collections.unmodifiableList(codegenList)
            );
        }

    }

    @Override
    public Stream<Codegen> getCodegen() {
        return codegenList.stream();
    }

    @Override
    public TypeName resolveType(String name) {
        if(name.contains("<")) {
            return resolveGenericType(name);
        }
        if (types.containsKey(name)) {
            return types.get(name);
        }
        return ClassName.bestGuess(name);
    }

    public TypeName resolveGenericType(String name) {
        String genericType = name.substring(0, name.indexOf("<"));
        String[] typeParameters = name.substring(name.indexOf("<") + 1, name.lastIndexOf(">")).split(",");
        TypeName[] typeNames = Arrays.stream(typeParameters).map(this::resolveType).collect(Collectors.toList()).toArray(TypeName[]::new);
        ClassName className = ClassName.bestGuess(genericType);
        return ParameterizedTypeName.get(className, typeNames);
    }

    @Override
    public File resolveFile(String path) {
        return new File(baseDir, path);
    }
}
