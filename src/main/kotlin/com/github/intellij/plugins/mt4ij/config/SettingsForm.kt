package com.github.intellij.plugins.mt4ij.config

import com.github.intellij.plugins.mt4ij.Bundle
import com.github.intellij.plugins.mt4ij.importing.TemplatingFolders
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.layout.ValidationInfoBuilder

/** Builds the settings panel; the controls are bound straight to the project settings state. */
internal object SettingsForm {

    fun build(panel: Panel, state: () -> SettingsState) {
        panel.apply {
            row(Bundle.message("mt4ij.settings.label.path")) {
                textField()
                    .columns(COLUMNS_MEDIUM)
                    .comment(Bundle.message("mt4ij.settings.comment.path"))
                    .bindText({ state().templatesPath }, { state().templatesPath = it.trim() })
                    .validationOnInput { validateFolderName(this, it.text) }
                    .validationOnApply { validateFolderName(this, it.text) }
            }

            row {
                checkBox(Bundle.message("mt4ij.settings.label.scan-on-startup"))
                    .bindSelected({ state().scanOnStartup }, { state().scanOnStartup = it })
            }

            row {
                checkBox(Bundle.message("mt4ij.settings.label.exclude-generated"))
                    .comment(Bundle.message("mt4ij.settings.comment.exclude-generated"))
                    .bindSelected({ state().excludeGeneratedSources }, { state().excludeGeneratedSources = it })
            }
        }
    }

    private fun validateFolderName(builder: ValidationInfoBuilder, name: String): ValidationInfo? {
        return when {
            name.isBlank() -> builder.error(Bundle.message("mt4ij.settings.error.path.empty"))
            !TemplatingFolders.isValidName(name) -> builder.error(Bundle.message("mt4ij.settings.error.path.relative"))
            name != name.trim() -> builder.error(Bundle.message("mt4ij.settings.error.path.spaces"))
            name.contains('/') || name.contains('\\') ->
                builder.error(Bundle.message("mt4ij.settings.error.path.separator"))
            else -> null
        }
    }
}
