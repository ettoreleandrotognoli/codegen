package com.github.ettoreleandrotognoli.codegen.example;

import org.etto.Article;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

public class ArticleTest {

    @Test
    public void testDTOAsImmutableAreEquals() {
        Article.DTO dto = new Article.DTO();
        Article.Immutable immutable = dto.asImmutable();
        assertThat(immutable).isEqualTo(dto);
        assertThat(immutable).isNotSameAs(dto);
    }

    @Test
    public void testDTOAsMutableAreTheSame() {
        Article.DTO dto = new Article.DTO();
        Article.Mutable mutable = dto.asMutable();
        assertThat(mutable).isSameAs(dto);
    }

    @Test
    public void testDTOAsObservableAreEquals() {
        Article.DTO dto = new Article.DTO();
        Article.Observable observable = dto.asObservable();
        assertThat(observable).isEqualTo(dto);
        assertThat(observable).isNotSameAs(dto);
    }

    @Test
    public void testImmutableAsImmutableAreTheSame() {
        Article.Immutable immutableA = new Article.DTO()
                .asImmutable();
        Article.Immutable immutableB = immutableA.asImmutable();
        assertThat(immutableA).isSameAs(immutableB);
    }

    @Test
    public void testImmutableAsMutableAreEquals() {
        Article.Immutable immutable = new Article.DTO()
                .asImmutable();
        Article.Mutable mutable = immutable.asMutable();
        assertThat(immutable).isEqualTo(mutable);
        assertThat(immutable).isNotSameAs(mutable);
    }

    @Test
    public void testObservableAsObservableAreTheSame() {
        Article.Observable observableA = new Article.DTO()
                .asObservable();
        Article.Observable observableB = observableA.asObservable();
        assertThat(observableA).isSameAs(observableB);
    }


    @Test
    public void testTitleIsNull() {
        Article.DTO article = new Article.DTO();
        Predicate<Article> predicate = Article.title().isNull();
        assertThat(predicate.test(article)).isTrue();
        article.setTitle("title");
        assertThat(predicate.test(article)).isFalse();
    }
}
