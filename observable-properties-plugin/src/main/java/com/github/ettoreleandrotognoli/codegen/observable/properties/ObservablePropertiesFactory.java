package com.github.ettoreleandrotognoli.codegen.observable.properties;

import com.github.ettoreleandrotognoli.codegen.api.CodegenFactory;
import com.google.auto.service.AutoService;

@AutoService(CodegenFactory.class)
public class ObservablePropertiesFactory implements CodegenFactory<ObservablePropertiesSpec, ObservablePropertiesCodegen> {

    @Override
    public String[] aliases() {
        return new String[]{"ObservableProperties"};
    }

    @Override
    public Class<? extends ObservablePropertiesSpec> specClass() {
        return ObservablePropertiesSpec.class;
    }

    @Override
    public Class<? extends ObservablePropertiesCodegen> genClass() {
        return ObservablePropertiesCodegen.class;
    }

    @Override
    public ObservablePropertiesCodegen create(ObservablePropertiesSpec spec) {
        return ObservablePropertiesCodegen.from(spec);
    }
}
