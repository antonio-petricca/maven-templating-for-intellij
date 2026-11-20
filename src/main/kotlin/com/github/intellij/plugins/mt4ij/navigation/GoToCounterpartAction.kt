package com.github.intellij.plugins.mt4ij.navigation

import com.github.intellij.plugins.mt4ij.Bundle
import com.github.intellij.plugins.mt4ij.MavenTemplatingIcons
import com.github.intellij.plugins.mt4ij.TemplateLocator
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbAware

/**
 * Opens the generated source of the current template, or the template of the current generated source.
 *
 * In the Maven Templating submenu it stays in view, disabled, when the file has no counterpart;
 * [GoToCounterpartNavigateAction] is the variant that hides itself in the Navigate and Go To menus.
 */
open class GoToCounterpartAction : AnAction(), DumbAware {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    /** Whether the action disappears, instead of being disabled, when the file is neither a template nor a generated source. */
    protected open val hiddenWhenNotApplicable = false

    override fun update(event: AnActionEvent) {
        val project      = event.project
        val file         = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val counterpart  = if (project != null && file != null) TemplateLocator.counterpartOf(project, file) else null
        val presentation = event.presentation

        when {
            counterpart != null -> {
                presentation.isEnabledAndVisible = true
                presentation.icon = counterpart.icon
                presentation.text = counterpart.actionText
                presentation.description = Bundle.message("action.mt4ij.actions.go-to-counterpart.description")
            }

            // A template whose generated source does not exist yet: shown, but there is nowhere to go
            project != null && file != null && TemplateLocator.isTemplate(project, file) -> {
                presentation.isVisible = true
                presentation.isEnabled = false
                presentation.icon = MavenTemplatingIcons.GoToGenerated
                presentation.text = Bundle.message("mt4ij.navigation.go-to-generated")
                presentation.description = Bundle.message("mt4ij.navigation.go-to-generated.not-built")
            }

            hiddenWhenNotApplicable -> presentation.isEnabledAndVisible = false

            else -> {
                presentation.isVisible = true
                presentation.isEnabled = false
                presentation.icon = MavenTemplatingIcons.GoToGenerated
                presentation.text = Bundle.message("action.mt4ij.actions.go-to-counterpart.text")
                presentation.description = Bundle.message("mt4ij.navigation.go-to-counterpart.unavailable")
            }
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

/** The jump as listed in the Navigate menu and in the Go To submenu of the editor, where it shows up only when it applies. */
class GoToCounterpartNavigateAction : GoToCounterpartAction() {

    override val hiddenWhenNotApplicable = true
}
