package com.github.intellij.plugins.mt4ij

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/** What the user sees of the plugin in the IDE: its name, its pages and its menu icons. */
class PluginDescriptorTest : BasePlatformTestCase() {

    private fun resourceText(path: String) =
        checkNotNull(javaClass.getResourceAsStream(path)) { "$path is missing" }.use { String(it.readBytes(), Charsets.UTF_8) }

    fun testPluginIsNamedAfterItsDisplayNameNotAfterItsModule() {
        val name = Regex("<name>(.*?)</name>").find(resourceText("/META-INF/plugin.xml"))?.groupValues?.get(1)

        assertEquals("Maven Templating assistant", name)
    }

    fun testBundledGuidesHaveNoLinkToTheOnlineGuide() {
        listOf("/guide/guide.html", "/guide/guide_it.html").forEach {
            assertFalse(it, resourceText(it).contains("github.com/antonio-petricca/maven-templating-for-intellij/blob/main/docs/guide"))
        }
    }

    // The guide window uses the embedded browser, which is a plugin of its own since 2026.2: without this dependency
    // its classes are not visible to the plugin, the window fails to open, and neither the tests nor the verifier notice
    fun testPluginDependsOnTheEmbeddedBrowser() {
        assertTrue(resourceText("/META-INF/plugin.xml").contains("<depends>com.intellij.modules.jcef</depends>"))
    }

    fun testEveryActionAndGroupOfThePluginHasAnIcon() {
        val manager = ActionManager.getInstance()
        val ids     = manager.getActionIdList("mt4ij.")

        assertTrue(ids.isNotEmpty())
        ids.forEach {
            assertNotNull("$it has no icon", manager.getAction(it).templatePresentation.icon)
        }
        assertTrue(manager.getAction("mt4ij.groups.main") is ActionGroup)
    }

    fun testGuidePagesHaveTheTitleOfTheirTab() {
        assertTrue(resourceText("/guide/guide.html").contains("<title>Maven Templating assistant — Guide</title>"))
        assertTrue(resourceText("/guide/guide_it.html").contains("<title>Maven Templating assistant — Guida</title>"))
    }
}
