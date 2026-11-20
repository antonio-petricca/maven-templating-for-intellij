package com.github.intellij.plugins.mt4ij.guide

import com.github.intellij.plugins.mt4ij.Bundle
import com.github.intellij.plugins.mt4ij.Counterpart
import com.intellij.DynamicBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.Project
import java.nio.charset.StandardCharsets
import java.util.Properties

/*
    Opens the bundled guide and the "What's New" page in an IDE tab. Colors and font are those of the IDE theme
    when the tab is opened (see GuideTheme).
 */
object GuideTabs {

    private fun language(): String = DynamicBundle.getLocale().language

    // The version is generated at build time: the plugin manager APIs are internal
    private val info: Properties by lazy {
        Properties().apply {
            readResource(GuideLogic.PLUGIN_INFO_RESOURCE)?.let { load(it.reader()) }
        }
    }

    private fun pluginName(): String = Bundle.message("mt4ij.settings.name")

    fun installedVersion(): String? = info.getProperty("version")

    fun openGuide(project: Project) {
        val html = readResource(GuideLogic.guideResource(language())) ?: return

        openTab(project, Bundle.message("mt4ij.guide.tab.title"), WhatsNewPage.themedGuide(GuideTheme.current(), html))
    }

    fun openWhatsNew(project: Project, visit: WhatsNewVisit = WhatsNewVisit.UPDATE) {
        val theme = GuideTheme.current()
        val language = language()

        val content = WhatsNewContent(
            pluginName = pluginName(),
            version = installedVersion().orEmpty(),
            intro = Bundle.message(if (visit == WhatsNewVisit.INSTALL) "mt4ij.whats-new.intro.install" else "mt4ij.whats-new.intro"),
            about = Bundle.message("mt4ij.whats-new.about"),
            newsTitle = Bundle.message("mt4ij.whats-new.news.title", installedVersion().orEmpty()),
            notesHtml = readResource(GuideLogic.whatsNewResource(language)).orEmpty(),
            whereTitle = Bundle.message("mt4ij.whats-new.where.title"),
            wherePoints = wherePoints(),
            footerLabel = Bundle.message("mt4ij.whats-new.footer.changelog"),
            footerUrl = GuideLogic.changelogUrl(language),
            iconSvg = readResource(GuideLogic.iconResource(theme.dark)).orEmpty(),
        )

        val html = WhatsNewPage.render(theme, readResource(GuideLogic.CSS_RESOURCE).orEmpty(), content)

        openTab(project, Bundle.message("mt4ij.whats-new.tab.title", pluginName()), html)
    }

    // The shortcut is the one in the keymap of the user, not a text to keep up to date by hand
    private fun wherePoints(): List<String> {
        val shortcut = KeymapUtil.getFirstKeyboardShortcutText(Counterpart.GO_TO_COUNTERPART_ACTION_ID)
            .ifEmpty { Bundle.message("mt4ij.whats-new.shortcut.none") }

        return listOf(
            Bundle.message("mt4ij.whats-new.where.1"),
            Bundle.message("mt4ij.whats-new.where.2", shortcut),
            Bundle.message("mt4ij.whats-new.where.3"),
            Bundle.message("mt4ij.whats-new.where.4"),
        )
    }

    private fun openTab(project: Project, title: String, html: String) {
        ApplicationManager.getApplication().invokeLater({
            // A tab with the same title is brought to the front instead of opening a second one
            val manager = FileEditorManager.getInstance(project)
            val existing = manager.openFiles.firstOrNull { it.name == title }

            if (existing != null) {
                OpenFileDescriptor(project, existing).navigate(true)
            } else {
                val request = HTMLEditorProvider.Request.html(html)

                HTMLEditorProvider.openEditor(project, title, request)
            }
        }, project.disposed)
    }

    private fun readResource(path: String): String? =
        GuideTabs::class.java.getResourceAsStream(path)?.use { String(it.readAllBytes(), StandardCharsets.UTF_8) }
}
