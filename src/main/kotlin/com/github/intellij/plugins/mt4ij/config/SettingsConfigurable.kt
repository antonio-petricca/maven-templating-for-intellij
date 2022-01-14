package com.github.intellij.plugins.mt4ij.config

import com.github.intellij.plugins.mt4ij.Bundle
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

/*
    // Swing

    https://stackoverflow.com/a/29258675/418599

    // Configuration UI

    https://intellij-support.jetbrains.com/hc/en-us/community/posts/360003316280-Adding-Your-Plugin-Setting-to-the-IDE-Settings-Dialog
    https://programtalk.com/vs/intellij-haxe/src/common/com/intellij/plugins/haxe/config/HaxeSettingsConfigurable.java/
 */

 //TODO Make label searchable

internal class SettingsConfigurable : Configurable {
    private val settingsForm = SettingsForm()

    override fun createComponent(): JComponent? {
        reset()
        settingsForm.setTemplatesPath(SettingsStorage.instance.state.templatesPath)

        return settingsForm.mainPanel
    }

    override fun isModified(): Boolean {
        return settingsForm.isModified(SettingsStorage.instance.state)
    }

    override fun apply() {
        SettingsStorage.instance.state.templatesPath = settingsForm.getTemplatesPath()!!
    }

    override fun getDisplayName(): String {
        return Bundle.message("mt4ij.settings.name")
    }
}
