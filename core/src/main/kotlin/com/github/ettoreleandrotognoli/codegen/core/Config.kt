package com.github.ettoreleandrotognoli.codegen.core

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.github.ettoreleandrotognoli.codegen.SnakeYaml
import com.github.ettoreleandrotognoli.codegen.api.CodeGenerator
import com.github.ettoreleandrotognoli.codegen.api.CodeGeneratorResolver
import com.github.ettoreleandrotognoli.codegen.api.CodeSpecClassResolver
import com.github.ettoreleandrotognoli.codegen.api.RawCodeSpec
import com.github.ettoreleandrotognoli.codegen.generator.data.DataClassRawSpec
import com.github.ettoreleandrotognoli.codegen.generator.entity.EntityRawSpec
import com.github.ettoreleandrotognoli.codegen.generator.jdesktop.ObservableRawSpec
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.yaml.snakeyaml.DumperOptions
import kotlin.reflect.KClass

@Configuration
open class Config {

    @Bean
    open fun yaml(): Yaml {
        val configuration = YamlConfiguration(strictMode = false)
        return Yaml(configuration = configuration)
    }

    @Bean
    open fun snakeYaml(): SnakeYaml {
        val dumperOptions = DumperOptions()
        dumperOptions.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        return SnakeYaml(dumperOptions)
    }

    @Bean
    open fun codeSpecClassResolver(): CodeSpecClassResolver {
        val aliases = mapOf(
                "DataClass" to DataClassRawSpec::class,
                "Observable" to ObservableRawSpec::class,
                "Entity" to EntityRawSpec::class
        )
        return DefaultCodeSpecClassResolver(aliases)
    }

    @Bean
    open fun codeGeneratorResolver(generators: List<CodeGenerator<*>>): CodeGeneratorResolver {
        val generatorsMap = HashMap<KClass<out RawCodeSpec>, MutableList<CodeGenerator<*>>>()
        generators.forEach {
            val list = generatorsMap.getOrDefault(it.specType(), mutableListOf())
            list.add(it)
            generatorsMap.put(it.specType(), list)
        }
        return DefaultCodeGeneratorResolver(generatorsMap)
    }

}