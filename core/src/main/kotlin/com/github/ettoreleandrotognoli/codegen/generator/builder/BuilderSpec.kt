package com.github.ettoreleandrotognoli.codegen.generator.builder

import com.github.ettoreleandrotognoli.codegen.api.BaseSpec
import com.github.ettoreleandrotognoli.codegen.api.PreBuildContext
import com.github.ettoreleandrotognoli.codegen.generator.asType
import com.github.ettoreleandrotognoli.codegen.generator.data.DataClassSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import java.util.*

class BuilderSpec(
        type: ClassName,
        val concreteType: ClassName,
        val `for`: ClassName,
        val extends: TypeName,
        val implements: List<TypeName>,
        val nestedInterface: Boolean,
        val dataclass: Optional<DataClassSpec> = Optional.empty()
) : BaseSpec(type) {

    companion object {


        fun from(rawSpec: BuilderRawSpec, context: PreBuildContext? = null): BuilderSpec {
            val nestedInterface = rawSpec.builder.nested
            val `for` = if (rawSpec.builder.`for` == null) ClassName.get(rawSpec.packageName, rawSpec.name) else ClassName.bestGuess(rawSpec.builder.`for`)
            val type = when {
                nestedInterface -> `for`.nestedClass("Builder")
                rawSpec.builder.`for` != null -> ClassName.get(rawSpec.packageName, rawSpec.name)
                else -> ClassName.get(rawSpec.packageName + "." + rawSpec.name.toLowerCase() + ".builder", "Builder")
            }
            val concreteType = type.nestedClass("Impl")
            val extends = asType(rawSpec.builder.extends);
            val implements = rawSpec.builder.implements.map { asType(it) }
            if (context == null) {
                return BuilderSpec(
                        type = type,
                        concreteType = concreteType,
                        `for` = `for`,
                        extends = extends,
                        implements = implements,
                        nestedInterface = nestedInterface
                )
            }
            val optionalDataclass = context.getSpec(DataClassSpec::class)
                    .filter { it.type == `for` }
                    .firstOrNull()
                    .let { Optional.ofNullable(it) }


            val dataclass = optionalDataclass.orElseThrow { Exception("A dataclass for $`for` was not defined") }

            return BuilderSpec(
                    type = type,
                    concreteType = concreteType,
                    `for` = `for`,
                    extends = extends,
                    implements = implements,
                    dataclass = optionalDataclass,
                    nestedInterface = nestedInterface
            )

        }
    }

}
