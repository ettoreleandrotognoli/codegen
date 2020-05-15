package io.github.ettoreleandrotognoli.codegen.api

import java.io.File

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