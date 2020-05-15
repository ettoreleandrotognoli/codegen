package com.github.ettoreleandrotognoli.codegen.processor

import com.google.auto.service.AutoService
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.swing.JOptionPane

@AutoService(Processor::class)
@SupportedAnnotationTypes("com.github.ettoreleandrotognoli.codegen.*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class AnnotationProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        JOptionPane.showMessageDialog(null, "AnnotationProcessor")
        return false
    }
}