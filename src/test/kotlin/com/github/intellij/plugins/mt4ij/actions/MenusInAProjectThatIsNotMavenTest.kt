package com.github.intellij.plugins.mt4ij.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/** Apart from the Maven tests, whose project is always imported from a pom. */
class MenusInAProjectThatIsNotMavenTest : BasePlatformTestCase() {

    private val groupId = "mt4ij.groups.main"

    private fun update(action: AnAction): AnActionEvent {
        val context = SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build()
        val event   = TestActionEvent.createTestEvent(action, context)

        runReadActionBlocking { action.update(event) }

        return event
    }

    private fun actionNamed(id: String) = ActionManager.getInstance().getAction(id)

    private fun contains(menu: DefaultActionGroup, id: String): Boolean = menu.getChildren(null).any {
        ActionManager.getInstance().getId(it) == id || (it is DefaultActionGroup && contains(it, id))
    }

    fun testSubmenuIsVisible() {
        val presentation = update(actionNamed(groupId)).presentation

        assertTrue(presentation.isVisible)
        assertTrue(presentation.isPopupGroup)
    }

    fun testSubmenuIsInToolsAndInTheRightClickMenus() {
        listOf("ToolsMenu", "EditorPopupMenu", "ProjectViewPopupMenu", "EditorTabPopupMenu").forEach {
            assertTrue("$it has no Maven Templating submenu", contains(actionNamed(it) as DefaultActionGroup, groupId))
        }
    }

    fun testScanIsVisibleButDisabledAndExplainsWhy() {
        val presentation = update(actionNamed("mt4ij.actions.project-scanner")).presentation

        assertTrue(presentation.isVisible)
        assertFalse(presentation.isEnabled)
        assertEquals("Available only in projects imported from Maven.", presentation.description)
    }

    fun testGuideAndWhatsNewAreEnabled() {
        listOf("mt4ij.actions.open-guide", "mt4ij.actions.open-guide.help", "mt4ij.actions.whats-new").forEach {
            val presentation = update(actionNamed(it)).presentation

            assertTrue("$it is not visible", presentation.isVisible)
            assertTrue("$it is not enabled", presentation.isEnabled)
        }
    }

    fun testNavigationInTheSubmenuIsDisabledOnAFileThatIsNeitherTemplateNorGenerated() {
        val file    = myFixture.configureByText("Plain.java", "class Plain {}").virtualFile
        val action  = actionNamed("mt4ij.actions.go-to-counterpart")
        val context = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.VIRTUAL_FILE, file)
            .build()
        val event   = TestActionEvent.createTestEvent(action, context)

        runReadActionBlocking { action.update(event) }

        assertTrue(event.presentation.isVisible)
        assertFalse(event.presentation.isEnabled)
        assertEquals("Go to Generated Source / Go to Template", event.presentation.text)
        assertFalse(event.presentation.description.isNullOrBlank())
    }

    fun testNavigationInTheNavigateMenuIsHiddenOnAFileThatIsNeitherTemplateNorGenerated() {
        val file    = myFixture.configureByText("Plain.java", "class Plain {}").virtualFile
        val action  = actionNamed("mt4ij.actions.go-to-counterpart.navigate")
        val context = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.VIRTUAL_FILE, file)
            .build()
        val event   = TestActionEvent.createTestEvent(action, context)

        runReadActionBlocking { action.update(event) }

        assertFalse(event.presentation.isVisible)
    }
}
