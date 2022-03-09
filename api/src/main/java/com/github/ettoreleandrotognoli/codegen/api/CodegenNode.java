package com.github.ettoreleandrotognoli.codegen.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodegenNode {

    private String name;
    private List<String> codegen;
    private String output;

}
