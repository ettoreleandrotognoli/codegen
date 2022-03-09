package com.github.ettoreleandrotognoli.codegen.data.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Data
public class DataSpec {

    @Data
    static class DataField {
        private String name;
        private String type;

        public String getMethod() {
            return "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }

        public String setMethod() {
            return "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }

    }

    private String name;
    @JsonProperty("package")
    private String pack;
    private Optional<String> parent = Optional.empty();
    private List<String> interfaces = Collections.emptyList();
    private List<DataField> fields = Collections.emptyList();
    private String output;
}
