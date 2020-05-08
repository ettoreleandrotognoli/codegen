package io.github.ettoreleandrotognoli.codegen.data

import kotlinx.serialization.Serializable

@Serializable
data class Property(
        val name: String,
        val type: String
)