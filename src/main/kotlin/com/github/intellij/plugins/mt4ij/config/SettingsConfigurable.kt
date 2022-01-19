package com.github.intellij.plugins.mt4ij.config

import com.github.intellij.plugins.mt4ij.ApiHelpers
import com.github.intellij.plugins.mt4ij.Bundle
import com.github.intellij.plugins.mt4ij.activities.ProjectScannerActivity
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

/*
    // Swing UI

    https://stackoverflow.com/a/29258675/418599

    // Configuration UI

    https://intellij-support.jetbrains.com/hc/en-us/community/posts/360003316280-Adding-Your-Plugin-Setting-to-the-IDE-Settings-Dialog
    https://programtalk.com/vs/intellij-haxe/src/common/com/intellij/plugins/haxe/config/HaxeSettingsConfigurable.java/

    // Searchable label

    https://intellij-support.jetbrains.com/hc/en-us/community/posts/206133719-How-does-SearchableConfigurable-work
 */

internal class SettingsConfigurable(project: Project) : SearchableConfigurable {
    private val projectRef   = project
    private val settingsForm = SettingsForm()

    private fun getSettings() : SettingsStorage {
        return SettingsStorage.getInstance(projectRef)
    }

    override fun createComponent(): JComponent? {
        reset()
        settingsForm.setTemplatesPath(getSettings().state.templatesPath)

        return settingsForm.mainPanel
    }

    override fun isModified(): Boolean {
        return settingsForm.isModified(getSettings().state)
    }

    override fun apply() {
        getSettings().state.templatesPath = settingsForm.getTemplatesPath()!!

        ApiHelpers.invokeLater(
            {
                val projectScannerActivity = ProjectScannerActivity()
                projectScannerActivity.runActivity(projectRef)
            },
            projectRef
        )
    }

    override fun getDisplayName(): String {
        return Bundle.message("mt4ij.settings.name")
    }

    override fun getId(): String {
        return displayName
    }
}
