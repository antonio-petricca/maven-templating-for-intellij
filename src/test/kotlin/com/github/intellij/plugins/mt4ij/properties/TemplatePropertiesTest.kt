package com.github.intellij.plugins.mt4ij.properties

import org.junit.Test
import com.github.intellij.plugins.mt4ij.MavenTemplatingTestCase
import com.github.intellij.plugins.mt4ij.TemplateLocator
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlTag
import com.intellij.testFramework.runInEdtAndGet
import kotlinx.coroutines.runBlocking

class TemplatePropertiesTest : MavenTemplatingTestCase() {

    private val templateText = """
        package demo;

        class Greeter {
            String name    = "${'$'}{greeting.name}";
            String version = "${'$'}{project.version}";
            String home    = "${'$'}{env.HOME}";
            String missing = "${'$'}{no.such.property}";
        }
    """.trimIndent()

    private fun importProjectWithTemplate(properties: String = "<properties><greeting.name>World</greeting.name></properties>"): PsiFile {
        createProjectSubDirs("src/main/java", "src/main/java-templates/demo")
        val template = createProjectSubFile("src/main/java-templates/demo/Greeter.java", templateText)

        runBlocking { importProjectAsync(pomWithPlugin("project", extra = properties)) }

        return psiOf(template)
    }

    @Test
    fun testFileInTheTemplatesFolderIsATemplate() {
        val file = importProjectWithTemplate()

        assertTrue(runReadActionBlocking { TemplateLocator.isTemplate(project, file.virtualFile) })
    }

    @Test
    fun testPropertyReferenceResolvesToThePomProperty() {
        val file = importProjectWithTemplate()

        val target = referenceTo(file, "greeting.name").let { reference -> runReadActionBlocking { reference.resolve() } }

        assertTrue(target is XmlTag)
        assertEquals("greeting.name", runReadActionBlocking { (target as XmlTag).name })
    }

    @Test
    fun testCompletionOffersPomPropertiesAndModelProperties() {
        val file = importProjectWithTemplate()

        val variants = runReadActionBlocking {
            referenceTo(file, "greeting.name").variants.map { (it as LookupElement).lookupString }
        }

        assertContainsElements(variants, "greeting.name", "project.version", "project.artifactId")
    }

    @Test
    fun testUnknownPropertyIsReported() {
        val file = importProjectWithTemplate()

        val messages = runInspection(file).map { it.descriptionTemplate }

        assertEquals(1, messages.size)
        assertTrue(messages.single().contains("no.such.property"))
    }

    @Test
    fun testPomPropertyModelPropertyAndEnvironmentAreNotReported() {
        val file = importProjectWithTemplate()

        val messages = runInspection(file).map { it.descriptionTemplate }

        assertFalse(messages.any { it.contains("greeting.name") })
        assertFalse(messages.any { it.contains("project.version") })
        assertFalse(messages.any { it.contains("env.HOME") })
    }

    @Test
    fun testQuickFixAddsTheMissingPropertyToThePom() {
        val file = importProjectWithTemplate()

        invokeQuickFix(file, runInspection(file).single())

        assertContainsElements(declaredPomProperties(), "no.such.property", "greeting.name")
        assertTrue("the warning must be gone", runInspection(file).isEmpty())
    }

    @Test
    fun testQuickFixOpensThePomWithTheCursorInTheEmptyValue() {
        val file = importProjectWithTemplate()

        invokeQuickFix(file, runInspection(file).single())

        val editor = runInEdtAndGet { FileEditorManager.getInstance(project).selectedTextEditor!! }
        val (before, after) = runInEdtAndGet {
            val text   = editor.document.text
            val offset = editor.caretModel.offset

            text.substring(0, offset) to text.substring(offset)
        }

        assertEquals(projectPom, FileDocumentManager.getInstance().getFile(editor.document))
        assertTrue(before, before.endsWith("<no.such.property>"))
        assertTrue(after, after.startsWith("</no.such.property>"))
    }

    @Test
    fun testQuickFixCreatesThePropertiesTagWhenThePomHasNone() {
        val file = importProjectWithTemplate(properties = "")

        invokeQuickFix(file, runInspection(file).first { it.descriptionTemplate.contains("greeting.name") })

        assertContainsElements(declaredPomProperties(), "greeting.name")
    }

    @Test
    fun testFilesOutsideTheTemplatesAreNotInspected() {
        importProjectWithTemplate()

        val plain = createProjectSubFile("src/main/java/demo/Plain.java", "package demo; class Plain { String s = \"\${unknown}\"; }")

        assertTrue(runInspection(psiOf(plain)).isEmpty())
    }
}
