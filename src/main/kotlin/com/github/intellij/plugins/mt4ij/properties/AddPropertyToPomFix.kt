package com.github.intellij.plugins.mt4ij.properties

import com.github.intellij.plugins.mt4ij.Bundle
import com.github.intellij.plugins.mt4ij.TemplateLocator
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleManager

/** Declares the missing property, empty, in the `<properties>` of the module pom. */
class AddPropertyToPomFix(private val propertyName: String) : LocalQuickFix {

    override fun getFamilyName(): String = Bundle.message("mt4ij.quickfix.add-property.name")

    // The pom is not the file the fix starts from: it gets its own write command
    override fun startInWriteAction(): Boolean = false

    // The change happens in another file, so there is nothing to show in the preview
    override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo =
        IntentionPreviewInfo.EMPTY

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val file         = descriptor.psiElement.containingFile.virtualFile ?: return
        val mavenProject = TemplateLocator.mavenProjectOf(project, file) ?: return
        val pom          = TemplateProperties.modulePom(project, mavenProject) ?: return

        // Also checks that the pom is writable
        WriteCommandAction.writeCommandAction(project, pom).withName(familyName).run<RuntimeException> {
            val root = pom.rootTag ?: return@run

            val properties = root.findFirstSubTag("properties")
                ?: root.addSubTag(root.createChildTag("properties", root.namespace, null, false), false)

            if (properties.findFirstSubTag(propertyName) == null) {
                val added = properties.addSubTag(properties.createChildTag(propertyName, properties.namespace, "", false), false)

                CodeStyleManager.getInstance(project).reformat(added)
            }
        }
    }
}
