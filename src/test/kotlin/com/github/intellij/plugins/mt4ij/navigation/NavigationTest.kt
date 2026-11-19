package com.github.intellij.plugins.mt4ij.navigation

import com.github.intellij.plugins.mt4ij.MavenTemplatingTestCase
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.TestActionEvent
import kotlinx.coroutines.runBlocking
import org.junit.Test

class NavigationTest : MavenTemplatingTestCase() {

    private val source = "package demo; class Greeter {}"

    private lateinit var template: VirtualFile
    private lateinit var generated: VirtualFile

    private fun importTemplatesProject(withGenerated: Boolean = true) {
        template = createProjectSubFile("src/main/java-templates/demo/Greeter.java", source)
        generated = createProjectSubFile(
            if (withGenerated) "target/generated-sources/java-templates/demo/Greeter.java" else "target/other/Greeter.java",
            source
        )

        runBlocking {
            createProjectSubDirs("src/main/java")
            importProjectAsync(pomWithPlugin("project"))
        }
    }

    private fun updateAction(file: VirtualFile): AnActionEvent {
        val action  = GoToCounterpartAction()
        val context = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.VIRTUAL_FILE, file)
            .build()
        val event   = TestActionEvent.createTestEvent(action, context)

        runReadActionBlocking { action.update(event) }

        return event
    }

    private fun markersOf(file: PsiFile): List<LineMarkerInfo<*>> = runReadActionBlocking {
        val result = mutableListOf<LineMarkerInfo<*>>()
        val psiClass = (file as PsiJavaFile).classes.single()

        TemplateLineMarkerProvider().collectSlowLineMarkers(listOf(psiClass.nameIdentifier!!), result)

        result
    }

    @Test
    fun testActionIsOfferedOnATemplateWhenTheGeneratedSourceExists() {
        importTemplatesProject()

        val presentation = updateAction(template).presentation

        assertTrue(presentation.isEnabledAndVisible)
        assertTrue(presentation.text, presentation.text.isNotBlank())
    }

    @Test
    fun testActionIsOfferedOnTheGeneratedSourceToo() {
        importTemplatesProject()

        assertTrue(updateAction(generated).presentation.isEnabledAndVisible)
    }

    @Test
    fun testActionTextTellsTheDirection() {
        importTemplatesProject()

        assertNotSame(updateAction(template).presentation.text, updateAction(generated).presentation.text)
    }

    @Test
    fun testActionIsHiddenWhenThereIsNoCounterpart() {
        importTemplatesProject(withGenerated = false)

        assertFalse(updateAction(template).presentation.isEnabledAndVisible)
    }

    @Test
    fun testActionIsHiddenOnOrdinarySources() {
        importTemplatesProject()
        val plain = createProjectSubFile("src/main/java/demo/Plain.java", "package demo; class Plain {}")

        assertFalse(updateAction(plain).presentation.isEnabledAndVisible)
    }

    @Test
    fun testLineMarkerIsOfferedOnTheTemplateClass() {
        importTemplatesProject()

        assertEquals(1, markersOf(psiOf(template)).size)
    }

    @Test
    fun testNoLineMarkerWithoutCounterpart() {
        importTemplatesProject(withGenerated = false)

        assertTrue(markersOf(psiOf(template)).isEmpty())
    }
}
