package com.github.intellij.plugins.mt4ij

import org.junit.Test
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.runBlocking

class TemplateLocatorTest : MavenTemplatingTestCase() {

    private fun counterpartOf(file: VirtualFile) = runReadActionBlocking { TemplateLocator.counterpartOf(project, file) }

    private fun isTemplate(file: VirtualFile) = runReadActionBlocking { TemplateLocator.isTemplate(project, file) }

    private fun importTemplatedProject() = runBlocking {
        createProjectSubDirs("src/main/java")
        importProjectAsync(pomWithPlugin("project"))
    }

    @Test
    fun testTemplateAndGeneratedSourceAreCounterpartsOfEachOther() {
        val template  = createProjectSubFile("src/main/java-templates/demo/Greeter.java", "package demo; class Greeter {}")
        val generated = createProjectSubFile("target/generated-sources/java-templates/demo/Greeter.java", "package demo; class Greeter {}")
        importTemplatedProject()

        val fromTemplate  = counterpartOf(template)
        val fromGenerated = counterpartOf(generated)

        assertEquals(generated, fromTemplate!!.file)
        assertTrue(fromTemplate.isGenerated)
        assertEquals(template, fromGenerated!!.file)
        assertFalse(fromGenerated.isGenerated)
    }

    @Test
    fun testTestTemplateAndItsGeneratedSourceAreCounterparts() {
        val template  = createProjectSubFile("src/test/java-templates/demo/GreeterTest.java", "package demo; class GreeterTest {}")
        val generated = createProjectSubFile("target/generated-test-sources/java-templates/demo/GreeterTest.java", "package demo; class GreeterTest {}")
        importTemplatedProject()

        assertEquals(generated, counterpartOf(template)!!.file)
        assertEquals(template, counterpartOf(generated)!!.file)
    }

    @Test
    fun testTemplateWithoutGeneratedSourceHasNoCounterpart() {
        val template = createProjectSubFile("src/main/java-templates/demo/Greeter.java", "package demo; class Greeter {}")
        importTemplatedProject()

        assertNull(counterpartOf(template))
    }

    @Test
    fun testOrdinarySourceIsNotATemplateAndHasNoCounterpart() {
        val plain = createProjectSubFile("src/main/java/demo/Plain.java", "package demo; class Plain {}")
        importTemplatedProject()

        assertFalse(isTemplate(plain))
        assertNull(counterpartOf(plain))
    }

    @Test
    fun testFileInTheTemplatesFolderIsATemplate() {
        val template = createProjectSubFile("src/main/java-templates/demo/Greeter.java", "package demo; class Greeter {}")
        importTemplatedProject()

        assertTrue(isTemplate(template))
    }
}
