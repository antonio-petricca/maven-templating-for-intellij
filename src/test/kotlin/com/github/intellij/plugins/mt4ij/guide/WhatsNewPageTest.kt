package com.github.intellij.plugins.mt4ij.guide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewPageTest {

    private val theme = GuideTheme(
        dark = true,
        background = "#1e1f22",
        text = "#dfe1e5",
        secondaryText = "#868a91",
        link = "#548af7",
        border = "#393b40",
        fontFamily = "Inter",
        fontSizePx = 14,
    )

    private val content = WhatsNewContent(
        pluginName = "Maven Templating assistant",
        version = "2.0.0",
        intro = "Thanks for updating! Here is what's new in this version.",
        about = "Keeps the template folders marked as sources.",
        newsTitle = "What's new in 2.0.0",
        notesHtml = "<ul><li>First change</li></ul>",
        whereTitle = "Where to find the features",
        wherePoints = listOf("Right-click, then Maven Templating.", "Tools > Maven <Templating>"),
        footerLabel = "Full changelog on GitHub",
        footerUrl = "https://example.org/CHANGELOG.md",
        iconSvg = "<svg id=\"icon\"></svg>",
    )

    private val page = WhatsNewPage.render(theme, "body { margin: 0; }", content)

    @Test
    fun pageHasIconNameVersionIntroAndNotes() {
        listOf(
            "<svg id=\"icon\"></svg>", "Maven Templating assistant", "2.0.0",
            "Thanks for updating! Here is what's new in this version.", "Keeps the template folders marked as sources.", "<li>First change</li>",
        ).forEach { assertTrue(it, page.contains(it)) }
    }

    @Test
    fun pageHasTheWhereToFindTheFeaturesSectionWithEscapedPoints() {
        assertTrue(page.contains("<h2>Where to find the features</h2>"))
        assertTrue(page.contains("<li>Right-click, then Maven Templating.</li>"))
        assertTrue(page.contains("<li>Tools &gt; Maven &lt;Templating&gt;</li>"))
    }

    @Test
    fun newsComeRightAfterTheIntroAndTheAboutAndBeforeTheWhereSection() {
        val positions = listOf("Thanks for updating", "Keeps the template folders", "<h2>What's new in 2.0.0</h2>", "<li>First change</li>", "<h2>Where to find the features</h2>")
            .map { page.indexOf(it).also { index -> assertTrue(it, index >= 0) } }

        assertEquals(positions.sorted(), positions)
    }

    @Test
    fun bothListsShareTheSameSectionStyle() {
        assertTrue(page.contains("<div class=\"section news\">"))
        assertTrue(page.contains("<div class=\"section where\">"))
        assertTrue(page.contains(".section ul {"))
    }

    @Test
    fun pageUsesTheColorsAndFontOfTheTheme() {
        listOf("#1e1f22", "#dfe1e5", "#868a91", "#548af7", "#393b40", "\"Inter\"", "14px").forEach {
            assertTrue(it, page.contains(it))
        }
    }

    @Test
    fun pageHasNoLinkToTheGuideAndLinksTheChangelog() {
        assertFalse(page.contains("onclick"))
        assertTrue(page.contains("<a href=\"https://example.org/CHANGELOG.md\">Full changelog on GitHub</a>"))
    }

    @Test
    fun textsAreEscaped() {
        val escaped = WhatsNewPage.render(theme, "", content.copy(pluginName = "A <b> & \"C\""))

        assertTrue(escaped.contains("A &lt;b&gt; &amp; &quot;C&quot;"))
    }

    @Test
    fun guideGetsTheThemeInItsHead() {
        val themed = WhatsNewPage.themedGuide(theme, "<html><head></head><body>x</body></html>")

        assertTrue(themed.contains("#1e1f22"))
        assertEquals(1, Regex("</head>").findAll(themed).count())
    }
}
