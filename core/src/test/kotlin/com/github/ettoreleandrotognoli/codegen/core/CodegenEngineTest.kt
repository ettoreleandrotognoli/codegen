package com.github.ettoreleandrotognoli.codegen.core

import com.github.ettoreleandrotognoli.codegen.api.Project
import kotlinx.serialization.ImplicitReflectionSerializer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import java.util.stream.Stream

@SpringBootTest
class CodegenEngineTest {


    @field:Autowired
    lateinit var codegenEngine: CodegenEngine

    @ImplicitReflectionSerializer
    @Test
    fun `getInstance`() {
        codegenEngine.processFiles(Project.DTO(
                File("./"),
                File("./target"),
                File("./target/generated-sources/codegen")
        ), listOf(File(javaClass.classLoader.getResource("main.yml").file)))
    }
}