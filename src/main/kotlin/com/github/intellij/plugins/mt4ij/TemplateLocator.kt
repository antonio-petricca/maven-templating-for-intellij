package com.github.intellij.plugins.mt4ij

import com.github.intellij.plugins.mt4ij.config.SettingsStorage
import com.github.intellij.plugins.mt4ij.importing.TemplatingFolders
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager
import java.io.File

/** The generated source of a template, or the template of a generated source. */
class Counterpart(val file: VirtualFile, val isGenerated: Boolean)

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
