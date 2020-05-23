package com.github.ettoreleandrotognoli.codegen.generator.data

import com.github.ettoreleandrotognoli.codegen.api.RawCodeSpec
import com.github.ettoreleandrotognoli.codegen.generator.copy.CopySpec
import com.github.ettoreleandrotognoli.codegen.generator.jackson.JacksonSpec
import com.github.ettoreleandrotognoli.codegen.generator.jdesktop.ObservableRawSpec
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class DataClassRawSpec(
        val name: String,
        val packageName: String,
        val properties: List<Property> = Collections.emptyList(),
        val extends: String = "Object",
        val implements: List<String> = Collections.emptyList(),
        val toString: ToStringSpec = ToStringSpec(),
        val equals: EqualsSpec = EqualsSpec(),
        val hashCode: HashCodeSpec = HashCodeSpec(),
        val copy: CopySpec = CopySpec(),
        val jackson: JacksonSpec? = null
) : RawCodeSpec {

}