package io.github.ettoreleandrotognoli.codegen

import io.github.ettoreleandrotognoli.codegen.core.CodegenSpec
import io.github.ettoreleandrotognoli.codegen.core.DefaultCodeGeneratorResolver
import io.github.ettoreleandrotognoli.codegen.data.DataClassSpec
import io.github.ettoreleandrotognoli.codegen.data.Property
import io.github.ettoreleandrotognoli.codegen.java.DataClassGenerator

class Sample {

    class Codegen {
        companion object {
            val EXAMPLE_NAME = CodegenSpec(listOf("DataClass"))
        }
    }

    class DataClass {
        companion object {
            val EXAMPLE_NAME = DataClassSpec("Name", "io.gitlab.ettoreleandrotognoli.example", listOf(Property(name = "value", type = "String")))
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