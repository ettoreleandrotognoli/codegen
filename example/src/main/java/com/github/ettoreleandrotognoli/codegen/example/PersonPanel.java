package com.github.ettoreleandrotognoli.codegen.example;

import com.github.ettoreleandrotognoli.binding.BindBean;
import com.github.ettoreleandrotognoli.binding.LinkProperty;
import com.github.ettoreleandrotognoli.binding.ModelHolder;
import com.github.ettoreleandrotognoli.binding.ModelSupport;
import org.etto.Person;
import org.jdesktop.beansbinding.AutoBinding;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.util.Collections;

public class PersonPanel extends JPanel implements ModelHolder<Person.Observable> {

    final private ModelSupport<Person.Observable> modelSupport = new ModelSupport<>(this, this::firePropertyChange);

    public Person.Observable getModel() {
        return modelSupport.getModel();
    }

    public void setModel(Person.Observable model) {
        modelSupport.setModel(model);
    }

    @BindBean(modelProperty = Person.PROP_NAME, componentProperty = "text", updateStrategy = AutoBinding.UpdateStrategy.READ_WRITE)
    private JTextField tfName;

    private void init() {
        tfName = new JTextField(20);
        setLayout(new BorderLayout());
        add(tfName);
    }

    @LinkProperty(Person.PROP_NAME)
    public void nameChanged(PropertyChangeEvent event) {
        System.err.println(event);
    }

    public PersonPanel() {
        init();
    }

    public static void main(String... args) {
        Person.Observable model = new Person.DTO()
                .contacts(Collections.emptyList())
                .labels(Collections.emptyList())
                .asObservable();
        PersonPanel personPanel = new PersonPanel();
        personPanel.setModel(model);
        JFrame jFrame = new JFrame();
        jFrame.add(personPanel);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.pack();
        jFrame.setVisible(true);
    }
}
