package com.github.ettoreleandrotognoli.codegen.generator.jackson

import kotlinx.serialization.Serializable

@Serializable
class JacksonSpec(
        val deserializeAnnotation: String = "com.fasterxml.jackson.databind.annotation.JsonDeserialize"
)