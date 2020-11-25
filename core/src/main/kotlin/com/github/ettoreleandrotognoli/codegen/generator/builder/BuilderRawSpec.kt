package com.github.ettoreleandrotognoli.codegen.generator.builder

import com.github.ettoreleandrotognoli.codegen.api.RawCodeSpec
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class BuilderRawOptions(
        val extends: String = "Object",
        val implements: List<String> = Collections.emptyList(),
        val `for`: String? = null,
        val nested: Boolean = true
)

@Serializable
class BuilderRawSpec(
        val name: String? = null,
        val packageName: String? = null,
        val builder: BuilderRawOptions = BuilderRawOptions()
) : RawCodeSpec