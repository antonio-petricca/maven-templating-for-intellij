package com.github.intellij.plugins.mt4ij.actions

import com.github.intellij.plugins.mt4ij.ApiHelpers
import com.github.intellij.plugins.mt4ij.Bundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import org.jetbrains.idea.maven.project.MavenProjectsManager

// Text and description come from the bundle (action.mt4ij.actions.project-scanner.*)
class ProjectScannerAction : AnAction(), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val mavenized = (project != null) && MavenProjectsManager.getInstance(project).isMavenizedProject

        // Always listed, so that the user sees why there is nothing to do in a project that is not Maven
        e.presentation.isVisible = true
        e.presentation.isEnabled = mavenized
        e.presentation.description = Bundle.message(
            if (mavenized) "action.mt4ij.actions.project-scanner.description"
            else "mt4ij.actions.project-scanner.not-maven"
        )
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        ApiHelpers.scanProject(project, notify = true)
    }
}
