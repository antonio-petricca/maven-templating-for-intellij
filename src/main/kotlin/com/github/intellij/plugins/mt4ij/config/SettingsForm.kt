package com.github.intellij.plugins.mt4ij.config

import com.github.intellij.plugins.mt4ij.Bundle
import com.github.intellij.plugins.mt4ij.importing.TemplatingFolders
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.MAX_LINE_LENGTH_WORD_WRAP
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.layout.ValidationInfoBuilder

/** Builds the settings panel; the controls are bound straight to the project settings state. */
internal object SettingsForm {

    fun build(
        panel: Panel,
        appState: () -> AppSettingsState = { AppSettings.getInstance().state },
        onOpenGuide: () -> Unit = {},
        state: () -> SettingsState,
    ) {
        panel.apply {
            // The label sits above the field and the comments wrap to the width of the page: on one line they
            // made the page wider than the settings window, which then cut the texts and scrolled sideways
            row {
                textField()
                    .label(Bundle.message("mt4ij.settings.label.path"), LabelPosition.TOP)
                    .columns(COLUMNS_MEDIUM)
                    .comment(Bundle.message("mt4ij.settings.comment.path"), MAX_LINE_LENGTH_WORD_WRAP)
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
                    .comment(Bundle.message("mt4ij.settings.comment.exclude-generated"), MAX_LINE_LENGTH_WORD_WRAP)
                    .bindSelected({ state().excludeGeneratedSources }, { state().excludeGeneratedSources = it })
            }

            row {
                checkBox(Bundle.message("mt4ij.settings.label.show-whats-new"))
                    .comment(Bundle.message("mt4ij.settings.comment.show-whats-new"), MAX_LINE_LENGTH_WORD_WRAP)
                    .bindSelected({ appState().showWhatsNew }, { appState().showWhatsNew = it })
            }

            row {
                link(Bundle.message("mt4ij.settings.link.guide")) { onOpenGuide() }
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
