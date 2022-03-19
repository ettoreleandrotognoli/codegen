package com.github.ettoreleandrotognoli.codegen.example;

import org.etto.Article;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.beans.PropertyChangeListener;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MockitoExtension.class)
public class ObservableArticleTest {

    @Test
    public void testListenProperty(@Mock PropertyChangeListener listener) {
        Article.Observable article = new Article.Observable();
        article.setTitle("old title");
        article.addPropertyChangeListener(listener);
        article.setTitle("new title");
        verify(listener, times(1)).propertyChange(
                argThat(event -> event.getPropertyName().equals(Article.PROP_TITLE) &&
                        event.getOldValue().equals("old title") &&
                        event.getNewValue().equals("new title") &&
                        event.getSource() == article)
        );
    }

    @Test
    public void testListenOneProperty(@Mock PropertyChangeListener listener) {
        Article.Observable article = new Article.Observable();
        article.setTitle("old title");
        article.setSummary("old summary");
        article.addPropertyChangeListener(Article.PROP_TITLE, listener);
        article.setTitle("new title");
        article.setSummary("new summary");
        verify(listener, times(1)).propertyChange(
                argThat(event -> event.getPropertyName().equals(Article.PROP_TITLE) &&
                        event.getOldValue().equals("old title") &&
                        event.getNewValue().equals("new title") &&
                        event.getSource() == article)
        );
    }

}
