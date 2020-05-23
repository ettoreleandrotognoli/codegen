package com.github.ettoreleandrotognoli.codegen.generator.entity

import com.github.ettoreleandrotognoli.codegen.*
import com.github.ettoreleandrotognoli.codegen.api.BuildContext
import com.github.ettoreleandrotognoli.codegen.api.PreBuildContext
import com.github.ettoreleandrotognoli.codegen.core.AbstractCodeGenerator
import com.github.ettoreleandrotognoli.codegen.generator.makeEquals
import com.github.ettoreleandrotognoli.codegen.generator.makeHashCode
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import org.springframework.stereotype.Component
import javax.lang.model.element.Modifier

@Component
class EntityGenerator : AbstractCodeGenerator<EntityRawSpec>(EntityRawSpec::class) {

    override fun typedPrepareContext(context: PreBuildContext.Mutable, rawSpec: EntityRawSpec) {
        val spec = EntitySpec.from(rawSpec)
        context.registerPreSpec(spec)
        context.registerIntention(spec.type)
    }

    override fun typedGenerate(context: BuildContext.Mutable, rawSpec: EntityRawSpec) {
        val spec = EntitySpec.from(rawSpec, context)

        val dataclass = spec.dataclass.get()

        spec.primaryKey
                .filter { !dataclass.properties.contains(it) }
                .let {
                    if (it.isNotEmpty()) throw Exception("The field(s) $it defined as primary key not exists in ${spec.of}")
                }

        val entityClassBuilder = TypeSpec.classBuilder(spec.type)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .superclass(spec.extends)
                .addSuperinterface(spec.of)

        spec.implements.forEach { entityClassBuilder.addSuperinterface(it) }

        constructors(spec.of, dataclass.propertyType).forEach {
            entityClassBuilder.addMethod(it.build())
        }

        dataclass.propertyType
                .map { FieldSpec.builder(it.value, it.key, Modifier.PRIVATE) }
                .map { it.build() }
                .forEach { entityClassBuilder.addField(it) }


        dataclass.propertyType
                .map {
                    buildConcreteGetMethod(it.key, it.value)
                            .addStatement("return this.$1L", it.key)
                }
                .map { it.build() }
                .forEach { entityClassBuilder.addMethod(it) }

        dataclass.propertyType
                .map {
                    buildConcreteSetMethod(it.key, it.value)
                            .addStatement("this.$1L = $1L", it.key)
                }
                .map { it.build() }
                .forEach { entityClassBuilder.addMethod(it) }


        entityClassBuilder.addMethod(makeHashCode(dataclass.rawSpec, spec.primaryKey))

        entityClassBuilder.addMethod(makeEquals(dataclass, spec.primaryKey))

        entityClassBuilder.addMethod(shallowCopyMethod(spec.of, spec.type, dataclass.propertyType).build())

        entityClassBuilder.addMethod(shallowCloneMethod(spec.type).build())

        entityClassBuilder.addMethod(deepCopyMethod(spec.of, spec.type, dataclass.propertyType, spec.concreteTypes).build())

        entityClassBuilder.addMethod(copyMethod(spec.of, spec.type, default = dataclass.rawSpec.copy.default).build())

        entityClassBuilder.addMethod(cloneMethod(spec.type).build())

        entityClassBuilder.addMethod(deepCloneMethod(spec.type).build())

        if (dataclass.rawSpec.toString.enable) {
            entityClassBuilder.addMethod(makeToString(spec.type, dataclass.propertyType, dataclass.rawSpec.toString.pattern))
        }


        val dataclassBuilder = context.getTypeSpecBuilder(spec.of).get()
        dataclassBuilder.addType(entityClassBuilder.build())
    }
}