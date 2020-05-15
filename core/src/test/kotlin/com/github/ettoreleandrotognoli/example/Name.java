package com.github.ettoreleandrotognoli.example;

import java.lang.Override;

public interface Name {
    String getValue();

    interface Mutable extends Name {
        void setValue(String value);
    }

    class DTO implements Name.Mutable {
        private String value;

        @Override
        public String getValue() {
            return this.value;
        }

        @Override
        public void setValue(String value) {
            this.value = value;
        }

        public Name.DTO copy(Name source) {
            this.value = source.getValue();
            return this;
        }

        public Name.DTO clone() {
            return new Name.DTO().copy(this);
        }
    }

    class Builder {
        private final Name.DTO prototype = new Name.DTO();

        public Name.DTO build() {
            return this.prototype.clone();
        }

        public Name.Builder setValue(String value) {
            this.prototype.setValue(value);
            return this;
        }
    }
}
