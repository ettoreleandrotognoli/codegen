package com.github.ettoreleandrotognoli.codegen.api

import com.squareup.javapoet.ClassName

abstract class BaseSpec(
        val type: ClassName
) {

    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseSpec

        if (type != other.type) return false

        return true
    }

}