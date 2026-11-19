package com.github.intellij.plugins.mt4ij.properties

import com.github.intellij.plugins.mt4ij.Bundle
import com.github.intellij.plugins.mt4ij.TemplateLocator
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLiteralExpression

/** Warns about `${name}` properties in templates that neither the pom nor the Maven model define. */
class UnresolvedMavenPropertyInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val file = holder.file.virtualFile

        if (file == null || !TemplateLocator.isTemplate(holder.project, file)) {
            return PsiElementVisitor.EMPTY_VISITOR
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is PsiLiteralExpression || element is PsiComment) {
                    check(element, holder)
                }
            }
        }
    }

    private fun check(element: PsiElement, holder: ProblemsHolder) {
        MavenPropertyReference.allIn(element)
            .filter { it.isClosed && it.propertyName.isNotEmpty() } // still being typed
            .filterNot { it.isDefined() }
            .forEach { reference ->
                holder.registerProblem(
                    element,
                    reference.rangeInElement,
                    Bundle.message("mt4ij.inspection.unresolved-property.message", reference.propertyName),
                    AddPropertyToPomFix(reference.propertyName)
                )
            }
    }
}
