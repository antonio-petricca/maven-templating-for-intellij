package com.github.intellij.plugins.mt4ij.listeners;

import com.github.intellij.plugins.mt4ij.config.SettingsStorage
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
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

    // Getting project instance

    https://intellij-support.jetbrains.com/hc/en-us/community/posts/206763335-Getting-active-project-
    https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/openapi/vfs/newvfs/BulkFileListener.java
    https://developerlife.com/2021/03/13/ij-idea-plugin-advanced/

    // Exceptions on source root modification "Assertion failed: Do not use API that changes roots from roots events. Try using invoke later or something else."

    https://youtrack.jetbrains.com/issue/EDU-4505
    https://developerlife.com/2021/03/13/ij-idea-plugin-advanced/#invokelater-and-modalitystate
 */

internal class VFSListener : BulkFileListener {
    private val log : Logger = Logger.getInstance(VFSListener::class.java)

    private fun getContentEntry(model: ModifiableRootModel): ContentEntry? {
        val contentEntries = model.contentEntries
        return (if (contentEntries.isNotEmpty()) contentEntries[0] else null)
    }

    private fun getContentEntry(model: ModifiableRootModel, virtualFile: VirtualFile): ContentEntry? {
        val contentEntries = model.contentEntries
        return (if (contentEntries.isEmpty()) model.addContentEntry(virtualFile) else contentEntries[0])
    }

    private fun getModelForFile(project: Project, virtualFile: VirtualFile): ModifiableRootModel? {
        val fileIndex = ProjectFileIndex.SERVICE.getInstance(project)
        val module    = fileIndex.getModuleForFile(virtualFile)

        if (null != module) {
            if (module.moduleTypeName.equals("JAVA_MODULE")) {
                return ModuleRootManager
                    .getInstance(module)
                    .modifiableModel
            }
        }

        return null
    }

    private fun getActiveProject() : Project {
        val dataContext = DataManager.getInstance().dataContextFromFocusAsync.blockingGet(0)
        val project     = dataContext?.getData(CommonDataKeys.PROJECT.name) as Project

        if (null == project) {
            log.warn("Active project not found.")
        }

        return project
    }

    private fun invokeLater(runnable: Runnable, project: Project) {
        val application = ApplicationManager.getApplication()

        application.invokeLater(
            { application.runWriteAction(runnable) },
            project.disposed
        )
    }

    private fun invokeCommit(model: ModifiableRootModel, project: Project) {
        invokeLater(
            {
                model.commit()
            },
            project
        )
    }

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
                if (null != contentEntry) {
                    contentEntry
                        .sourceFolders
                        .filter { it.file?.equals(virtualFile) ?: false }
                        .forEach { sourceFolder ->
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
    }

    private fun doRemoveSourceFolder(project: Project, virtualFile: VirtualFile) {
        val model = getModelForFile(project, virtualFile)
        if (null != model) {
            val contentEntry = getContentEntry(model)
            if (null != contentEntry) {
                contentEntry
                    .sourceFolders
                    .filter { it.file?.equals(virtualFile) ?: false }
                    .forEach { sourceFolder ->
                        contentEntry.removeSourceFolder(sourceFolder)
                        invokeCommit(model, project)

                        log.info(String.format(
                            "Removed source folder: { virtualFile: \"%s\" }",
                            virtualFile.path
                        ))
                    }
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
                if (null != contentEntry) {
                    contentEntry
                        .sourceFolders
                        .filter { it.file?.equals(virtualFile) ?: false }
                        .forEach { sourceFolder ->
                            contentEntry.removeSourceFolder(sourceFolder)
                            model.commit()

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
    }

    override fun after(events: MutableList<out VFileEvent>) {
        val project = getActiveProject()

        if (null != project) {
            val templatesPath = SettingsStorage.getInstance(project).state.templatesPath
            val sourcesFolder = "main/${templatesPath}"
            val testsFolder = "test/${templatesPath}"

            events.forEach { event ->
                val path = event.path;
                val isSourceFolder = path.endsWith(sourcesFolder)
                val isTestFolder = path.endsWith(testsFolder)

                if (isSourceFolder || isTestFolder) {
                    if (
                        (event is VFileCreateEvent)
                        && (event.isDirectory)
                    ) {
                        doAddSourceFolder(project, event.file!!, isTestFolder)
                    } else if (event is VFileMoveEvent) {
                        doMoveSourceFolderAfter(project, templatesPath, event.newParent, isTestFolder)
                    } else if (
                        (event is VFilePropertyChangeEvent)
                        && (event.propertyName == "name")
                    ) {
                        doRenameSourceFolderAfter(project, event.newPath, isTestFolder)
                    }
                }
            }
        }

        super.after(events)
    }

    override fun before(events: MutableList<out VFileEvent>) {
        val project = getActiveProject()

        if (null != project) {
            val templatesPath = SettingsStorage.getInstance(project).state.templatesPath
            val sourcesFolder = "main/${templatesPath}"
            val testsFolder   = "test/${templatesPath}"

            events.forEach { event ->
                val path = event.path;
                val isSourceFolder = path.endsWith(sourcesFolder)
                val isTestFolder = path.endsWith(testsFolder)

                if (isSourceFolder || isTestFolder) {
                    if (event is VFileDeleteEvent) {
                        doRemoveSourceFolder(project, event.file)
                    } else if (event is VFileMoveEvent) {
                        doMoveSourceFolderBefore(project, templatesPath, event.oldParent)
                    } else if (
                        (event is VFilePropertyChangeEvent)
                        && (event.propertyName == "name")
                    ) {
                        doRenameSourceFolderBefore(project, event.oldPath)
                    }
                }
            }
        }

        super.before(events)
    }
}
