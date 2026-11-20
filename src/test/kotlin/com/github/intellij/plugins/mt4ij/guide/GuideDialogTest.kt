package com.github.intellij.plugins.mt4ij.guide

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class GuideDialogTest : BasePlatformTestCase() {

    fun testDialogShowsTheThemedGuide() {
        val html = checkNotNull(GuideTabs.themedGuide())

        ApplicationManager.getApplication().invokeAndWait {
            // The plain HTML pane is the path that works without an embedded browser
            val dialog = GuideDialog(project, html, useBrowser = false)

            try {
                val pane = checkNotNull(dialog.fallbackPane)

                assertEquals("text/html", pane.contentType)
                assertFalse(pane.isEditable)
                assertTrue(pane.text.contains("user guide"))
            } finally {
                Disposer.dispose(dialog.disposable)
            }
        }
    }

    fun testOnlyWebLinksOpenInTheSystemBrowser() {
        assertTrue(GuideDialog.isWebLink("https://example.org"))
        assertTrue(GuideDialog.isWebLink("http://example.org"))
        assertFalse(GuideDialog.isWebLink("about:blank"))
        assertFalse(GuideDialog.isWebLink("#section"))
    }
}
