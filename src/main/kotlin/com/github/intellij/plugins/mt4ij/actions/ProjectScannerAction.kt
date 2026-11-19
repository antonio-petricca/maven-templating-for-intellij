package com.github.intellij.plugins.mt4ij.actions

import com.github.intellij.plugins.mt4ij.ApiHelpers
import com.github.intellij.plugins.mt4ij.Bundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import org.jetbrains.idea.maven.project.MavenProjectsManager

class ProjectScannerAction :
    AnAction(Bundle.message("mt4ij.actions.project-scanner.title")),
    DumbAware
{

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project

        e.presentation.isEnabledAndVisible =
            (project != null) && MavenProjectsManager.getInstance(project).isMavenizedProject
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        ApiHelpers.scanProject(project)
    }
}
