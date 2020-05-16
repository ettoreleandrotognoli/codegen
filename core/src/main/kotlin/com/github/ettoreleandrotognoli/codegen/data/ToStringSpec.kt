package com.github.ettoreleandrotognoli.codegen.data


import kotlinx.serialization.Serializable

@Serializable
data class ToStringSpec(
        val enable: Boolean = true,
        val pattern: String? = null
)