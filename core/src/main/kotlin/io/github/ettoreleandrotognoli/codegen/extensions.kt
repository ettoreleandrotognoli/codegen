package io.github.ettoreleandrotognoli.codegen

import java.io.InputStream

typealias SnakeYaml = org.yaml.snakeyaml.Yaml

fun InputStream.asString(): String {
    val stringBuilder = StringBuilder()
    val buffer = ByteArray(this.available())
    this.read(buffer)
    stringBuilder.append(String(buffer))
    return stringBuilder.toString()
}
