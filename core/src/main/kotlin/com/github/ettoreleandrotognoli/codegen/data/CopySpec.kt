package com.github.ettoreleandrotognoli.codegen.data

import kotlinx.serialization.Serializable

@Serializable
data class CopySpec(
        val default: String = "deep"
)