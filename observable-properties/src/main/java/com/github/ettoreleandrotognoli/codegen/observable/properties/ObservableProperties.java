package com.github.ettoreleandrotognoli.codegen.observable.properties;

import java.beans.PropertyChangeListener;

public interface ObservableProperties {

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);

    PropertyChangeListener[] getPropertyChangeListeners();

    void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);

    void removePropertyChangeListener(String propertyName, PropertyChangeListener listener);

    PropertyChangeListener[] getPropertyChangeListeners(String propertyName);
}
