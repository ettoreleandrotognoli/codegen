package io.gitlab.ettoreleandrotognoli.codegen.java

import com.charleskorn.kaml.Yaml
import io.gitlab.ettoreleandrotognoli.codegen.Project
import io.gitlab.ettoreleandrotognoli.codegen.data.DataClassSpec
import io.gitlab.ettoreleandrotognoli.codegen.data.Property
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.InputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun InputStream.asString(): String {
    val stringBuilder = StringBuilder()
    val buffer = ByteArray(this.available())
    this.read(buffer)
    stringBuilder.append(String(buffer))
    return stringBuilder.toString()
}

class DataClassSpecGeneratorTest {


    val dataClassSpec = DataClassSpec("Name", "io.gitlab.ettoreleandrotognoli.example", listOf(Property(name = "value", type = "String")))

    @field:TempDir
    lateinit var basePath: File

    @Test
    fun `Create File`() {
        val project = Project.DTO(basePath)
        val generator = DataClassGenerator()
        generator.generate(project, dataClassSpec);
        val javaFile = File(project.generatedSourcePath, "io/gitlab/ettoreleandrotognoli/example/Name.java")
        assertTrue(javaFile.exists())
    }


    @Test
    fun `Load from YML`() {
        val yml = javaClass.classLoader
                .getResourceAsStream("data-class.yml")
                .asString()
        val dataClassSpec = Yaml.default.parse(DataClassSpec.serializer(), yml)
        assertEquals(this.dataClassSpec, dataClassSpec)
    }

}