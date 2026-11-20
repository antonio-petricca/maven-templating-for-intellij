package com.github.intellij.plugins.mt4ij.guide

/** What the "What's New" page shows, already localized. */
data class WhatsNewContent(
    /** The title of the page, the same as the one of its editor tab. */
    val title: String,
    val pluginName: String,
    val version: String,
    val intro: String,
    val about: String,
    val newsTitle: String,
    val notesHtml: String,
    val whereTitle: String,
    val wherePoints: List<String>,
    val footerLabel: String,
    val footerUrl: String,
    val iconSvg: String,
)

/** Builds the HTML of the pages opened in the editor tabs, from the IDE theme. Free of IDE APIs, so it can be tested. */
object WhatsNewPage {

    private val PAGE_STYLE = """
        .header { display: flex; align-items: center; gap: 20px; margin-bottom: 8px; }
        .header svg { width: 64px; height: 64px; flex: none; }
        .header h1 { margin: 0; font-size: 1.9em; font-weight: 600; }
        .header .version { margin: 2px 0 0; }
        .intro { font-size: 1.1em; margin: 20px 0 8px; }
        .about { margin: 0 0 20px; }
        .section h2 { margin: 0 0 4px; font-size: 1.15em; font-weight: 600; }
        .section ul { margin: 0 0 20px; padding-left: 22px; }
        .section li { margin: 8px 0; }
        .footer { margin-top: 40px; padding-top: 12px; border-top: 1px solid; font-size: .9em; }
    """.trimIndent()

    fun render(theme: GuideTheme, layoutCss: String, content: WhatsNewContent): String = listOf(
        "<!DOCTYPE html>",
        "<html><head><meta charset=\"UTF-8\"><title>${escape(content.title)}</title>",
        "<style>$layoutCss</style>",
        "<style>$PAGE_STYLE</style>",
        "<style>${theme.css()}</style>",
        "</head>",
        "<body>",
        "<div class=\"header\">",
        content.iconSvg,
        "<div><h1>${escape(content.pluginName)}</h1><p class=\"version muted\">${escape(content.version)}</p></div>",
        "</div>",
        "<p class=\"intro\">${escape(content.intro)}</p>",
        "<p class=\"about muted\">${escape(content.about)}</p>",
        "<div class=\"section news\"><h2>${escape(content.newsTitle)}</h2>${content.notesHtml}</div>",
        "<div class=\"section where\"><h2>${escape(content.whereTitle)}</h2><ul>${content.wherePoints.joinToString("") { "<li>${escape(it)}</li>" }}</ul></div>",
        "<p class=\"footer\"><a href=\"${escape(content.footerUrl)}\">${escape(content.footerLabel)}</a></p>",
        "</body></html>",
    ).joinToString("\n")

    /** The guide as built (layout only), with the colors and the font of the IDE appended to its head. */
    fun themedGuide(theme: GuideTheme, guideHtml: String): String =
        guideHtml.replace("</head>", "<style>${theme.css()}</style></head>")

    private fun escape(text: String) = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
