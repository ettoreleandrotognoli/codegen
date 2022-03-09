package com.github.ettoreleandrotognoli.codegen.api;

public interface CodegenFactory<T, C extends Codegen> {

    default String[] aliases() {
        return new String[]{};
    }

    Class<? extends T> specClass();

    Class<? extends C> genClass();

    C create(T spec);

    class Unknown implements CodegenFactory<Object, Codegen.Unknown> {

        @Override
        public Class<?> specClass() {
            return Object.class;
        }

        @Override
        public Codegen.Unknown create(Object spec) {
            return new Codegen.Unknown(spec);
        }

        @Override
        public Class<? extends Codegen.Unknown> genClass() {
            return Codegen.Unknown.class;
        }
    }

}
