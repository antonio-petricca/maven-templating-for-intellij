package com.github.intellij.plugins.mt4ij.config

import com.intellij.openapi.options.ConfigurationException
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.UIUtil

class SettingsConfigurableTest : BasePlatformTestCase() {

    private lateinit var configurable: SettingsConfigurable
    private lateinit var nameField: JBTextField

    override fun setUp() {
        super.setUp()

        configurable = SettingsConfigurable(project)
        nameField = UIUtil.findComponentOfType(configurable.createComponent(), JBTextField::class.java)!!
    }

    override fun tearDown() {
        try {
            configurable.disposeUIResources()
            SettingsStorage.getInstance(project).state.templatesPath = SettingsState().templatesPath
        } finally {
            super.tearDown()
        }
    }

    private fun applyName(name: String) {
        nameField.text = name
        configurable.apply()
    }

    fun testInvalidNamesAreRejected() {
        listOf("", "   ", ".", "..", "a/b", "a\\b", " tpl").forEach { name ->
            try {
                applyName(name)
                fail("[$name] must be rejected")
            } catch (expected: ConfigurationException) {
                assertEquals("java-templates", SettingsStorage.getInstance(project).state.templatesPath)
            }
        }
    }

    fun testGeneratedSourcesAreExcludedByDefault() {
        assertTrue(SettingsState().excludeGeneratedSources)
    }

    fun testValidNameIsSaved() {
        applyName("tpl")

        assertEquals("tpl", SettingsStorage.getInstance(project).state.templatesPath)
    }
}
