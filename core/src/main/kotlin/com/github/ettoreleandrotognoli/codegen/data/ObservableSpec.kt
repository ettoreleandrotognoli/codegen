package com.github.ettoreleandrotognoli.codegen.data

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class ObservableSpec(
        val extends: String = "Object",
        val implements: List<String> = Collections.emptyList()
)