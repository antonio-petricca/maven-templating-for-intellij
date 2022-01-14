package com.github.mt4ij.config

import com.github.mt4ij.Bundle
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

/*
    // Swing

    https://stackoverflow.com/a/29258675/418599

    // Configuration UI

    https://intellij-support.jetbrains.com/hc/en-us/community/posts/360003316280-Adding-Your-Plugin-Setting-to-the-IDE-Settings-Dialog
    https://programtalk.com/vs/intellij-haxe/src/common/com/intellij/plugins/haxe/config/HaxeSettingsConfigurable.java/
 */

internal class SettingsConfigurable : Configurable {
    private val settingsForm = SettingsForm()

    private fun getSettings() : SettingsStorage {
        return SettingsStorage.instance
    }

    override fun createComponent(): JComponent? {
        reset()
        settingsForm.templatesPath?.text = getSettings().state.templatesPath //TODO Move to UI

        return settingsForm.mainPanel
    }

    override fun isModified(): Boolean {
        return (getSettings().state.templatesPath != settingsForm.templatesPath?.text?.trim()) //TODO Move to UI
    }

    override fun apply() {
        getSettings().state.templatesPath = settingsForm.templatesPath?.text?.trim().toString() //TODO Move to UI
    }

    override fun getDisplayName(): String {
        return Bundle.message("mt4ij.settings.name")
    }
}
