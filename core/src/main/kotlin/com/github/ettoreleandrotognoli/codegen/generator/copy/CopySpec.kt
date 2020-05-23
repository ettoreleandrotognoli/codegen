package com.github.ettoreleandrotognoli.codegen.generator.copy

import kotlinx.serialization.Serializable

@Serializable
data class CopySpec(
        val default: String = "deep"
)