package com.github.ettoreleandrotognoli.codegen.core

import kotlinx.serialization.ImplicitReflectionSerializer
import org.junit.jupiter.api.Test

class CodegenEngineTest {



    @ImplicitReflectionSerializer
    @Test
    fun `getInstance`() {
        CodegenEngine
                .getInstance()
    }
}