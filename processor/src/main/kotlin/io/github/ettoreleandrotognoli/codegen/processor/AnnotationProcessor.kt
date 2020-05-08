package io.github.ettoreleandrotognoli.codegen.processor

import com.google.auto.service.AutoService
import io.github.ettoreleandrotognoli.codegen.data.DataClass
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.swing.JOptionPane

@AutoService(Processor::class)
@SupportedAnnotationTypes("io.gitlab.ettoreleandrotognoli.codegen.*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class AnnotationProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val elements: Set<Element?> = roundEnv.getElementsAnnotatedWith(DataClass::class.java)
        JOptionPane.showMessageDialog(null, "AnnotationProcessor")
        return false
    }
}