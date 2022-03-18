package com.github.ettoreleandrotognoli.codegen.example;

import org.etto.Contact;
import org.etto.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PersonTest {

    private Person person;

    @BeforeEach
    public void setup() {
        person = new Person.DTO()
                .name("ettore")
                .preferredContact(new Contact.DTO().type("email"))
                .asImmutable();
    }

    @Test
    void testNamePredicateStartsWith() {
        Predicate<Person> predicate = Person.name()
                .startsWith("e");
        assertThat(predicate.test(person)).isTrue();
    }

    @Test
    void testNameEqualsTo() {
        Predicate<Person> predicate = Person.name()
                .equalsTo("ettore");
        assertThat(predicate.test(person)).isTrue();
    }

    @Test
    void testParentIsNull() {
        Predicate<Person> predicate = Person.parent()
                .isNull();
        assertThat(predicate.test(person)).isTrue();
    }

    @Test
    void testParentIsNotNull() {
        Predicate<Person> predicate = Person.parent()
                .isNotNull();
        assertThat(predicate.test(person)).isFalse();
    }

    @Test
    void testPreferredContactType() {
        Predicate<Person> predicate = Person.preferredContact()
                .type()
                .equalsTo("email");
        assertThat(predicate.test(person)).isTrue();
    }


    @Test
    void testNullParent() {
        Predicate<Person> predicate = Person.parent()
                .name()
                .equalsTo("null parent");
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> {
            predicate.test(person);
        });
    }

}
