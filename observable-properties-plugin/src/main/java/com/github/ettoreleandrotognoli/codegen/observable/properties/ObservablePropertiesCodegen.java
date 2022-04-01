package com.github.ettoreleandrotognoli.codegen.observable.properties;

import com.github.ettoreleandrotognoli.codegen.api.Codegen;
import com.github.ettoreleandrotognoli.codegen.api.Context;
import com.github.ettoreleandrotognoli.codegen.api.Names;
import com.github.ettoreleandrotognoli.codegen.data.plugin.DataCodegen;
import com.squareup.javapoet.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jdesktop.observablecollections.ObservableCollections;

import javax.lang.model.element.Modifier;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;
import java.util.Optional;

import static com.github.ettoreleandrotognoli.codegen.api.Helper.*;

@Data
@AllArgsConstructor
public class ObservablePropertiesCodegen implements Codegen {

    static final String PROPERTY_CHANGE_SUPPORT = "propertyChangeSupport";
    private ObservablePropertiesSpec spec;
    private ClassName baseInterface;
    private ClassName baseClass;

    public static ObservablePropertiesCodegen from(ObservablePropertiesSpec spec) {
        ClassName baseInterface = ClassName.get(spec.getPack(), spec.getName());
        ClassName baseClass = ClassName.get(spec.getPack(), spec.getName(), "Observable");
        return new ObservablePropertiesCodegen(
                spec,
                baseInterface,
                baseClass
        );
    }

    public MethodSpec.Builder getSignature(Context context, ObservablePropertiesSpec.ObservableField field) {
        Names names = context.names();
        TypeName fieldType = context.resolveType(field.getType());
        return MethodSpec.methodBuilder(names.asGetMethod(field))
                .addModifiers(Modifier.PUBLIC)
                .returns(fieldType);
    }

    public MethodSpec.Builder getMethod(Context context, ObservablePropertiesSpec.ObservableField field) {
        return getSignature(context, field)
                .addAnnotation(Override.class)
                .addStatement("return $N", field.getName());
    }

    public MethodSpec.Builder setSignature(Context context, ObservablePropertiesSpec.ObservableField field) {
        Names names = context.names();
        TypeName fieldType = context.resolveType(field.getType());
        return MethodSpec.methodBuilder(names.asSetMethod(field))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(fieldType, field.getName()).build())
                .returns(TypeName.VOID);
    }

    public MethodSpec.Builder setMethod(Context context, ObservablePropertiesSpec.ObservableField field) {
        TypeName fieldType = context.resolveType(field.getType());
        Names oldNames = context.names().prefix("old");
        Names propNames = context.names().prefix("PROP");
        return setSignature(context, field)
                .addAnnotation(Override.class)
                .addStatement("$T $N = $N.$N", fieldType, oldNames.asLowerCamelCase(field.getName()), THIS, field.getName())
                .addStatement("$N.$N = $N", THIS, field.getName(), field.getName())
                .addStatement(
                        "if (!$T.equals($N, $N)) $N.firePropertyChange($T.$N,$N,$N)",
                        Objects.class,
                        oldNames.asLowerCamelCase(field.getName()), field.getName(),
                        PROPERTY_CHANGE_SUPPORT, baseInterface, propNames.asConst(field),
                        oldNames.asLowerCamelCase(field.getName()), field.getName()
                );
    }

    public MethodSpec.Builder replaceSignature(Context context, ObservablePropertiesSpec.ObservableField field, TypeName returnType) {
        Names names = context.names();
        TypeName fieldType = context.resolveType(field.getType());
        return MethodSpec.methodBuilder(names.asFieldName(field))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(fieldType, field.getName()).build())
                .returns(returnType);
    }

    public MethodSpec.Builder replaceMethod(Context context, ObservablePropertiesSpec.ObservableField field, TypeName returnType) {
        TypeName fieldType = context.resolveType(field.getType());
        Names names = context.names();
        return replaceSignature(context, field, returnType)
                .addAnnotation(Override.class)
                .addStatement("$N($N)", names.asSetMethod(field), names.asFieldName(field))
                .addStatement("return $N", THIS);
    }

    public MethodSpec.Builder asSuperObservableMethod(Context context) {
        return MethodSpec.methodBuilder("asObservable")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(baseClass)
                .addStatement("return $T.of($N)", baseClass, THIS);
    }

    public MethodSpec.Builder asObservableMethod(Context context) {
        return MethodSpec.methodBuilder("asObservable")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(baseClass)
                .addStatement("return $N", THIS);
    }


    public MethodSpec.Builder ofMethod(Context context) {
        Names name = context.names();
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(OF)
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .returns(baseClass)
                .addParameter(baseInterface, SOURCE);
        methodBuilder.addStatement("$T $N = new $T()", baseClass, name.asLowerCamelCase(spec.getName()), baseClass);
        for (ObservablePropertiesSpec.ObservableField field : spec.getFields()) {
            String fieldName = field.getName();
            TypeName fieldType = context.resolveType(field.getType());
            boolean hasAsObservable = context.getCodegen(ObservablePropertiesCodegen.class)
                    .map(ObservablePropertiesCodegen::getBaseInterface)
                    .anyMatch(fieldType::equals);
            if (hasAsObservable) {
                methodBuilder.addStatement(
                        "$N.$N = $T.ofNullable($N.$L()).map($T::asObservable).orElse(null)",
                        name.asLowerCamelCase(spec.getName()), fieldName,
                        Optional.class,
                        SOURCE, name.asGetMethod(field),
                        fieldType
                );
            } else if (isMap(fieldType)) {
                methodBuilder.addStatement(
                        "$N.$N = $T.ofNullable($N.$L()).map($T::observableMap).orElse(null)",
                        name.asLowerCamelCase(spec.getName()), fieldName,
                        Optional.class,
                        SOURCE, name.asGetMethod(field),
                        ObservableCollections.class
                );
            } else if (isList(fieldType)) {
                methodBuilder.addStatement(
                        "$N.$N = $T.ofNullable($N.$L()).map($T::observableList).orElse(null)",
                        name.asLowerCamelCase(spec.getName()), fieldName,
                        Optional.class,
                        SOURCE, name.asGetMethod(field),
                        ObservableCollections.class
                );
            } else {
                methodBuilder.addStatement("$N.$N = $N.$L()", name.asLowerCamelCase(spec.getName()), fieldName, SOURCE, name.asGetMethod(field));
            }
        }
        methodBuilder.addStatement("return $N", name.asLowerCamelCase(spec.getName()));
        return methodBuilder;
    }

    @Override
    public void prepare(Context.Builder builder) {

    }

    @Override
    public void generate(Context context) {
        TypeSpec.Builder baseInterfaceBuilder = context.getBuilder(baseInterface);
        TypeSpec.Builder observable = TypeSpec.classBuilder("Observable")
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .addSuperinterface(baseInterface.nestedClass("Mutable"))
                .addSuperinterface(ObservableProperties.class);
        spec.getInterfaces()
                .stream()
                .map(context::resolveType)
                .forEach(observable::addSuperinterface);
        spec.getParent()
                .map(context::resolveType)
                .ifPresent(observable::superclass);
        FieldSpec propertyChangeSupportField = FieldSpec.builder(PropertyChangeSupport.class, PROPERTY_CHANGE_SUPPORT)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.TRANSIENT)
                .initializer("new $T($N)", PropertyChangeSupport.class, THIS)
                .build();
        observable.addField(propertyChangeSupportField);
        for (ObservablePropertiesSpec.ObservableField field : spec.getFields()) {
            TypeName fieldType = context.resolveType(field.getType());
            FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, field.getName())
                    .addModifiers(Modifier.PRIVATE);
            field.getDefaultValue().ifPresent(value -> {
                if (isString(fieldType)) {
                    fieldBuilder.initializer("$S", value);
                } else {
                    fieldBuilder.initializer("$L", value);
                }
            });
            observable.addField(fieldBuilder.build());
        }
        for (ObservablePropertiesSpec.ObservableField field : spec.getFields()) {
            observable.addMethod(getMethod(context, field).build());
            observable.addMethod(setMethod(context, field).build());
            observable.addMethod(replaceMethod(context, field, baseClass).build());
        }
        observable.addMethod(MethodSpec.methodBuilder("addPropertyChangeListener")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID)
                .addParameter(PropertyChangeListener.class, "listener")
                .addStatement("$N.addPropertyChangeListener($N)", PROPERTY_CHANGE_SUPPORT, "listener")
                .build());
        observable.addMethod(MethodSpec.methodBuilder("removePropertyChangeListener")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID)
                .addParameter(PropertyChangeListener.class, "listener")
                .addStatement("$N.removePropertyChangeListener($N)", PROPERTY_CHANGE_SUPPORT, "listener")
                .build());
        observable.addMethod(MethodSpec.methodBuilder("addPropertyChangeListener")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID)
                .addParameter(String.class, "propertyName")
                .addParameter(PropertyChangeListener.class, "listener")
                .addStatement("$N.addPropertyChangeListener($N, $N)", PROPERTY_CHANGE_SUPPORT, "propertyName", "listener")
                .build());
        observable.addMethod(MethodSpec.methodBuilder("removePropertyChangeListener")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID)
                .addParameter(String.class, "propertyName")
                .addParameter(PropertyChangeListener.class, "listener")
                .addStatement("$N.removePropertyChangeListener($N, $N)", PROPERTY_CHANGE_SUPPORT, "propertyName", "listener")
                .build());
        observable.addMethod(MethodSpec.methodBuilder("getPropertyChangeListeners")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(PropertyChangeListener[].class)
                .addStatement("return $N.getPropertyChangeListeners()", PROPERTY_CHANGE_SUPPORT)
                .build());
        observable.addMethod(MethodSpec.methodBuilder("getPropertyChangeListeners")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(PropertyChangeListener[].class)
                .addParameter(String.class, "propertyName")
                .addStatement("return $N.getPropertyChangeListeners($N)", PROPERTY_CHANGE_SUPPORT, "propertyName")
                .build());
        observable.addMethod(asObservableMethod(context).build());
        observable.addMethod(ofMethod(context).build());

        Optional<DataCodegen> dataCodegen = context.getCodegen(DataCodegen.class)
                .filter(it -> it.getBaseInterface().equals(baseInterface))
                .findAny();
        dataCodegen.ifPresent(codegen -> {
            observable.addMethod(codegen.toStringMethod(context, baseClass).build());
            observable.addMethod(codegen.equalsMethod(context).build());
        });

        baseInterfaceBuilder.addMethod(asSuperObservableMethod(context).build());
        baseInterfaceBuilder.addType(observable.build());
    }
}
