package com.github.intellij.plugins.mt4ij.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.project.MavenProjectsManager

/** The "Maven Templating" submenu, offered only in the projects imported from Maven. */
class MavenTemplatingGroup : DefaultActionGroup(), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isPopupGroup = true
        e.presentation.isVisible = e.project?.let(::isMavenized) ?: false
    }

    private fun isMavenized(project: Project) = MavenProjectsManager.getInstance(project).isMavenizedProject
}
