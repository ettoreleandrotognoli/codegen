package com.github.ettoreleandrotognoli.codegen.api;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

public interface Codegen {

    void prepare(Context.Builder builder);

    void generate(Context context);


    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    class Unknown implements Codegen {

        private final Object spec;

        @Override
        public void prepare(Context.Builder builder) {

        }

        @Override
        public void generate(Context context) {

        }
    }

}
