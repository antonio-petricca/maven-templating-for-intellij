package com.github.intellij.plugins.mt4ij.actions

import com.github.intellij.plugins.mt4ij.PluginHelpers
import com.github.intellij.plugins.mt4ij.Bundle
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class ProjectScannerAction :
    AnAction(Bundle.message("mt4ij.actions.project-scanner.text")),
    DumbAware
{

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project!!

        PluginHelpers.invokeLater(
            {
                PluginHelpers.scanProject(project)
            },
            project
        )
    }
}
