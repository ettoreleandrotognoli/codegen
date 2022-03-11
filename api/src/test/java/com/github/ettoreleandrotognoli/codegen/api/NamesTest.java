package com.github.ettoreleandrotognoli.codegen.api;

import com.github.ettoreleandrotognoli.codegen.api.impl.NameImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NamesTest {

    private Names name = new NameImpl();

    @Test
    public void testUpperSnakeCase() {
        assertThat(name.asUpperSnakeCase("fieldName"))
                .isEqualTo("FIELD_NAME");
    }

    @Test
    public void testConst() {
        assertThat(name.asConst("fieldName"))
                .isEqualTo("FIELD_NAME");
    }

    @Test
    public void testFieldName() {
        assertThat(name.asFieldName("field.name"))
                .isEqualTo("fieldName");
    }

    @Test
    public void testLowerCamelCase() {
        assertThat(name.asLowerCamelCase("field_name"))
                .isEqualTo("fieldName");
    }
}
