package com.github.ettoreleandrotognoli.codegen.data.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.ettoreleandrotognoli.codegen.api.Named;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Data
public class DataSpec implements Named {

    @Data
    static class DataField implements Named {
        private String name;
        private String type;
        private Optional<String> factory = Optional.empty();
    }

    @Data
    static class ToString {
        private boolean enable = true;
        private Optional<String> pattern = Optional.empty();
    }

    private String name;
    @JsonProperty("package")
    private String pack;
    private List<String> interfaces = Collections.emptyList();
    private List<DataField> fields = Collections.emptyList();
    private Optional<ToString> toString = Optional.empty();;
}
