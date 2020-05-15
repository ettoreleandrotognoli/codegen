package io.github.ettoreleandrotognoli.codegen.core

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.nhaarman.mockito_kotlin.times
import io.github.ettoreleandrotognoli.codegen.KMockito
import io.github.ettoreleandrotognoli.codegen.SnakeYaml
import io.github.ettoreleandrotognoli.codegen.api.CodeGenerator
import io.github.ettoreleandrotognoli.codegen.api.Project
import kotlinx.serialization.ImplicitReflectionSerializer
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.yaml.snakeyaml.DumperOptions
import java.io.File

class CodegenEngineTest {



    @ImplicitReflectionSerializer
    @Test
    fun `getInstance`() {
        CodegenEngine
                .getInstance()
                .process(Project.DTO(File("/home/ettore/Code/codegen")), listOf(javaClass.classLoader.getResourceAsStream("main.yml")).stream())
    }
}