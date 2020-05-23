package com.github.ettoreleandrotognoli.codegen.generator.jdesktop

import com.github.ettoreleandrotognoli.codegen.api.RawCodeSpec
import kotlinx.serialization.Serializable
import java.util.*


@Serializable
data class ObservableRawOptions(
        val extends: String = "Object",
        val implements: List<String> = Collections.emptyList()
)

@Serializable
data class ObservableRawSpec(
        val name: String,
        val packageName: String,
        val observable: ObservableRawOptions = ObservableRawOptions()
) : RawCodeSpec