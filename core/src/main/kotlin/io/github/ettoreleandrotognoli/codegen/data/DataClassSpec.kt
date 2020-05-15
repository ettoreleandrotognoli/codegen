package io.github.ettoreleandrotognoli.codegen.data

import io.github.ettoreleandrotognoli.codegen.api.CodeSpec
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class DataClassSpec(
        val name: String,
        val packageName: String,
        val properties: List<Property> = Collections.emptyList(),
        val observable: Boolean = false,
        val extends: String = "Object",
        val implements: List<String> = Collections.emptyList()
) : CodeSpec {


}