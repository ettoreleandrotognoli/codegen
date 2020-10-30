package com.github.ettoreleandrotognoli.codegen.generator.builder

import com.github.ettoreleandrotognoli.codegen.api.RawCodeSpec
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class BuilderRawOptions(
        val extends: String = "Object",
        val implements: List<String> = Collections.emptyList()
)

@Serializable
class BuilderRawSpec(
        val name: String,
        val packageName: String,
        val builder: BuilderRawOptions = BuilderRawOptions()
) : RawCodeSpec