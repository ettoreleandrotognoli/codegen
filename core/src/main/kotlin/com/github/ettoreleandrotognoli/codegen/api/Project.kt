package com.github.ettoreleandrotognoli.codegen.api

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
            override var basePath: File,
            override var targetPath: File,
            override var generatedSourcePath: File
    ) : Project

}