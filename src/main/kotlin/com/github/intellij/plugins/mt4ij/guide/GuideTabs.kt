package com.github.intellij.plugins.mt4ij.guide

import com.github.intellij.plugins.mt4ij.Bundle
import com.intellij.DynamicBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider
import com.intellij.openapi.project.Project
import java.nio.charset.StandardCharsets
import java.util.Properties

/*
    Opens the bundled guide and the "What's New" page in an IDE tab.

    The plain HTMLEditorProvider.openEditor(project, title, html) cannot react to link clicks, so the pages go
    through a Request with a JsQueryHandler: the "Open the guide" link calls the JS function named by
    HTMLEditorProvider.JS_FUNCTION_NAME and the handler opens the guide bundled in the plugin.
 */
object GuideTabs {

    private const val OPEN_GUIDE_QUERY = "open-guide"

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

        openTab(project, Bundle.message("mt4ij.guide.tab.title"), html)
    }

    fun openWhatsNew(project: Project) {
        val css = readResource(GuideLogic.CSS_RESOURCE).orEmpty()
        val notes = readResource(GuideLogic.whatsNewResource(language())).orEmpty()
        val heading = Bundle.message("mt4ij.whats-new.heading", pluginName(), installedVersion().orEmpty())

        val html = listOf(
            "<!DOCTYPE html>",
            "<html><head><meta charset=\"UTF-8\"><style>$css</style></head>",
            "<body>",
            "<h1>$heading</h1>",
            notes,
            "<p><a href=\"#\" onclick=\"${HTMLEditorProvider.JS_FUNCTION_NAME}('$OPEN_GUIDE_QUERY'); return false;\">" +
                Bundle.message("mt4ij.whats-new.link.guide") + "</a></p>",
            "</body></html>",
        ).joinToString("\n")

        openTab(project, Bundle.message("mt4ij.whats-new.tab.title", pluginName()), html, openGuideHandler(project))
    }

    private fun openGuideHandler(project: Project) = object : HTMLEditorProvider.JsQueryHandler {

        override suspend fun query(id: Long, request: String): String {
            if (request == OPEN_GUIDE_QUERY) {
                ApplicationManager.getApplication().invokeLater({ openGuide(project) }, project.disposed)
            }

            return ""
        }
    }

    private fun openTab(project: Project, title: String, html: String, handler: HTMLEditorProvider.JsQueryHandler? = null) {
        ApplicationManager.getApplication().invokeLater({
            // A tab with the same title is brought to the front instead of opening a second one
            val manager = FileEditorManager.getInstance(project)
            val existing = manager.openFiles.firstOrNull { it.name == title }

            if (existing != null) {
                OpenFileDescriptor(project, existing).navigate(true)
            } else {
                val request = HTMLEditorProvider.Request.html(html).let { if (handler == null) it else it.withQueryHandler(handler) }

                HTMLEditorProvider.openEditor(project, title, request)
            }
        }, project.disposed)
    }

    private fun readResource(path: String): String? =
        GuideTabs::class.java.getResourceAsStream(path)?.use { String(it.readAllBytes(), StandardCharsets.UTF_8) }
}
