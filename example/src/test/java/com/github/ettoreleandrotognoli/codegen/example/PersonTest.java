package com.github.ettoreleandrotognoli.codegen.example;

import org.etto.Person;
import org.junit.jupiter.api.Test;

public class PersonTest {

    @Test
    void test() {
        Person person = new Person.DTO().setName("ettore").setNickname("etto");
        person.getName();

    }

}
