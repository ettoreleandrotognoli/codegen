package com.github.ettoreleandrotognoli.codegen.observable.properties;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.ettoreleandrotognoli.codegen.api.Named;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Data
public class ObservablePropertiesSpec {
    @Data
    static class ObservableField implements Named {
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
    private List<ObservableField> fields = Collections.emptyList();
    private List<String> interfaces = Collections.emptyList();
    private Optional<String> parent = Optional.empty();
}
