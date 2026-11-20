package com.github.intellij.plugins.mt4ij.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware

/** The "Maven Templating" submenu: always there, so that the guide is reachable in any project. */
class MavenTemplatingGroup : DefaultActionGroup(), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isPopupGroup = true
        e.presentation.isVisible = true
    }
}
