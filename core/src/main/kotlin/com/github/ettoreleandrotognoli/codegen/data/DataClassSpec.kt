package com.github.ettoreleandrotognoli.codegen.data

import com.github.ettoreleandrotognoli.codegen.api.CodeSpec
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class DataClassSpec(
        val name: String,
        val packageName: String,
        val properties: List<Property> = Collections.emptyList(),
        val observable: ObservableSpec? = null,
        val extends: String = "Object",
        val implements: List<String> = Collections.emptyList(),
        val toString: ToStringSpec = ToStringSpec(),
        val equals: EqualsSpec = EqualsSpec(),
        val hashCode: HashCodeSpec = HashCodeSpec(),
        val copy: CopySpec = CopySpec()
) : CodeSpec {

}