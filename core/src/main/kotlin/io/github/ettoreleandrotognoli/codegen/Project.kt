package io.github.ettoreleandrotognoli.codegen

import io.github.ettoreleandrotognoli.codegen.data.DataClass
import java.io.File

@DataClass
interface Project {

    val basePath: File

    val targetPath: File
        get() {
            return File(basePath, "target")
        }

    val generatedSourcePath: File
        get() {
            return File(targetPath, "generated-source")
        }

    class DTO(
            override val basePath: File
    ) : Project

}