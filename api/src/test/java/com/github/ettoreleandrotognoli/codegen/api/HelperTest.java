package com.github.ettoreleandrotognoli.codegen.api;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.github.ettoreleandrotognoli.codegen.api.Helper.isList;
import static com.github.ettoreleandrotognoli.codegen.api.Helper.isMap;
import static org.assertj.core.api.Assertions.assertThat;

public class HelperTest {

    @Test
    public void testIsList() {
        TypeName stringList = ParameterizedTypeName.get(List.class, String.class);
        assertThat(isList(stringList)).isTrue();
    }


    @Test
    public void testIsMap() {
        TypeName stringList = ParameterizedTypeName.get(Map.class, String.class, String.class);
        assertThat(isMap(stringList)).isTrue();
    }
}
