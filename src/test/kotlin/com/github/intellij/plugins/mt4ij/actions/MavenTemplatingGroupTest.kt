package com.github.intellij.plugins.mt4ij.actions

import com.github.intellij.plugins.mt4ij.MavenTemplatingTestCase
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.testFramework.TestActionEvent
import kotlinx.coroutines.runBlocking
import org.junit.Test

class MavenTemplatingGroupTest : MavenTemplatingTestCase() {

    private val groupId = "mt4ij.groups.main"

    private fun updateGroup(): AnActionEvent {
        val group   = MavenTemplatingGroup()
        val context = SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build()
        val event   = TestActionEvent.createTestEvent(group, context)

        runReadActionBlocking { group.update(event) }

        return event
    }

    @Test
    fun testGroupIsVisibleInAMavenProject() {
        runBlocking { importProjectAsync(pomWithPlugin("project")) }

        val presentation = updateGroup().presentation

        assertTrue(presentation.isVisible)
        assertTrue(presentation.isPopupGroup)
    }

    @Test
    fun testGroupRunsOnABackgroundThread() {
        assertEquals(ActionUpdateThread.BGT, MavenTemplatingGroup().actionUpdateThread)
    }

    @Test
    fun testGroupHoldsTheNavigationScanAndGuideActions() {
        val group = ActionManager.getInstance().getAction(groupId) as DefaultActionGroup
        val ids   = group.getChildren(null).map { ActionManager.getInstance().getId(it) }

        assertEquals(
            listOf("mt4ij.actions.go-to-counterpart", "mt4ij.actions.project-scanner", null, "mt4ij.actions.open-guide", "mt4ij.actions.whats-new"),
            ids
        )
    }

    @Test
    fun testNavigationActionHasAShortcutNoOtherActionUses() {
        val actionId  = "mt4ij.actions.go-to-counterpart"
        val keymap    = KeymapManager.getInstance().activeKeymap
        val shortcuts = keymap.getShortcuts(actionId).filterIsInstance<KeyboardShortcut>()

        assertTrue(shortcuts.isNotEmpty())
        shortcuts.forEach { assertEquals(listOf(actionId), keymap.getActionIds(it.firstKeyStroke).toList()) }
    }

    @Test
    fun testNavigationActionIsInTheGoToMenuOfTheEditor() {
        val goTo = ActionManager.getInstance().getAction("EditorPopupMenu.GoTo") as DefaultActionGroup
        val ids  = goTo.getChildren(null).map { ActionManager.getInstance().getId(it) }

        assertTrue(ids.toString(), ids.contains("mt4ij.actions.go-to-counterpart"))
    }
}
