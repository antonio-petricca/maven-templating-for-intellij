package com.github.intellij.plugins.mt4ij

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/** What the user sees of the plugin in the IDE. */
class PluginDescriptorTest : BasePlatformTestCase() {

    private fun resourceText(path: String) =
        checkNotNull(javaClass.getResourceAsStream(path)) { "$path is missing" }.use { String(it.readBytes(), Charsets.UTF_8) }

    fun testPluginIsNamedAfterItsDisplayNameNotAfterItsModule() {
        val name = Regex("<name>(.*?)</name>").find(resourceText("/META-INF/plugin.xml"))?.groupValues?.get(1)

        assertEquals("Maven Templating assistant", name)
    }

    fun testGuidePagesHaveTheTitleOfTheirTab() {
        assertTrue(resourceText("/guide/guide.html").contains("<title>Maven Templating assistant — Guide</title>"))
        assertTrue(resourceText("/guide/guide_it.html").contains("<title>Maven Templating assistant — Guida</title>"))
    }
}
