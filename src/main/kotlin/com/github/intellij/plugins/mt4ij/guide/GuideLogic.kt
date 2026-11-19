package com.github.intellij.plugins.mt4ij.guide

/** Pure decisions about the guide and the "What's New" tab, kept apart from the IDE APIs so they can be tested. */
object GuideLogic {

    const val PLUGIN_INFO_RESOURCE = "/guide/plugin-info.properties"
    const val CSS_RESOURCE = "/guide/guide.css"

    private const val ITALIAN = "it"
    private const val GITHUB_DOCS = "https://github.com/antonio-petricca/maven-templating-for-intellij/blob/main/docs/"

    /** Italian guide for the Italian language, the English one for any other. */
    fun guideResource(language: String): String =
        if (language == ITALIAN) "/guide/guide_it.html" else "/guide/guide.html"

    /** Italian change notes for the Italian language, the English ones for any other. */
    fun whatsNewResource(language: String): String =
        if (language == ITALIAN) "/guide/whats-new_it.html" else "/guide/whats-new.html"

    /** Online copy of the guide, for the places where the IDE cannot open the bundled one. */
    fun guideUrl(language: String): String =
        GITHUB_DOCS + if (language == ITALIAN) "guide.it.md" else "guide.md"

    /** True on the first install (nothing stored yet) and whenever the installed version differs from the stored one. */
    fun shouldShowWhatsNew(installedVersion: String?, lastShownVersion: String, enabled: Boolean): Boolean {
        if (!enabled || installedVersion.isNullOrBlank()) {
            return false
        }

        return installedVersion != lastShownVersion
    }
}
