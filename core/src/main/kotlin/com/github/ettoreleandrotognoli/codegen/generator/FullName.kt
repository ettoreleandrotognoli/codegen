package com.github.ettoreleandrotognoli.codegen.generator

import com.squareup.javapoet.ClassName

/**
 * workaround to avoid conflict names when there are inheritance between dataclasses
 */
fun ClassName.fullName(): ClassName {
    return ClassName.get(this.packageName(), this.simpleNames().joinToString(separator = "."))
}