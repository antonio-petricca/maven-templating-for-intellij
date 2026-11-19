package com.github.intellij.plugins.mt4ij.properties

import com.github.intellij.plugins.mt4ij.Bundle
import com.github.intellij.plugins.mt4ij.TemplateLocator
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.idea.maven.project.MavenProject

/** A `${name}` occurrence inside a template, resolving to the pom element that defines the property. */
class MavenPropertyReference(
    element: PsiElement,
    range: TextRange,
    val propertyName: String,
    /** False while the closing brace is missing, as in `${pro` typed but not finished. */
    val isClosed: Boolean = true,
) : PsiReferenceBase<PsiElement>(element, range, true) {

    companion object {
        // The name runs up to the closing brace, or up to a space or a quote when the brace is still missing
        private val OCCURRENCE = Regex("""\$\{([^}\s"]*)(})?""")

        /** The property references of a literal or comment, empty for elements outside the templates. */
        fun allIn(element: PsiElement): List<MavenPropertyReference> {
            val text = element.text

            if (!text.contains("\${")) {
                return emptyList()
            }

            val file = element.containingFile?.virtualFile ?: return emptyList()

            if (!TemplateLocator.isTemplate(element.project, file)) {
                return emptyList()
            }

            return OCCURRENCE.findAll(text).map { match ->
                val name  = match.groups[1]!!.value
                val start = match.range.first + 2 // after "${"

                MavenPropertyReference(element, TextRange(start, start + name.length), name, match.groups[2] != null)
            }.toList()
        }
    }

    private val mavenProject: MavenProject?
        get() = element.containingFile?.virtualFile?.let { TemplateLocator.mavenProjectOf(element.project, it) }

    /** The pom element defining the property (null when unknown, or when there is nothing to navigate to). */
    override fun resolve(): PsiElement? = definition()?.target

    /** True when the property is defined, even if it has no pom element. */
    fun isDefined(): Boolean = TemplateProperties.isIgnored(propertyName) || definition() != null

    private fun definition(): PropertyDefinition? =
        mavenProject?.let { TemplateProperties.find(element.project, it, propertyName) }

    override fun getVariants(): Array<Any> {
        val maven   = mavenProject ?: return emptyArray()
        val project = element.project

        val pomProperties = TemplateProperties.declared(project, maven).keys
        val modelNames    = TemplateProperties.modelNames()
        val resolved      = maven.properties.keys.map { it.toString() }

        val pomLookups = pomProperties.map {
            LookupElementBuilder.create(it)
                .withIcon(AllIcons.Nodes.Property)
                .withTypeText(Bundle.message("mt4ij.property.kind.pom"), true)
        }

        val otherLookups = (modelNames + resolved - pomProperties).map {
            LookupElementBuilder.create(it)
                .withIcon(AllIcons.Nodes.Property)
                .withTypeText(Bundle.message("mt4ij.property.kind.model"), true)
        }

        return (pomLookups + otherLookups).toTypedArray()
    }
}
