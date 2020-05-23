package com.github.ettoreleandrotognoli.codegen.core

import com.github.ettoreleandrotognoli.codegen.Sample
import com.github.ettoreleandrotognoli.codegen.generator.data.DataClassRawSpec
import com.github.ettoreleandrotognoli.codegen.generator.data.DataClassGenerator
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class DefaultCodeGeneratorResolverTest {

    @Test
    fun `Test find DataClassGenerator`() {
        val generator = Sample.CodeGeneratorResolver.DEFAULT.resolve(DataClassRawSpec::class)
        generator.filterIsInstance(DataClassGenerator::class.java)
                .count()
                .let {
                    Assertions.assertThat(it).isEqualTo(1)
                }

    }
}