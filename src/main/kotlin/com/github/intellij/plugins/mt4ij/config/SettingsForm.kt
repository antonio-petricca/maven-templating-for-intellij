package com.github.intellij.plugins.mt4ij.config

import com.github.intellij.plugins.mt4ij.Bundle
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.util.ui.JBUI
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

internal class SettingsForm {
    var mainPanel: JPanel = JPanel(
        GridLayoutManager(
            3,
            2,
            JBUI.emptyInsets(),
            -1,
            -1
        )
    )

    private var templatesPath: JTextField = JTextField()

    init {
        val label = JLabel(Bundle.message("mt4ij.settings.label.path"))
        val hSpacer = JPanel()
        val vSpacer = JPanel()

        mainPanel.add(label, GridConstraints().apply {
            row = 0
            column = 0
            anchor = GridConstraints.ANCHOR_WEST
        })

        mainPanel.add(templatesPath, GridConstraints().apply {
            row = 1
            column = 0
            fill = GridConstraints.FILL_HORIZONTAL
            hSizePolicy = GridConstraints.SIZEPOLICY_WANT_GROW
        })

        mainPanel.add(hSpacer, GridConstraints().apply {
            row = 0
            column = 1
            hSizePolicy = GridConstraints.SIZEPOLICY_WANT_GROW
        })

        mainPanel.add(vSpacer, GridConstraints().apply {
            row = 2
            column = 0
            vSizePolicy = GridConstraints.SIZEPOLICY_WANT_GROW
        })
    }

    fun getTemplatesPath(): String? {
        return templatesPath.text?.trim()
    }

    fun isModified(settingsState: SettingsState): Boolean {
        return (settingsState.templatesPath != getTemplatesPath())
    }

    fun setTemplatesPath(templatesPath: String) {
        this.templatesPath.text = templatesPath.trim()
    }
}
