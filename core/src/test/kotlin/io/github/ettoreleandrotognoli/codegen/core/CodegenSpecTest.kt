package io.github.ettoreleandrotognoli.codegen.core

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import io.github.ettoreleandrotognoli.codegen.Sample
import io.github.ettoreleandrotognoli.codegen.asString
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CodegenSpecTest {

    @Test
    fun `Load from YML`() {
        val yml = javaClass.classLoader
                .getResourceAsStream("data-class.yml")
                .asString()
        val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))
        val codegenSpec = yaml.parse(CodegenSpec.serializer(), yml)
        assertEquals(Sample.Codegen.EXAMPLE_NAME, codegenSpec)
    }

}