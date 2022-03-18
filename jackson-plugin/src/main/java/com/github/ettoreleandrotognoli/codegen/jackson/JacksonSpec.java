package com.github.ettoreleandrotognoli.codegen.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JacksonSpec {
    private String name;
    @JsonProperty("package")
    private String pack;
}
