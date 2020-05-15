package com.github.ettoreleandrotognoli.codegen.data

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.github.ettoreleandrotognoli.codegen.Sample
import com.github.ettoreleandrotognoli.codegen.asString
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DataClassSpecTest {


    @Test
    fun `Load from YML`() {
        val yml = javaClass.classLoader
                .getResourceAsStream("data-class.yml")
                .asString()
        val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))
        val dataClassSpec = yaml.parse(DataClassSpec.serializer(), yml)
        assertEquals(Sample.DataClass.EXAMPLE_NAME, dataClassSpec)
    }

}