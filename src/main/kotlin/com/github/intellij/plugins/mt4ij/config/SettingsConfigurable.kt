package com.github.intellij.plugins.mt4ij.config

import com.github.intellij.plugins.mt4ij.ApiHelpers
import com.github.intellij.plugins.mt4ij.Bundle
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.Configurable
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

internal class SettingsConfigurable(private val project: Project) : Configurable {
    private val log: Logger = Logger.getInstance(SettingsConfigurable::class.java)
    private var settingsForm: SettingsForm? = null

    override fun createComponent(): JComponent? {
        log.info("Creating settings form...")

        if (settingsForm == null) {
            settingsForm = SettingsForm()
        }

        return settingsForm?.mainPanel
    }

    override fun isModified(): Boolean {
        log.debug("Checking if settings are modified...")

        val settings = SettingsStorage
            .getInstance(project)
            .state

        return settingsForm?.isModified(settings) ?: false
    }

    override fun apply() {
        log.info("Applying settings...")

        val settings = SettingsStorage
            .getInstance(project)
            .state

        settingsForm?.apply {
            settings.templatesPath = (getTemplatesPath() ?: "")
        }

        ApiHelpers.invokeLater(
            {
                ApiHelpers.scanProject(project)
            },
            project
        )
    }

    override fun getDisplayName(): String {
        log.debug("Getting display name...")

        return Bundle.message("mt4ij.settings.name")
    }

    override fun reset() {
        log.info("Resetting settings...")

        val settings = SettingsStorage
            .getInstance(project)
            .state

        settingsForm?.setTemplatesPath(settings.templatesPath)
    }
}
