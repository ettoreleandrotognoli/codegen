package com.github.ettoreleandrotognoli.codegen.core

import com.github.ettoreleandrotognoli.codegen.api.CodeSpec
import kotlinx.serialization.Serializable

@Serializable
data class CodegenSpec(
        val codegen: List<String>
) : CodeSpec {

}