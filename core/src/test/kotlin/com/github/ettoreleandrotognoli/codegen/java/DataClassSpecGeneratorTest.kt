package com.github.ettoreleandrotognoli.codegen.java

import com.github.ettoreleandrotognoli.codegen.Sample
import com.github.ettoreleandrotognoli.codegen.api.Project
import com.github.ettoreleandrotognoli.codegen.core.CodegenContext
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
        val javaFile = File(project.generatedSourcePath, "com/github/ettoreleandrotognoli/example/Name.java")
        assertTrue(javaFile.exists())
    }

}