package com.github.ettoreleandrotognoli.codegen.loader;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.ettoreleandrotognoli.codegen.api.Codegen;
import com.github.ettoreleandrotognoli.codegen.api.CodegenFactory;
import com.github.ettoreleandrotognoli.codegen.api.Context;
import com.github.ettoreleandrotognoli.codegen.api.impl.CodegenContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collector;

public class ReaderTest {


    private CodegenReader reader;

    @BeforeEach
    public void setup() {
        YAMLFactory yamlFactory = new YAMLFactory();
        ObjectMapper objectMapper = new ObjectMapper(yamlFactory);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new Jdk8Module());
        FactoryLoader factoryLoader = new FactoryLoader();
        this.reader = new CodegenReader(objectMapper, factoryLoader.codegenFactoryMap(), new CodegenFactory.Unknown());
    }


    @Test
    public void read() throws IOException {
        Context.Builder contextBuilder = new CodegenContext.Builder(new File("."));
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("codegen.yaml");
        List<Codegen> codegenList = new LinkedList<>();
        reader.read(resourceAsStream).subscribe(codegenList::add);
        for (Codegen codegen : codegenList) {
            contextBuilder.register(codegen);
        }
        for (Codegen codegen : codegenList) {
            codegen.prepare(contextBuilder);
        }
        Context context = contextBuilder.build();
        context.getCodegen().forEach( codegen -> {
            codegen.generate(context);
        });
    }

}