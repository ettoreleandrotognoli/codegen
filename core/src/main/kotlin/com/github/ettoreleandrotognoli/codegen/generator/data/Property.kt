package com.github.ettoreleandrotognoli.codegen.generator.data

import kotlinx.serialization.Serializable

@Serializable
data class Property(
        val name: String,
        val type: String
)