package com.github.intellij.plugins.mt4ij.guide

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Color

/** Colors and font of the IDE, as the CSS of the pages opened in the editor tabs needs them. */
data class GuideTheme(
    val dark: Boolean,
    val background: String,
    val text: String,
    val secondaryText: String,
    val link: String,
    val border: String,
    val fontFamily: String,
    val fontSizePx: Int,
) {

    /** The only source of colors and font of the guide and of the "What's New" pages. */
    fun css(): String = """
        html { background: $background; }
        body { background: $background; color: $text; font-family: "$fontFamily", -apple-system, "Segoe UI", sans-serif; font-size: ${fontSizePx}px; }
        a { color: $link; }
        h1, h2, h3 { color: $text; }
        h2, th, td, blockquote { border-color: $border; }
        blockquote { color: $secondaryText; }
        .muted { color: $secondaryText; }
        .footer { border-color: $border; }
    """.trimIndent()

    companion object {

        private fun Color.css() = "#%02x%02x%02x".format(red, green, blue)

        /** The theme of the IDE as it is right now. */
        fun current(): GuideTheme {
            val label = UIUtil.getLabelFont()

            return GuideTheme(
                dark = !JBColor.isBright(),
                background = EditorColorsManager.getInstance().globalScheme.defaultBackground.css(),
                text = UIUtil.getLabelForeground().css(),
                secondaryText = UIUtil.getContextHelpForeground().css(),
                link = JBUI.CurrentTheme.Link.Foreground.ENABLED.css(),
                border = JBColor.border().css(),
                fontFamily = label.family,
                // The label font is sized for the IDE widgets: the text of a page reads better a little larger
                fontSizePx = label.size + 1,
            )
        }
    }
}
