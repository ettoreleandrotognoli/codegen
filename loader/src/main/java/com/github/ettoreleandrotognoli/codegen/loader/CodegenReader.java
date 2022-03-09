package com.github.ettoreleandrotognoli.codegen.loader;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.ettoreleandrotognoli.codegen.api.Codegen;
import com.github.ettoreleandrotognoli.codegen.api.CodegenFactory;
import com.github.ettoreleandrotognoli.codegen.api.CodegenNode;
import io.reactivex.rxjava3.core.Observable;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class CodegenReader {


    private final ObjectMapper objectMapper;
    private final Map<String, CodegenFactory<?, ?>> codegenFactoryMap;
    private final CodegenFactory<?, ?> unknownFactory;

    public <T> Codegen resolve(CodegenFactory<T, ?> codegenFactory, JsonNode source) throws IOException {
        Class<? extends T> specClass = codegenFactory.specClass();
        T spec = objectMapper.readerFor(specClass).readValue(source);
        return codegenFactory.create(spec);
    }

    public List<Codegen> resolve(GenerationNode node) throws IOException {
        List<String> codegenTypes = node.getCodegen().getCodegen();
        List<Codegen> codegenList = new ArrayList<>(codegenTypes.size());
        for (String codegenType : codegenTypes) {
            CodegenFactory<?, ?> codegenFactory = codegenFactoryMap.getOrDefault(codegenType, unknownFactory);
            Codegen codegen = resolve(codegenFactory, node.getSource());
            codegenList.add(codegen);
        }
        return codegenList;
    }

    public Observable<Codegen> read(InputStream inputStream) {
        return Observable.create(emitter -> {
            MappingIterator<JsonNode> iterator = objectMapper
                    .readerFor(JsonNode.class)
                    .readValues(inputStream);
            JsonNode defaultValues = iterator.next();
            while (iterator.hasNext()) {
                JsonNode jsonNode = iterator.next();
                JsonNode merged = objectMapper
                        .readerForUpdating(defaultValues.deepCopy())
                        .readValue(jsonNode);
                CodegenNode codegenNode = objectMapper
                        .readerFor(CodegenNode.class)
                        .readValue(merged);
                GenerationNode node = new GenerationNode(codegenNode, merged);
                List<Codegen> resolve = resolve(node);
                resolve.forEach(emitter::onNext);
            }
        });
    }

    public static CodegenReader createDefault() {
        YAMLFactory yamlFactory = new YAMLFactory();
        ObjectMapper objectMapper = new ObjectMapper(yamlFactory);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new Jdk8Module());
        FactoryLoader factoryLoader = new FactoryLoader();
        return new CodegenReader(objectMapper, factoryLoader.codegenFactoryMap(), new CodegenFactory.Unknown());
    }

}
