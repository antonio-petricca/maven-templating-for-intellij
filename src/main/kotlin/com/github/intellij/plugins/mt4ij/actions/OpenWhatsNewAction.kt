package com.github.intellij.plugins.mt4ij.actions

import com.github.intellij.plugins.mt4ij.guide.GuideTabs
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

// Text and description come from the bundle (action.mt4ij.actions.whats-new.*)
class OpenWhatsNewAction : AnAction(), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        GuideTabs.openWhatsNew(e.project ?: return)
    }
}
