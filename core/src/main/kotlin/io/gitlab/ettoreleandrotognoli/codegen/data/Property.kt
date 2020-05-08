package io.gitlab.ettoreleandrotognoli.codegen.data

import kotlinx.serialization.Serializable

@Serializable
data class Property(
        val name: String,
        val type: String
)