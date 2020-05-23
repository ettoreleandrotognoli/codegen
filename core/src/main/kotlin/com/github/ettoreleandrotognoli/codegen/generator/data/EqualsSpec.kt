package com.github.ettoreleandrotognoli.codegen.generator.data

import kotlinx.serialization.Serializable

@Serializable
data class EqualsSpec(
        val enable: Boolean = true,
        val fields: List<String>? = null
)
