package com.github.intellij.plugins.mt4ij.guide

/** Why the "What's New" tab is shown: the plugin has just been installed, or it has been updated. */
enum class WhatsNewVisit { INSTALL, UPDATE }

/** Pure decisions about the guide and the "What's New" tab, kept apart from the IDE APIs so they can be tested. */
object GuideLogic {

    const val PLUGIN_INFO_RESOURCE = "/guide/plugin-info.properties"
    const val CSS_RESOURCE = "/guide/guide.css"

    private const val ITALIAN = "it"
    const val ICON_RESOURCE = "/META-INF/pluginIcon.svg"
    const val ICON_DARK_RESOURCE = "/META-INF/pluginIcon_dark.svg"

    private const val GITHUB = "https://github.com/antonio-petricca/maven-templating-for-intellij/blob/main/"

    /** Italian guide for the Italian language, the English one for any other. */
    fun guideResource(language: String): String =
        if (language == ITALIAN) "/guide/guide_it.html" else "/guide/guide.html"

    /** Italian change notes for the Italian language, the English ones for any other. */
    fun whatsNewResource(language: String): String =
        if (language == ITALIAN) "/guide/whats-new_it.html" else "/guide/whats-new.html"

    /** Changelog on GitHub, in the language of the IDE when there is a translation. */
    fun changelogUrl(language: String): String =
        GITHUB + if (language == ITALIAN) "CHANGELOG.it.md" else "CHANGELOG.md"

    /** The plugin icon for a dark or a light theme. */
    fun iconResource(dark: Boolean): String = if (dark) ICON_DARK_RESOURCE else ICON_RESOURCE

    /**
     * [WhatsNewVisit.INSTALL] on the first install (nothing stored yet), [WhatsNewVisit.UPDATE] whenever the installed
     * version differs from the stored one, null when there is nothing to show.
     */
    fun whatsNewVisit(installedVersion: String?, lastShownVersion: String, enabled: Boolean): WhatsNewVisit? = when {
        !enabled || installedVersion.isNullOrBlank() -> null
        lastShownVersion.isEmpty()                   -> WhatsNewVisit.INSTALL
        installedVersion != lastShownVersion         -> WhatsNewVisit.UPDATE
        else                                         -> null
    }
}
