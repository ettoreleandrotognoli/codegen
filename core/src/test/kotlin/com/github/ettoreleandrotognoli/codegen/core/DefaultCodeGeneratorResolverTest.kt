package com.github.ettoreleandrotognoli.codegen.core

import com.github.ettoreleandrotognoli.codegen.Sample
import com.github.ettoreleandrotognoli.codegen.data.DataClassSpec
import com.github.ettoreleandrotognoli.codegen.java.DataClassGenerator
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class DefaultCodeGeneratorResolverTest {

    @Test
    fun `Test find DataClassGenerator`() {
        val generator = Sample.CodeGeneratorResolver.DEFAULT.resolve(DataClassSpec::class)
        generator.filterIsInstance(DataClassGenerator::class.java)
                .count()
                .let {
                    Assertions.assertThat(it).isEqualTo(1)
                }

    }
}