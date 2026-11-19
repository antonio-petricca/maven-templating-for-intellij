package com.github.intellij.plugins.mt4ij.properties

import com.github.intellij.plugins.mt4ij.MavenTemplatingTestCase
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TemplateInspectionTest : MavenTemplatingTestCase() {

    private fun templateWith(vararg literals: String): PsiFile {
        val fields = literals.mapIndexed { index, literal -> "    String f$index = \"$literal\";" }.joinToString("\n")

        createProjectSubDirs("src/main/java", "src/main/java-templates/demo")
        val template = createProjectSubFile("src/main/java-templates/demo/Probe.java", "package demo;\nclass Probe {\n$fields\n}\n")

        runBlocking { importProjectAsync(pomWithPlugin("project")) }

        return psiOf(template)
    }

    private fun reported(file: PsiFile) = runInspection(file).map { it.descriptionTemplate }

    @Test
    fun testMavenAndSystemPropertiesAreNotReported() {
        val file = templateWith(
            "\${basedir}", "\${project.packaging}", "\${project.build.finalName}", "\${pom.version}",
            "\${line.separator}", "\${maven.build.timestamp}", "\${project.version}", "\${env.HOME}",
        )

        assertEquals("unexpected warnings: ${reported(file)}", emptyList<String>(), reported(file))
    }

    @Test
    fun testUnclosedPlaceholdersAreNotReported() {
        val file = templateWith("\${", "\${pro")

        assertEquals("unexpected warnings: ${reported(file)}", emptyList<String>(), reported(file))
    }

    @Test
    fun testUnknownPropertyNextToUnclosedOneIsStillReported() {
        val file = templateWith("\${pro", "\${unknown.name}")

        val messages = reported(file)

        assertEquals(messages.toString(), 1, messages.size)
        assertTrue(messages.single().contains("unknown.name"))
    }

    @Test
    fun testCompletionOffersPropertiesForAnUnclosedPlaceholder() {
        val file = templateWith("\${pro")

        val variants = runReadActionBlocking { referenceTo(file, "pro").variants.map { (it as LookupElement).lookupString } }

        assertContainsElements(variants, "project.version", "project.artifactId")
    }
}
