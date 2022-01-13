package com.github.mt4ij.config

import com.github.mt4ij.Bundle
import com.github.mt4ij.listeners.SettingsForm
import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent

/*
    // Swing

    https://stackoverflow.com/a/29258675/418599

    // Configuration UI

    https://intellij-support.jetbrains.com/hc/en-us/community/posts/360003316280-Adding-Your-Plugin-Setting-to-the-IDE-Settings-Dialog
    https://programtalk.com/vs/intellij-haxe/src/common/com/intellij/plugins/haxe/config/HaxeSettingsConfigurable.java/
    https://programtalk.com/vs/?source=intellij-haxe/src/common/com/intellij/plugins/haxe/config/HaxeProjectSettings.java
 */

class SettingsConfigurable : SearchableConfigurable {
    private val settingsForm = SettingsForm()

    override fun createComponent(): JComponent? {
        return settingsForm.getMainPanel()
    }

    override fun isModified(): Boolean {
        TODO("Not yet implemented")
    }

    override fun apply() {
        TODO("Not yet implemented")
    }

    override fun getDisplayName(): String {
        return Bundle.message("mt4ij.settings.name")
    }

    override fun getId(): String {
        return "mt4ij.settings.id"
    }
}
