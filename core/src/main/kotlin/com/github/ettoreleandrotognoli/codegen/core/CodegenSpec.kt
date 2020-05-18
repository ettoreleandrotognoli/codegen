package com.github.ettoreleandrotognoli.codegen.core

import com.github.ettoreleandrotognoli.codegen.api.CodeSpec
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class CodegenSpec(
        val codegen: List<String>
) : CodeSpec {


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other !is CodegenSpec) return false
        if (!Objects.equals(codegen, other.codegen)) return false
        return true;
    }
}