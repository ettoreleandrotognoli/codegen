package com.github.ettoreleandrotognoli.codegen.example;

import org.etto.Article;
import org.etto.Author;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PredicateArticleTest {

    @Test
    public void testTitleIsNull() {
        Article.DTO article = new Article.DTO();
        Predicate<Article> predicate = Article.title().isNull();
        assertThat(predicate.test(article)).isTrue();
        article.setTitle("title");
        assertThat(predicate.test(article)).isFalse();
    }

    @Test
    public void testTitleIsEqual() {
        Article.DTO article = new Article.DTO();
        Predicate<Article> predicate = Article.title().equalsTo("title");
        assertThat(predicate.test(article)).isFalse();
        article.setTitle("title");
        assertThat(predicate.test(article)).isTrue();
    }

    @Test
    public void testTitleIsEmpty() {
        Article.DTO article = new Article.DTO().title("fuu");
        Predicate<Article> predicate = Article.title().isEmpty();
        assertThat(predicate.test(article)).isFalse();
        article.setTitle("");
        assertThat(predicate.test(article)).isTrue();
    }

    @Test
    public void testTitleIsBlank() {
        Article.DTO article = new Article.DTO().title("fuu");
        Predicate<Article> predicate = Article.title().isBlank();
        assertThat(predicate.test(article)).isFalse();
        article.setTitle(" \n\t");
        assertThat(predicate.test(article)).isTrue();
    }

    @Test
    public void testTitleCharAt() {
        Article.DTO article = new Article.DTO().title("fuu");
        Predicate<Article> predicate = Article.title().charAt(2).equalsTo("r");
        assertThat(predicate.test(article)).isFalse();
        article.setTitle("bar");
        assertThat(predicate.test(article)).isTrue();
    }

    @Test
    public void testTitleLength() {
        Article.DTO article = new Article.DTO().title("fuu");
        Predicate<Article> predicate = Article.title().length().equalsTo(0);
        assertThat(predicate.test(article)).isFalse();
        article.setTitle("");
        assertThat(predicate.test(article)).isTrue();
    }

    @Test
    public void testAuthorsIsEmpty() {
        Article.DTO article = new Article.DTO().authors(Collections.emptyList());
        Predicate<Article> predicate = Article.authors().isEmpty();
        assertThat(predicate.test(article)).isTrue();
        article.setAuthors(List.of(new Author.DTO()));
        assertThat(predicate.test(article)).isFalse();
    }

    @Test
    public void testAuthorsElementAtMatchesWithAuthorNameEqualsTo() {
        Article.DTO article = new Article.DTO().authors(Collections.emptyList());
        Predicate<Article> predicate = Article.authors().elementAt(0).matchesWith(Author.name().equalsTo("bar"));
        assertThrows(IndexOutOfBoundsException.class, () -> {
            predicate.test(article);
        });
        article.setAuthors(List.of(new Author.DTO().name("fuu")));
        assertThat(predicate.test(article)).isFalse();
        article.setAuthors(List.of(new Author.DTO().name("bar")));
        assertThat(predicate.test(article)).isTrue();
    }

    @Test
    public void testAuthorsContains() {
        Article.DTO article = new Article.DTO().authors(Collections.emptyList());
        Predicate<Article> predicate = Article.authors().contains(Author.name().equalsTo("bar"));
        assertThat(predicate.test(article)).isFalse();
        article.setAuthors(List.of(new Author.DTO().name("fuu")));
        assertThat(predicate.test(article)).isFalse();
        article.setAuthors(List.of(new Author.DTO().name("bar")));
        assertThat(predicate.test(article)).isTrue();
    }

    @Test
    public void testAuthorsSize() {
        Article.DTO article = new Article.DTO().authors(Collections.emptyList());
        Predicate<Article> predicate = Article.authors().size().equalsTo(0);
        assertThat(predicate.test(article)).isTrue();
    }
}
