package com.github.intellij.plugins.mt4ij.guide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideLogicTest {

    @Test
    fun firstInstallShowsWhatsNew() {
        assertTrue(GuideLogic.shouldShowWhatsNew("1.2.0", "", enabled = true))
    }

    @Test
    fun sameVersionIsNotShownAgain() {
        assertFalse(GuideLogic.shouldShowWhatsNew("1.2.0", "1.2.0", enabled = true))
    }

    @Test
    fun differentVersionShowsWhatsNew() {
        assertTrue(GuideLogic.shouldShowWhatsNew("1.3.0", "1.2.0", enabled = true))
    }

    @Test
    fun disabledOptionNeverShowsWhatsNew() {
        assertFalse(GuideLogic.shouldShowWhatsNew("1.3.0", "1.2.0", enabled = false))
        assertFalse(GuideLogic.shouldShowWhatsNew("1.3.0", "", enabled = false))
    }

    @Test
    fun unknownInstalledVersionIsIgnored() {
        assertFalse(GuideLogic.shouldShowWhatsNew(null, "", enabled = true))
    }

    @Test
    fun italianGuideIsChosenForItalian() {
        assertEquals("/guide/guide_it.html", GuideLogic.guideResource("it"))
        assertTrue(GuideLogic.guideUrl("it").endsWith("/guide.it.md"))
    }

    @Test
    fun englishGuideIsChosenForAnyOtherLanguage() {
        listOf("en", "de", "").forEach {
            assertEquals("/guide/guide.html", GuideLogic.guideResource(it))
            assertTrue(GuideLogic.guideUrl(it).endsWith("/guide.md"))
        }
    }

    @Test
    fun bundledGuidesAreOnTheClasspath() {
        listOf("en", "it").forEach {
            assertNotNull(GuideLogic.guideResource(it), javaClass.getResource(GuideLogic.guideResource(it)))
        }
    }

    @Test
    fun pluginInfoAndChangeNotesAreOnTheClasspath() {
        val info = java.util.Properties().apply {
            // Not javaClass: inside apply it would be java.util.Properties, loaded by the bootstrap class loader
            GuideLogicTest::class.java.getResourceAsStream(GuideLogic.PLUGIN_INFO_RESOURCE)!!.use { load(it.reader(Charsets.UTF_8)) }
        }

        assertFalse(info.getProperty("version").isNullOrBlank())
        listOf("en", "it").forEach {
            assertNotNull(javaClass.getResource(GuideLogic.whatsNewResource(it)))
        }

        assertNotNull(javaClass.getResource(GuideLogic.CSS_RESOURCE))
    }

    @Test
    fun whatsNewNotesFollowTheLanguage() {
        assertEquals("/guide/whats-new_it.html", GuideLogic.whatsNewResource("it"))
        assertEquals("/guide/whats-new.html", GuideLogic.whatsNewResource("en"))
        assertEquals("/guide/whats-new.html", GuideLogic.whatsNewResource("fr"))
    }
}
