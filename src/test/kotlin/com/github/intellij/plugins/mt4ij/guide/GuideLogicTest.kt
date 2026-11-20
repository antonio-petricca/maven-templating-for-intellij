package com.github.intellij.plugins.mt4ij.guide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideLogicTest {

    @Test
    fun firstInstallShowsWhatsNewAsAnInstall() {
        assertEquals(WhatsNewVisit.INSTALL, GuideLogic.whatsNewVisit("1.2.0", "", enabled = true))
    }

    @Test
    fun sameVersionIsNotShownAgain() {
        assertNull(GuideLogic.whatsNewVisit("1.2.0", "1.2.0", enabled = true))
    }

    @Test
    fun differentVersionShowsWhatsNewAsAnUpdate() {
        assertEquals(WhatsNewVisit.UPDATE, GuideLogic.whatsNewVisit("1.3.0", "1.2.0", enabled = true))
    }

    @Test
    fun disabledOptionNeverShowsWhatsNew() {
        assertNull(GuideLogic.whatsNewVisit("1.3.0", "1.2.0", enabled = false))
        assertNull(GuideLogic.whatsNewVisit("1.3.0", "", enabled = false))
    }

    @Test
    fun unknownInstalledVersionIsIgnored() {
        assertNull(GuideLogic.whatsNewVisit(null, "", enabled = true))
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

    @Test
    fun iconAndChangelogFollowTheThemeAndTheLanguage() {
        assertEquals("/META-INF/pluginIcon_dark.svg", GuideLogic.iconResource(dark = true))
        assertEquals("/META-INF/pluginIcon.svg", GuideLogic.iconResource(dark = false))
        assertNotNull(javaClass.getResource(GuideLogic.iconResource(dark = true)))
        assertNotNull(javaClass.getResource(GuideLogic.iconResource(dark = false)))
        assertTrue(GuideLogic.changelogUrl("it").endsWith("/CHANGELOG.it.md"))
        assertTrue(GuideLogic.changelogUrl("en").endsWith("/CHANGELOG.md"))
    }
}
