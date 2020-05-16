package com.github.ettoreleandrotognoli.codegen.data

import kotlinx.serialization.Serializable

@Serializable
data class EqualsSpec(
        val enable: Boolean = true,
        val fields: List<String>? = null
)
