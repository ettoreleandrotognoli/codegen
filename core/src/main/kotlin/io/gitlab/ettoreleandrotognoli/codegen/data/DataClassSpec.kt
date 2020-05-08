package io.gitlab.ettoreleandrotognoli.codegen.data

import io.gitlab.ettoreleandrotognoli.codegen.CodeSpec
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class DataClassSpec(
        val name: String,
        val packageName: String,
        val properties: List<Property> = Collections.emptyList()
) : CodeSpec {


}