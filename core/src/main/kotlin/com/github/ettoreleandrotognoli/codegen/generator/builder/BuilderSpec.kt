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
        val of: ClassName,
        val extends: TypeName,
        val implements: List<TypeName>,
        val dataclass: Optional<DataClassSpec> = Optional.empty()
) : BaseSpec(type) {

    companion object {
        fun from(rawSpec: BuilderRawSpec, context: PreBuildContext? = null): BuilderSpec {
            val type = ClassName.get(rawSpec.packageName, rawSpec.name).nestedClass("Builder")
            val concreteType = ClassName.get(rawSpec.packageName, rawSpec.name).nestedClass("BuildImpl")
            val of = ClassName.get(rawSpec.packageName, rawSpec.name)
            val extends = asType(rawSpec.builder.extends);
            val implements = rawSpec.builder.implements.map { asType(it) }
            if (context == null) {
                return BuilderSpec(
                        type = type,
                        concreteType = concreteType,
                        of = of,
                        extends = extends,
                        implements = implements
                )
            }
            val optionalDataclass = context.getSpec(DataClassSpec::class)
                    .filter { it.type.packageName() == rawSpec.packageName }
                    .filter { it.type.simpleName() == rawSpec.name }
                    .firstOrNull()
                    .let { Optional.ofNullable(it) }


            val dataclass = optionalDataclass.orElseThrow { Exception("A dataclass for $of was not defined") }

            return BuilderSpec(
                    type = type,
                    concreteType = concreteType,
                    of = of,
                    extends = extends,
                    implements = implements,
                    dataclass = optionalDataclass
            )

        }
    }

}
