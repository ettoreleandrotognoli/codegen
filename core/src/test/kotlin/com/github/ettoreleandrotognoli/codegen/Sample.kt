package com.github.ettoreleandrotognoli.codegen

import com.github.ettoreleandrotognoli.codegen.core.CodegenSpec
import com.github.ettoreleandrotognoli.codegen.core.DefaultCodeGeneratorResolver
import com.github.ettoreleandrotognoli.codegen.data.DataClassSpec
import com.github.ettoreleandrotognoli.codegen.data.Property
import com.github.ettoreleandrotognoli.codegen.java.DataClassGenerator

class Sample {

    class Codegen {
        companion object {
            val EXAMPLE_NAME = CodegenSpec(listOf("DataClass"))
        }
    }

    class DataClass {
        companion object {
            val EXAMPLE_NAME = DataClassSpec(
                    "Name",
                    "com.github.ettoreleandrotognoli.example",
                    listOf(
                            Property(name = "value", type = "String"),
                            Property(name = "enabled", type = "boolean"),
                            Property(name = "list", type = "java.util.List<String>")
                    )
            )
        }
    }

    class CodeGeneratorResolver {

        companion object {
            val DEFAULT = DefaultCodeGeneratorResolver(mapOf(
                    DataClassSpec::class to listOf(DataClassGenerator())
            ))
        }

    }


}