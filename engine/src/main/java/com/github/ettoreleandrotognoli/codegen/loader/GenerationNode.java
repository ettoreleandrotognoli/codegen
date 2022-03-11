package com.github.ettoreleandrotognoli.codegen.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.ettoreleandrotognoli.codegen.api.CodegenNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class GenerationNode {
    private final CodegenNode codegen;
    private final JsonNode source;
}
