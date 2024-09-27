package com.github.jbij.plugins.mt4ij.config

import javax.swing.JPanel
import javax.swing.JTextField

internal class SettingsForm {
    var mainPanel     : JPanel?     = null
    var templatesPath : JTextField? = null

    fun getTemplatesPath() : String? {
        return templatesPath?.text?.trim()
    }

    fun isModified(settingsState: SettingsState) : Boolean {
        return (settingsState.templatesPath != getTemplatesPath())
    }

    fun setTemplatesPath(templatesPath: String) {
        this.templatesPath?.text = templatesPath.trim()
    }
}
