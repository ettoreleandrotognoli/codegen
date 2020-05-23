package com.github.ettoreleandrotognoli.codegen.generator

import com.github.ettoreleandrotognoli.codegen.Sample
import com.github.ettoreleandrotognoli.codegen.core.CodegenContext
import com.github.ettoreleandrotognoli.codegen.generator.data.DataClassGenerator
import com.squareup.javapoet.ClassName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertNotNull


class DataClassSpecGeneratorTest {


    @field:TempDir
    lateinit var basePath: File

    @Test
    fun `Should create a TypeSpec$Builder for the class`() {
        val generator = DataClassGenerator()
        val context = CodegenContext()
        generator.generate(context, Sample.DataClass.EXAMPLE_NAME);
        val spec = Sample.DataClass.EXAMPLE_NAME
        val builder = context.getTypeSpecBuilder(ClassName.get(spec.packageName, spec.name))
        assertNotNull(builder)
    }

}