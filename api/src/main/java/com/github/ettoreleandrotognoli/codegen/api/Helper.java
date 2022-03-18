package com.github.ettoreleandrotognoli.codegen.api;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class Helper {

    public static final String OF = "of";
    public static final String THIS = "this";
    public static final String SOURCE = "source";
    public static final String OTHER = "other";

    static final ClassName listClass = ClassName.get(List.class);
    static final ClassName mapClass = ClassName.get(Map.class);
    static final ClassName streamClass = ClassName.get(Stream.class);
    static final ClassName optionalClass = ClassName.get(Optional.class);

    public static boolean isImmutable(TypeName typeName) {
        if (typeName.isPrimitive()) {
            return true;
        }
        if (typeName.isBoxedPrimitive()) {
            return true;
        }
        if (Objects.equals(ClassName.get(String.class).toString(), typeName.toString())) {
            return true;
        }
        if(isStream(typeName) || isOptional(typeName)){
            return true;
        }
        return false;
    }

    public static boolean isString(TypeName typeName) {
        return ClassName.get(String.class).equals(typeName);
    }

    public static boolean isStream(TypeName typeName) {
        return typeName.toString().startsWith(streamClass.toString());
    }

    public static boolean isOptional(TypeName typeName) {
        return typeName.toString().startsWith(optionalClass.toString());
    }

    public static boolean isList(TypeName typeName) {
        return typeName.toString().startsWith(listClass.toString());
    }

    public static boolean isMap(TypeName typeName) {
        return typeName.toString().startsWith(mapClass.toString());
    }
}

