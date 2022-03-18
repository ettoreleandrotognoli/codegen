package com.github.ettoreleandrotognoli.codegen.loader;

import com.github.ettoreleandrotognoli.codegen.api.Codegen;
import com.github.ettoreleandrotognoli.codegen.api.Context;
import com.github.ettoreleandrotognoli.codegen.api.impl.CodegenContext;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Builder
public class Engine implements Runnable {




    private Path sourcesPath;
    private Path outputPath;
    private Map<String, Class<?>> factories;

    @SneakyThrows
    @Override
    public void run() {
        List<Codegen> codegenList = new LinkedList<>();
        CodegenReader reader = CodegenReader.createDefault();
        for (Path file : Files.list(sourcesPath).collect(Collectors.toList())) {
            try (InputStream inputStream = new FileInputStream(file.toFile())) {
                reader.read(inputStream).subscribe(codegenList::add);
            }
        }
        Context.Builder contextBuilder = new CodegenContext.Builder(factories);
        for (Codegen codegen : codegenList) {
            contextBuilder.register(codegen);
        }
        for (Codegen codegen : codegenList) {
            codegen.prepare(contextBuilder);
        }
        Context context = contextBuilder.build();
        for (Codegen codegen : codegenList) {
            codegen.generate(context);
        }
        context.getBuilders()
                .forEach(entry -> {
                    ClassName className = entry.getKey();
                    TypeSpec.Builder classBuilder = entry.getValue();
                    JavaFile.Builder fileBuilder = JavaFile.builder(className.packageName(), classBuilder.build());
                    JavaFile javaFile = fileBuilder.skipJavaLangImports(true).build();
                    try {
                        javaFile.writeTo(outputPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
}
