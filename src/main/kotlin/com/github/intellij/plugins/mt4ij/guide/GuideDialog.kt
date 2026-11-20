package com.github.intellij.plugins.mt4ij.guide

import com.github.intellij.plugins.mt4ij.Bundle
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBUI
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest
import java.awt.Dimension
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.event.HyperlinkEvent

/*
    The bundled guide in a window of the plugin, not an online page. The page is shown by the embedded browser (JCEF)
    or, where the IDE has none, by a plain HTML pane. Links to the web open in the browser of the system.
 */
class GuideDialog(project: Project, private val html: String, useBrowser: Boolean = JBCefApp.isSupported()) :
    DialogWrapper(project, false, IdeModalityType.MODELESS)
{
    internal val fallbackPane: JEditorPane? = if (useBrowser) null else createFallbackPane()

    private val browser: JBCefBrowser? = if (useBrowser) createBrowser() else null

    init {
        title = Bundle.message("mt4ij.guide.tab.title")
        init()
    }

    override fun getDimensionServiceKey(): String = "mt4ij.guide.dialog"

    override fun createCenterPanel(): JComponent {
        val content = browser?.component ?: JBScrollPane(fallbackPane)

        return content.apply { preferredSize = Dimension(JBUI.scale(900), JBUI.scale(700)) }
    }

    override fun createActions(): Array<Action> = arrayOf(cancelAction.apply {
        putValue(Action.NAME, Bundle.message("mt4ij.guide.dialog.close"))
    })

    private fun createBrowser(): JBCefBrowser {
        val created = JBCefBrowser.create(JBCefBrowser.createBuilder().setOffScreenRendering(false))

        created.jbCefClient.addRequestHandler(object : CefRequestHandlerAdapter() {
            override fun onBeforeBrowse(
                browser: CefBrowser?,
                frame: CefFrame?,
                request: CefRequest?,
                userGesture: Boolean,
                isRedirect: Boolean,
            ): Boolean {
                val url = request?.url.orEmpty()

                if (userGesture && isWebLink(url)) {
                    BrowserUtil.browse(url)
                    return true
                }

                return false
            }
        }, created.cefBrowser)

        Disposer.register(disposable, created)
        created.loadHTML(html)

        return created
    }

    // Swing understands only old CSS and fails on the style sheets written for the browser: they are left out,
    // and the HTML kit of the IDE gives the page the fonts and colors of the theme
    private fun createFallbackPane() = JEditorPane().apply {
        editorKit = HTMLEditorKitBuilder().withWordWrapViewFactory().build()
        text = withoutStyleSheets(html)
        isEditable = false
        caretPosition = 0
        addHyperlinkListener {
            if (it.eventType == HyperlinkEvent.EventType.ACTIVATED && isWebLink(it.url?.toString().orEmpty())) {
                BrowserUtil.browse(it.url.toString())
            }
        }
    }

    companion object {

        // The window open for each project, forgotten when it is closed so that it never holds on to the project (EDT only)
        private val openWindows = mutableMapOf<Project, GuideDialog>()

        internal fun withoutStyleSheets(html: String): String =
            html.replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")

        fun isWebLink(url: String): Boolean = url.startsWith("http://") || url.startsWith("https://")

        /** Shows the guide; a window already open is brought to the front instead of opening a second one. */
        fun show(project: Project, html: String) {
            openWindows[project]?.takeIf { it.isShowing }?.let {
                it.window?.toFront()
                return
            }

            val dialog = GuideDialog(project, html)

            openWindows[project] = dialog
            Disposer.register(dialog.disposable, Disposable { openWindows.remove(project) })
            dialog.show()
        }
    }
}
