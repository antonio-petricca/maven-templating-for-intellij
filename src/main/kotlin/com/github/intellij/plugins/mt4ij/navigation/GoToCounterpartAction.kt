package com.github.intellij.plugins.mt4ij.navigation

import com.github.intellij.plugins.mt4ij.Bundle
import com.github.intellij.plugins.mt4ij.TemplateLocator
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbAware

/** Opens the generated source of the current template, or the template of the current generated source. */
class GoToCounterpartAction : AnAction(), DumbAware {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val counterpart = event.project?.let { project ->
            event.getData(CommonDataKeys.VIRTUAL_FILE)?.let { TemplateLocator.counterpartOf(project, it) }
        }

        event.presentation.isEnabledAndVisible = counterpart != null

        if (counterpart != null) {
            event.presentation.text = Bundle.message(
                if (counterpart.isGenerated) "mt4ij.navigation.go-to-generated" else "mt4ij.navigation.go-to-template"
            )
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file    = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        // Runs on the EDT, which has no implicit read access
        val target  = runReadActionBlocking { TemplateLocator.counterpartOf(project, file) } ?: return

        OpenFileDescriptor(project, target.file).navigate(true)
    }
}
