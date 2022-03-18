package com.github.ettoreleandrotognoli.codegen.predicate;

import com.github.ettoreleandrotognoli.codegen.api.CodegenFactory;
import com.google.auto.service.AutoService;

@AutoService(CodegenFactory.class)
public class PredicateFactory implements CodegenFactory<PredicateSpec, PredicateCodegen> {

    @Override
    public String[] aliases() {
        return new String[]{"Predicate"};
    }

    @Override
    public Class<? extends PredicateSpec> specClass() {
        return PredicateSpec.class;
    }

    @Override
    public Class<? extends PredicateCodegen> genClass() {
        return PredicateCodegen.class;
    }

    @Override
    public PredicateCodegen create(PredicateSpec spec) {
        return PredicateCodegen.from(spec);
    }
}
