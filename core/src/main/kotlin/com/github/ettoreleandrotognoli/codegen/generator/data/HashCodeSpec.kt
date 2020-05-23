package com.github.ettoreleandrotognoli.codegen.generator.data

import kotlinx.serialization.Serializable

@Serializable
data class HashCodeSpec(
        val enable: Boolean = true,
        val fields: List<String>? = null
)