package com.github.ettoreleandrotognoli.codegen.generator.entity

import com.github.ettoreleandrotognoli.codegen.api.RawCodeSpec
import kotlinx.serialization.Serializable
import java.util.*


@Serializable
data class EntityRawOptions(
        val extends: String = "Object",
        val implements: List<String> = Collections.emptyList()
)


@Serializable
data class EntityRawSpec(
        val name: String,
        val packageName: String,
        val primaryKey: List<String> = listOf("id"),
        val entity: EntityRawOptions = EntityRawOptions()
) : RawCodeSpec