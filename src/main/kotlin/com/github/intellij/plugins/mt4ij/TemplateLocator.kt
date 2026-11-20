package com.github.intellij.plugins.mt4ij

import com.github.intellij.plugins.mt4ij.config.SettingsStorage
import com.github.intellij.plugins.mt4ij.importing.TemplatingFolders
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager
import java.io.File
import javax.swing.Icon

/**
 * The generated source of a template, or the template of a generated source.
 * [isGenerated] tells which one [file] is; texts and icons describe the way to it.
 */
class Counterpart(val file: VirtualFile, val isGenerated: Boolean) {

    val icon: Icon       get() = if (isGenerated) MavenTemplatingIcons.GoToGenerated else MavenTemplatingIcons.GoToTemplate
    val gutterIcon: Icon get() = if (isGenerated) MavenTemplatingIcons.GoToGeneratedGutter else MavenTemplatingIcons.GoToTemplateGutter

    /** The shortcut of the navigation action, like "(Ctrl+Alt+Shift+Space)", or nothing when it has none. */
    private val shortcutHint: String
        get() = KeymapUtil.getFirstKeyboardShortcutText(GO_TO_COUNTERPART_ACTION_ID).let { if (it.isEmpty()) "" else " ($it)" }

    val actionText: String
        get() = Bundle.message(if (isGenerated) "mt4ij.navigation.go-to-generated" else "mt4ij.navigation.go-to-template")

    /** What the banner says about the file it is shown on, which is the opposite of [file]. */
    val bannerText: String
        get() = Bundle.message(if (isGenerated) "mt4ij.navigation.banner.template" else "mt4ij.navigation.banner.generated")

    val bannerLinkText: String
        get() = Bundle.message(if (isGenerated) "mt4ij.navigation.banner.open-generated" else "mt4ij.navigation.banner.open-template") + shortcutHint

    /** [actionText] with the shortcut, for the tooltip of the gutter icon. */
    val tooltipText: String
        get() = actionText + shortcutHint

    companion object {
        const val GO_TO_COUNTERPART_ACTION_ID = "mt4ij.actions.go-to-counterpart"
    }
}

/**
 * Tells whether a Java file is a template or a generated source of the templating-maven-plugin
 * and finds its counterpart. Every lookup exits quickly for files outside the templating folders.
 */
object TemplateLocator {

    fun mavenProjectOf(project: Project, file: VirtualFile): MavenProject? {
        if (file.extension != "java" || !file.isInLocalFileSystem) {
            return null
        }

        val manager = MavenProjectsManager.getInstanceIfCreated(project) ?: return null

        // Files under target/ are excluded from the modules, so the module based lookup finds nothing for them
        return manager.findContainingProject(file) ?: containingByPath(manager, file)
    }

    // The innermost Maven project whose directory contains the file
    private fun containingByPath(manager: MavenProjectsManager, file: VirtualFile): MavenProject? =
        manager.projects
            .filter { FileUtil.isAncestor(it.directoryPath.toString(), file.path, true) }
            .maxByOrNull { it.directoryPath.toString().length }

    fun isTemplate(project: Project, file: VirtualFile): Boolean {
        val mavenProject = mavenProjectOf(project, file) ?: return false
        val folders      = foldersOf(project, mavenProject)

        return relativeTo(file, folders.sources + folders.testSources) != null
    }

    fun counterpartOf(project: Project, file: VirtualFile): Counterpart? {
        val mavenProject = mavenProjectOf(project, file) ?: return null
        val folders      = foldersOf(project, mavenProject)

        relativeTo(file, folders.sources)?.let { return existing(it, folders.outputs, true) }
        relativeTo(file, folders.testSources)?.let { return existing(it, folders.testOutputs, true) }
        relativeTo(file, folders.outputs)?.let { return existing(it, folders.sources, false) }
        relativeTo(file, folders.testOutputs)?.let { return existing(it, folders.testSources, false) }

        return null
    }

    private fun foldersOf(project: Project, mavenProject: MavenProject): TemplatingFolders {
        val templatesName = SettingsStorage.getInstance(project).state.templatesPath

        return TemplatingFolders.of(mavenProject, templatesName)
    }

    // Path of the file below the first root containing it
    private fun relativeTo(file: VirtualFile, roots: List<String>): String? {
        val path = file.path

        // isAncestor ignores the case on case-insensitive file systems, and so the path lengths stay comparable
        return roots.firstOrNull { FileUtil.isAncestor(it, path, true) }?.let { path.substring(it.length + 1) }
    }

    // Counterparts are looked up on disk only: no refresh, so a missing build output simply gives null
    private fun existing(relativePath: String, roots: List<String>, isGenerated: Boolean): Counterpart? {
        roots.forEach { root ->
            VfsUtil.findFileByIoFile(File(root, relativePath), false)
                ?.takeIf { !it.isDirectory }
                ?.let { return Counterpart(it, isGenerated) }
        }

        return null
    }
}
