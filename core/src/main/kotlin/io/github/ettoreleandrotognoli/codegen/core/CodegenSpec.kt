package io.github.ettoreleandrotognoli.codegen.core

import io.github.ettoreleandrotognoli.codegen.api.CodeSpec
import kotlinx.serialization.Serializable

@Serializable
data class CodegenSpec(
        val codegen: List<String>
) : CodeSpec {

}