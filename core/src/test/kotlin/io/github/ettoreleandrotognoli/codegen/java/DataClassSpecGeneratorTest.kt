package io.github.ettoreleandrotognoli.codegen.java

import io.github.ettoreleandrotognoli.codegen.Sample
import io.github.ettoreleandrotognoli.codegen.api.Project
import io.github.ettoreleandrotognoli.codegen.core.CodegenContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue


class DataClassSpecGeneratorTest {


    @field:TempDir
    lateinit var basePath: File

    @Test
    fun `Create File`() {
        val project = Project.DTO(basePath, File(basePath, "target"), File(basePath, "target/generated-sources/codegen"))
        val generator = DataClassGenerator()
        generator.generate(CodegenContext(project), Sample.DataClass.EXAMPLE_NAME);
        val javaFile = File(project.generatedSourcePath, "io/github/ettoreleandrotognoli/example/Name.java")
        assertTrue(javaFile.exists())
    }

}