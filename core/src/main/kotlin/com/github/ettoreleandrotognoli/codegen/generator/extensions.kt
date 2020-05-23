package com.github.ettoreleandrotognoli.codegen.generator

import com.github.ettoreleandrotognoli.codegen.generator.data.DataClassRawSpec
import com.github.ettoreleandrotognoli.codegen.generator.data.DataClassSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import java.util.*
import java.util.regex.Pattern
import javax.lang.model.element.Modifier

val CAMEL_CASE_REGEX = Pattern.compile("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")


fun String.asConstName(): String {
    return CAMEL_CASE_REGEX.split(this).joinToString(separator = "_") { it.toUpperCase() }
}


fun ClassName.isNested(): Boolean {
    return (this.packageName() + "." + this.simpleName()) != this.canonicalName()
}


fun makeHashCode(codeSpec: DataClassRawSpec, fields: List<String>): MethodSpec {
    val methodSpecBuilder = MethodSpec.methodBuilder("hashCode")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .returns(TypeName.INT)
    methodSpecBuilder.addCode("return \$T.hash(${fields.joinToString(separator = ", ")});\n", Objects::class.java);
    return methodSpecBuilder.build()
}

fun makeEquals(codeSpec: DataClassSpec, fields: List<String>): MethodSpec {
    val methodSpecBuilder = MethodSpec.methodBuilder("equals")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addParameter(Object::class.java, "obj")
            .returns(TypeName.BOOLEAN)
    methodSpecBuilder.addCode("if (this == obj) return true;\n");
    methodSpecBuilder.addCode("if (obj == null) return false;\n");
    methodSpecBuilder.addCode("if (!(obj instanceof \$T)) return false;\n", codeSpec.type)
    methodSpecBuilder.addCode("$1T other = ($1T) obj;\n", codeSpec.type)
    fields.forEach {
        methodSpecBuilder.addCode("if(!\$T.equals(\$L, \$L.\$L())) return false;\n", Objects::class.java, it, "other", codeSpec.propertyGetMethodName[it]!!)
    }
    methodSpecBuilder.addCode("return true;\n")
    return methodSpecBuilder.build()
}