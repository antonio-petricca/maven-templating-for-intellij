package com.github.intellij.plugins.mt4ij.listeners

import com.github.intellij.plugins.mt4ij.navigation.GeneratedSourceGutterIcon
import com.github.intellij.plugins.mt4ij.SourceFolderChange
import com.github.intellij.plugins.mt4ij.SourceFolders
import com.github.intellij.plugins.mt4ij.config.SettingsStorage
import com.github.intellij.plugins.mt4ij.importing.TemplatingFolders
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.ui.EditorNotifications
import org.jetbrains.idea.maven.project.MavenProjectsManager
import java.util.concurrent.ConcurrentHashMap

/*
    // File system events listening

    https://intellij-support.jetbrains.com/hc/en-us/community/posts/360003028820-BulkFileListener-behavior-with-out-of-process-files

    // Manage source directories

    https://plugins.jetbrains.com/docs/intellij/module.html
    https://intellij-support.jetbrains.com/hc/en-us/community/posts/4413381081234-Mark-folder-as-source-or-test-root?page=1#community_comment_4414998507282
    https://intellij-support.jetbrains.com/hc/en-us/community/posts/206116979-How-to-set-Sources-Root-directory-and-Test-Sources-Root-directory?page=1#community_comment_206125705
    https://youtrack.jetbrains.com/issue/EDU-4505

    // Logging

    https://intellij-support.jetbrains.com/hc/en-us/community/posts/206779715-Proper-way-to-log-in-Idea-plugins
    https://stackoverflow.com/a/65852985/418599

    // Exceptions on source root modification "Assertion failed: Do not use API that changes roots from roots events. Try using invoke later or something else."

    https://youtrack.jetbrains.com/issue/EDU-4505
    https://youtrack.jetbrains.com/issue/IDEA-247362
    https://developerlife.com/2021/03/13/ij-idea-plugin-advanced/#invokelater-and-modalitystate
 */

internal class VFSListener(private val project: Project) : BulkFileListener {

    // Template folders about to be renamed or moved: the IDE carries their source root along to the new URL
    private val relocating = ConcurrentHashMap.newKeySet<VirtualFile>()

    override fun before(events: MutableList<out VFileEvent>) {
        // The folder is still at its old place: unmark it
        val changes = events.mapNotNull { event ->
            val file = event.file
            val isRemoval = when (event) {
                is VFileDeleteEvent, is VFileMoveEvent -> true
                is VFilePropertyChangeEvent -> event.isRename
                else -> false
            }

            val change = if (isRemoval && file != null) toChange(event.path, file, false) else null

            if (change != null && (event is VFileMoveEvent || event is VFilePropertyChangeEvent)) {
                relocating += file!!
            }

            change
        }

        scheduleUpdate(changes)
    }

    override fun after(events: MutableList<out VFileEvent>) {
        // The folder is now at its new place: mark it
        val changes = events.mapNotNull { event ->
            val file = when (event) {
                is VFileCreateEvent -> event.takeIf { it.isDirectory }?.file
                is VFileMoveEvent -> event.file
                is VFilePropertyChangeEvent -> event.takeIf { it.isRename }?.file
                is VFileCopyEvent -> event.newParent.findChild(event.newChildName)
                else -> null
            }

            file?.let { (toChange(it.path, it, true) ?: unmarkRelocated(event, it)).also { _ -> relocating.remove(it) } }
        }

        scheduleUpdate(changes)
        refreshBanners(events)
    }

    // Builds create and delete generated sources (or whole output folders), and the banners must follow
    private fun refreshBanners(events: List<VFileEvent>) {
        val isBuildOutputChange = events.any {
            (it is VFileCreateEvent || it is VFileDeleteEvent || it is VFileMoveEvent || it is VFileCopyEvent) &&
                (it.path.endsWith(".java") || (it as? VFileCreateEvent)?.isDirectory ?: true)
        }

        if (isBuildOutputChange && !project.isDisposed) {
            EditorNotifications.getInstance(project).updateAllNotifications()
            GeneratedSourceGutterIcon.refresh(project)
        }
    }

    // A template folder renamed or moved to a place that is not a templates folder must not stay a source root
    private fun unmarkRelocated(event: VFileEvent, file: VirtualFile): SourceFolderChange? {
        val wasTemplates = (event is VFileMoveEvent || event is VFilePropertyChangeEvent) && file in relocating
        val module = ProjectFileIndex.getInstance(project).getModuleForFile(file)

        return if (wasTemplates && file.isValid && module != null) {
            SourceFolderChange.remove(module, file, false)
        } else {
            null
        }
    }

    // Only directories matter; the templates folders are those of the Maven project containing them
    private fun toChange(path: String, file: VirtualFile, isAddition: Boolean): SourceFolderChange? {
        if (!file.isValid || !file.isDirectory) {
            return null
        }

        val fileIndex = ProjectFileIndex.getInstance(project)

        if (!fileIndex.isInContent(file)) {
            return null
        }

        val mavenProject = MavenProjectsManager.getInstanceIfCreated(project)?.findContainingProject(file) ?: return null
        val module       = fileIndex.getModuleForFile(file) ?: return null

        val templatesName = SettingsStorage.getInstance(project).state.templatesPath
        val folders       = TemplatingFolders.of(mavenProject, templatesName)
        val folderPath    = path.trimEnd('/')

        val isTest = when (folderPath) {
            in folders.sources     -> false
            in folders.testSources -> true
            else                   -> return null
        }

        return if (isAddition) {
            SourceFolderChange.add(module, file, isTest)
        } else {
            SourceFolderChange.remove(module, file, isTest)
        }
    }

    // Roots cannot be changed while VFS events are being dispatched
    private fun scheduleUpdate(changes: List<SourceFolderChange>) {
        if (changes.isNotEmpty()) {
            ApplicationManager.getApplication().invokeLater(
                { SourceFolders.apply(changes) },
                project.disposed
            )
        }
    }
}
