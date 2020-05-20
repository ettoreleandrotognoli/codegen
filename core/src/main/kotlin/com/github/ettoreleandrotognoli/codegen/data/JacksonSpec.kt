package com.github.ettoreleandrotognoli.codegen.data

import kotlinx.serialization.Serializable

@Serializable
class JacksonSpec(
        val deserializeAnnotation: String = "com.fasterxml.jackson.databind.annotation.JsonDeserialize"
)