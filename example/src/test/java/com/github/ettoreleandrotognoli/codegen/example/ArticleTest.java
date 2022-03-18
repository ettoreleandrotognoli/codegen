package com.github.ettoreleandrotognoli.codegen.example;

import org.etto.Article;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

public class ArticleTest {


    @Test
    public void testTitleIsNull() {
        Article.DTO article = new Article.DTO();
        Predicate<Article> predicate = Article.title().isNull();
        assertThat(predicate.test(article)).isTrue();
        article.setTitle("title");
        assertThat(predicate.test(article)).isFalse();
    }
}
