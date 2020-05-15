package com.github.ettoreleandrotognoli.codegen.core

import com.github.ettoreleandrotognoli.codegen.api.CodeSpec
import com.github.ettoreleandrotognoli.codegen.api.Context
import com.github.ettoreleandrotognoli.codegen.api.Project

class CodegenContext(
        override val project: Project,
        private val specs: MutableMap<CodeSpec, String> = HashMap()
) : Context {

    override fun getRawSpec(codeSpec: CodeSpec): String {
        return specs[codeSpec]!!
    }

    fun putRawSpec(codeSpec: CodeSpec, rawSpec: String) {
        specs[codeSpec] = rawSpec
    }
}