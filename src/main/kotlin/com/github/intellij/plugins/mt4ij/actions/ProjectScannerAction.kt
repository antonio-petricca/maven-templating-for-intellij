package com.github.intellij.plugins.mt4ij.actions

import com.github.intellij.plugins.mt4ij.ApiHelpers
import com.github.intellij.plugins.mt4ij.Bundle
import com.github.intellij.plugins.mt4ij.activities.ProjectScannerActivity
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class ProjectScannerAction :
    AnAction(Bundle.message("mt4ij.actions.project-scanner.text")),
    DumbAware
{

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project!!

        ApiHelpers.invokeLater(
            {
                val projectScannerActivity = ProjectScannerActivity()
                projectScannerActivity.runActivity(project)
            },
            project
        )
    }
}
