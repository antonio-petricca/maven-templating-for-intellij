package com.github.intellij.plugins.mt4ij.config

import com.github.intellij.plugins.mt4ij.ApiHelpers
import com.github.intellij.plugins.mt4ij.Bundle
import com.github.intellij.plugins.mt4ij.guide.GuideTabs
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel

internal class SettingsConfigurable(private val project: Project) :
    BoundSearchableConfigurable(Bundle.message("mt4ij.settings.name"), "mt4ij.settings", "mt4ij.settings")
{
    private var dialogPanel: DialogPanel? = null

    override fun createPanel(): DialogPanel = panel {
        SettingsForm.build(this, onOpenGuide = { GuideTabs.openGuide(project) }) { getState() }
    }.also { dialogPanel = it }

    override fun apply() {
        // Invalid values must not be saved: the validators of the form run before anything is applied
        dialogPanel?.validateAll()?.firstOrNull()?.let { throw ConfigurationException(it.message) }

        val previous = getState().copy()

        super.apply()

        val current = getState()

        if (current.templatesPath != previous.templatesPath) {
            // Folders with the old name are unmarked before the new ones are looked for
            ApiHelpers.scanProject(project, notify = true, previousName = previous.templatesPath)
        }

        // The excluded folders are declared at Maven import time: only a reimport applies the change
        if (current.excludeGeneratedSources != previous.excludeGeneratedSources) {
            ApiHelpers.suggestReimport(project)
        }
    }

    private fun getState(): SettingsState = SettingsStorage.getInstance(project).state
}
