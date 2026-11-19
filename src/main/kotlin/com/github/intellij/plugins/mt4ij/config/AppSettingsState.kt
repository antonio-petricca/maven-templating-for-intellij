package com.github.intellij.plugins.mt4ij.config

/** Application-level state; new fields must have defaults, so that older saved states keep loading. */
data class AppSettingsState(
    var showWhatsNew: Boolean = true,
)
