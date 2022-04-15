package com.github.intellij.plugins.mt4ij.listeners

import com.github.intellij.plugins.mt4ij.PluginHelpers.Companion.getContentEntry
import com.github.intellij.plugins.mt4ij.PluginHelpers.Companion.getModelForFile
import com.github.intellij.plugins.mt4ij.PluginHelpers.Companion.invokeCommit
import com.github.intellij.plugins.mt4ij.config.SettingsStorage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*
import org.jetbrains.jps.model.java.JavaSourceRootType.SOURCE
import org.jetbrains.jps.model.java.JavaSourceRootType.TEST_SOURCE
import java.io.File
import java.net.URL

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

internal class VFSListener(project: Project) : BulkFileListener {
    private val log : Logger = Logger.getInstance(VFSListener::class.java)
    private val projectRef   = project

    private fun doAddSourceFolder(project: Project, virtualFile: VirtualFile, isTestFolder: Boolean) {
        val model = getModelForFile(project, virtualFile)

        if (null != model) {
            val contentEntry = getContentEntry(model, virtualFile)

            if (null != contentEntry) {
                val sourceRoots = model.getSourceRoots(if (!isTestFolder) SOURCE else TEST_SOURCE)

                if (!sourceRoots.contains(virtualFile)) {
                    contentEntry.addSourceFolder(virtualFile, isTestFolder)
                    invokeCommit(model, project)

                    log.info(String.format(
                        "Added source folder: { virtualFile: \"%s\", isTestFolder: %s }",
                        virtualFile.path,
                        isTestFolder
                    ))
                }
            }
        }
    }

    private fun doMoveSourceFolderAfter(project: Project, templatesPath: String, newParent: VirtualFile, isTestFolder: Boolean) {
        val virtualFile = VfsUtil.findFileByURL(URL("${newParent}/${templatesPath}"))

        if (null != virtualFile) {
            val model = getModelForFile(project, virtualFile)

            if (null != model) {
                val contentEntry = getContentEntry(model, virtualFile)

                if (null != contentEntry) {
                    val sourceRoots = model.getSourceRoots(if (!isTestFolder) SOURCE else TEST_SOURCE)

                    if (!sourceRoots.contains(virtualFile)) {
                        contentEntry.addSourceFolder(virtualFile, isTestFolder)
                        invokeCommit(model, project)

                        log.info(String.format(
                            "Added source folder (moving): { virtualFile: \"%s\", isTestFolder: %s }",
                            virtualFile.path,
                            isTestFolder
                        ))
                    }
                }
            }
        }
    }

    private fun doMoveSourceFolderBefore(project: Project, templatesPath: String, oldParent: VirtualFile) {
        val virtualFile = VfsUtil.findFileByURL(URL("${oldParent}/${templatesPath}"))

        if (null != virtualFile) {
            val model = getModelForFile(project, virtualFile)

            if (null != model) {
                val contentEntry = getContentEntry(model)

                contentEntry
                    ?.sourceFolders
                    ?.filter { it.file?.equals(virtualFile) ?: false }
                    ?.forEach { sourceFolder ->
                        contentEntry.removeSourceFolder(sourceFolder)
                        invokeCommit(model, project)

                        log.info(String.format(
                            "Removed source folder (moving): { virtualFile: \"%s\" }",
                            virtualFile.path
                        ))
                    }
            }
        }
    }

    private fun doRemoveSourceFolder(project: Project, virtualFile: VirtualFile) {
        val model = getModelForFile(project, virtualFile)

        if (null != model) {
            val contentEntry = getContentEntry(model)

            contentEntry
                ?.sourceFolders
                ?.filter { it.file?.equals(virtualFile) ?: false }
                ?.forEach { sourceFolder ->
                    contentEntry.removeSourceFolder(sourceFolder)
                    invokeCommit(model, project)

                    log.info(String.format(
                        "Removed source folder: { virtualFile: \"%s\" }",
                        virtualFile.path
                    ))
                }
        }
    }

    private fun doRenameSourceFolderAfter(project: Project, newPath: String, isTestFolder: Boolean) {
        val virtualFile = VfsUtil.findFileByIoFile(File(newPath), true)

        if (null != virtualFile) {
            val model = getModelForFile(project, virtualFile)

            if (null != model) {
                val contentEntry = getContentEntry(model, virtualFile)

                if (null != contentEntry) {
                    val sourceRoots = model.getSourceRoots(if (!isTestFolder) SOURCE else TEST_SOURCE)

                    if (!sourceRoots.contains(virtualFile)) {
                        contentEntry.addSourceFolder(virtualFile, isTestFolder)
                        invokeCommit(model, project)

                        log.info(String.format(
                            "Added source folder (renaming): { virtualFile: \"%s\", isTestFolder: %s }",
                            virtualFile.path,
                            isTestFolder
                        ))
                    }
                }
            }
        }
    }

    private fun doRenameSourceFolderBefore(project: Project, oldPath: String) {
        val virtualFile = VfsUtil.findFileByIoFile(File(oldPath), true)
        if (null != virtualFile) {
            val model = getModelForFile(project, virtualFile)

            if (null != model) {
                val contentEntry = getContentEntry(model)

                contentEntry
                    ?.sourceFolders
                    ?.filter { it.file?.equals(virtualFile) ?: false }
                    ?.forEach { sourceFolder ->
                        contentEntry.removeSourceFolder(sourceFolder)
                        invokeCommit(model, project)

                        log.info(
                            String.format(
                                "Removed source folder (renaming): { virtualFile: \"%s\" }",
                                virtualFile.path
                            )
                        )
                    }
            }
        }
    }

    override fun after(events: MutableList<out VFileEvent>) {
        val templatesPath = SettingsStorage.getInstance(projectRef).state.templatesPath
        val sourcesFolder = "main/${templatesPath}"
        val testsFolder = "test/${templatesPath}"

        events.forEach { event ->
            val path = event.path
            val isSourceFolder = path.endsWith(sourcesFolder)
            val isTestFolder = path.endsWith(testsFolder)

            if (isSourceFolder || isTestFolder) {
                if (
                    (event is VFileCreateEvent)
                    && (event.isDirectory)
                ) {
                    doAddSourceFolder(projectRef, event.file!!, isTestFolder)
                } else if (event is VFileMoveEvent) {
                    doMoveSourceFolderAfter(projectRef, templatesPath, event.newParent, isTestFolder)
                } else if (
                    (event is VFilePropertyChangeEvent)
                    && (event.propertyName == "name")
                ) {
                    doRenameSourceFolderAfter(projectRef, event.newPath, isTestFolder)
                }
            }
        }

        super.after(events)
    }

    override fun before(events: MutableList<out VFileEvent>) {
        val templatesPath = SettingsStorage.getInstance(projectRef).state.templatesPath
        val sourcesFolder = "main/${templatesPath}"
        val testsFolder   = "test/${templatesPath}"

        events.forEach { event ->
            val path = event.path
            val isSourceFolder = path.endsWith(sourcesFolder)
            val isTestFolder = path.endsWith(testsFolder)

            if (isSourceFolder || isTestFolder) {
                if (event is VFileDeleteEvent) {
                    doRemoveSourceFolder(projectRef, event.file)
                } else if (event is VFileMoveEvent) {
                    doMoveSourceFolderBefore(projectRef, templatesPath, event.oldParent)
                } else if (
                    (event is VFilePropertyChangeEvent)
                    && (event.propertyName == "name")
                ) {
                    doRenameSourceFolderBefore(projectRef, event.oldPath)
                }
            }
        }

        super.before(events)
    }
}
