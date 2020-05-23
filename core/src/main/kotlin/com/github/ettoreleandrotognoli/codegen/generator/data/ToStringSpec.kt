package com.github.ettoreleandrotognoli.codegen.generator.data


import kotlinx.serialization.Serializable

@Serializable
data class ToStringSpec(
        val enable: Boolean = true,
        val pattern: String? = null
)