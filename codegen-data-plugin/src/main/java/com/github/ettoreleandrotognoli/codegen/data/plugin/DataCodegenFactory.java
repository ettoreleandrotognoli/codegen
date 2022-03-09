package com.github.ettoreleandrotognoli.codegen.data.plugin;

import com.github.ettoreleandrotognoli.codegen.api.CodegenFactory;
import com.google.auto.service.AutoService;

@AutoService(CodegenFactory.class)
public class DataCodegenFactory implements CodegenFactory<DataSpec, DataCodegen> {

    @Override
    public String[] aliases() {
        return new String[]{"DataClass"};
    }

    @Override
    public Class genClass() {
        return DataCodegen.class;
    }

    @Override
    public Class<? extends DataSpec> specClass() {
        return DataSpec.class;
    }

    @Override
    public DataCodegen create(DataSpec spec) {
        return DataCodegen.from(spec);
    }

}
