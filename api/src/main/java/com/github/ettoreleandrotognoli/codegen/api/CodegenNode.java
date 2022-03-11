package com.github.ettoreleandrotognoli.codegen.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodegenNode {

    private String name;
    private Map<String, Optional<Object>> codegen;
}
