package com.github.ettoreleandrotognoli.codegen.jackson;

import com.github.ettoreleandrotognoli.codegen.api.CodegenFactory;
import com.google.auto.service.AutoService;

@AutoService(CodegenFactory.class)
public class JacksonFactory implements CodegenFactory<JacksonSpec, JacksonCodegen> {

    @Override
    public String[] aliases() {
        return new String[]{"Jackson"};
    }

    @Override
    public Class<? extends JacksonSpec> specClass() {
        return JacksonSpec.class;
    }

    @Override
    public Class<? extends JacksonCodegen> genClass() {
        return JacksonCodegen.class;
    }

    @Override
    public JacksonCodegen create(JacksonSpec spec) {
        return JacksonCodegen.from(spec);
    }
}
