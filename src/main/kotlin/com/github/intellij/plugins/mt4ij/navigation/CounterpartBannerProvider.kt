package com.github.intellij.plugins.mt4ij.navigation

import com.github.intellij.plugins.mt4ij.Bundle
import com.github.intellij.plugins.mt4ij.MavenTemplatingIcons
import com.github.intellij.plugins.mt4ij.TemplateLocator
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import java.util.function.Function
import javax.swing.JComponent

/**
 * Banner on top of a template and of a generated source, with a link to the other one
 * (or a note that the template has no generated source yet).
 * Gutter icons are not drawn in the folders excluded from the project (such as target/), the banner always is.
 */
class CounterpartBannerProvider : EditorNotificationProvider, DumbAware {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?>? {
        val counterpart = TemplateLocator.counterpartOf(project, file)
            ?: return if (TemplateLocator.isTemplate(project, file)) notBuiltBanner() else null

        return Function { editor ->
            val panel = EditorNotificationPanel(editor, EditorNotificationPanel.Status.Info)

            panel.icon(counterpart.icon)
            panel.text = counterpart.bannerText
            panel.createActionLabel(counterpart.bannerLinkText) {
                // The build output may have changed since the banner was shown
                val current = TemplateLocator.counterpartOf(project, file)?.file?.takeIf { it.isValid }

                if (current != null) {
                    OpenFileDescriptor(project, current).navigate(true)
                } else {
                    EditorNotifications.getInstance(project).updateNotifications(file)
                }
            }

            panel
        }
    }

    // The generated source does not exist until the project is built: there is nothing to link to yet
    private fun notBuiltBanner() = Function<FileEditor, JComponent?> { editor ->
        EditorNotificationPanel(editor, EditorNotificationPanel.Status.Info).apply {
            icon(MavenTemplatingIcons.GoToGenerated)
            text = Bundle.message("mt4ij.navigation.banner.not-built")
        }
    }
}
