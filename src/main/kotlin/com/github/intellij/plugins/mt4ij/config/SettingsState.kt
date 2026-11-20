package com.github.intellij.plugins.mt4ij.config

/*
    https://kotlinlang.org/docs/data-classes.html#properties-declared-in-the-class-body

    New fields must have defaults, so that settings saved by older versions keep loading.
 */

data class SettingsState(
    var templatesPath: String = "java-templates",
    var scanOnStartup: Boolean = true,
    var excludeGeneratedSources: Boolean = true,
)
