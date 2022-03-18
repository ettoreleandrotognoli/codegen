package com.github.ettoreleandrotognoli.codegen.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.ettoreleandrotognoli.codegen.api.Codegen;
import com.github.ettoreleandrotognoli.codegen.api.Context;
import com.github.ettoreleandrotognoli.codegen.data.plugin.DataCodegen;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import lombok.AllArgsConstructor;

import java.util.Optional;

@AllArgsConstructor
public class JacksonCodegen implements Codegen {

    private ClassName baseInterface;
    private JacksonSpec spec;

    public static JacksonCodegen from(JacksonSpec spec) {
        ClassName baseInterface = ClassName.get(spec.getPack(), spec.getName());
        return new JacksonCodegen(
                baseInterface,
                spec
        );
    }

    @Override
    public void prepare(Context.Builder builder) {

    }

    @Override
    public void generate(Context context) {
        Optional<DataCodegen> dataCodegen = context.getCodegen(DataCodegen.class)
                .filter(it -> baseInterface.equals(it.getBaseInterface()))
                .findAny();
        if (dataCodegen.isEmpty()) {
            return;
        }
        DataCodegen data = dataCodegen.get();
        TypeSpec.Builder builder = context.getBuilder(baseInterface);
        AnnotationSpec annotation = AnnotationSpec.builder(JsonDeserialize.class)
                .addMember("as", "$T.class", data.getDtoClass())
                .build();
        builder.addAnnotation(annotation);
    }
}
