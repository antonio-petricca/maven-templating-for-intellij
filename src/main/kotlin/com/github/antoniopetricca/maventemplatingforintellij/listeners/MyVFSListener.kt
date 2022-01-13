package com.github.antoniopetricca.maventemplatingforintellij.listeners;

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
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
 */

//TODO Make it configurable (activation, folder)
//FIXME exceptions on source root modification ( https://youtrack.jetbrains.com/issue/EDU-4505 )

internal class MyVFSListener : BulkFileListener {
    private val TEMPLATES_FOLDER = "java-templates"
    private val SOURCES_FOLDER   = "main/${TEMPLATES_FOLDER}"
    private val TESTS_FOLDER     = "test/${TEMPLATES_FOLDER}"

    private val log : Logger = Logger.getInstance(MyVFSListener::class.java)

    private fun addSourceFolder(virtualFile: VirtualFile, isTestFolder: Boolean) {
        getOpenProjects()
            .forEach { project ->
                val model = getModelForFile(project, virtualFile)

                if (null != model) {
                    val contentEntry = getContentEntry(model, virtualFile)

                    if (null != contentEntry) {
                        val sourceRoots = model.getSourceRoots(if (!isTestFolder) SOURCE else TEST_SOURCE)

                        if (!sourceRoots.contains(virtualFile)) {
                            contentEntry.addSourceFolder(virtualFile, isTestFolder)
                            model.commit()

                            log.info(
                                String.format(
                                    "Added source folder: { virtualFile: \"%s\", isTestFolder: %s }",
                                    virtualFile.path,
                                    isTestFolder
                                )
                            )
                        }
                    }
                }
            }
    }

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

    private fun getOpenProjects(): Array<Project> {
        return ProjectManager
            .getInstance()
            .openProjects
    }

    private fun moveSourceFolderAfter(newParent: VirtualFile, isTestFolder: Boolean) {
        getOpenProjects()
            .forEach { project ->
                val virtualFile = VfsUtil.findFileByURL(URL("${newParent}/${TEMPLATES_FOLDER}"))

                if (null != virtualFile) {
                    val model = getModelForFile(project, virtualFile)

                    if (null != model) {
                        val contentEntry = getContentEntry(model, virtualFile)

                        if (null != contentEntry) {
                            val sourceRoots = model.getSourceRoots(if (!isTestFolder) SOURCE else TEST_SOURCE)

                            if (!sourceRoots.contains(virtualFile)) {
                                contentEntry.addSourceFolder(virtualFile, isTestFolder)
                                model.commit()

                                log.info(
                                    String.format(
                                        "Added source folder (moving): { virtualFile: \"%s\", isTestFolder: %s }",
                                        virtualFile.path,
                                        isTestFolder
                                    )
                                )
                            }
                        }
                    }
                }
            }
    }

    private fun moveSourceFolderBefore(oldParent: VirtualFile) {
        getOpenProjects()
            .forEach { project ->
                val virtualFile = VfsUtil.findFileByURL(URL("${oldParent}/${TEMPLATES_FOLDER}"))

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

                                    log.info(String.format(
                                        "Removed source folder (moving): { virtualFile: \"%s\" }",
                                        virtualFile.path
                                    ))
                                }
                        }
                    }
                }
            }
    }

    private fun removeSourceFolder(virtualFile: VirtualFile) {
        getOpenProjects()
            .forEach { project ->
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

                                log.info(String.format(
                                    "Removed source folder: { virtualFile: \"%s\" }",
                                    virtualFile.path
                                ))
                            }
                    }
                }
            }
    }

    private fun renameSourceFolderAfter(newPath: String, isTestFolder: Boolean) {
        getOpenProjects()
            .forEach { project ->
                val virtualFile = VfsUtil.findFileByIoFile(File(newPath), true)

                if (null != virtualFile) {
                    val model = getModelForFile(project, virtualFile)
                    if (null != model) {
                        val contentEntry = getContentEntry(model, virtualFile)

                        if (null != contentEntry) {
                            val sourceRoots = model.getSourceRoots(if (!isTestFolder) SOURCE else TEST_SOURCE)

                            if (!sourceRoots.contains(virtualFile)) {
                                contentEntry.addSourceFolder(virtualFile, isTestFolder)
                                model.commit()

                                log.info(
                                    String.format(
                                        "Added source folder (renaming): { virtualFile: \"%s\", isTestFolder: %s }",
                                        virtualFile.path,
                                        isTestFolder
                                    )
                                )
                            }
                        }
                    }
                }
            }
    }

    private fun renameSourceFolderBefore(oldPath: String) {
        getOpenProjects()
            .forEach { project ->
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

                                    log.info(String.format(
                                        "Removed source folder (renaming): { virtualFile: \"%s\" }",
                                        virtualFile.path
                                    ))
                                }
                        }
                    }
                }
            }
    }

    override fun after(events: MutableList<out VFileEvent>) {
        events.forEach { event ->
            val path           = event.path;
            val isSourceFolder = path.contains(SOURCES_FOLDER)
            val isTestFolder   = path.contains(TESTS_FOLDER)

            if (isSourceFolder || isTestFolder) {
                if (
                       (event is VFileCreateEvent)
                    && (event.isDirectory)
                ) {
                    addSourceFolder(event.file!!, isTestFolder)
                } else if (event is VFileMoveEvent) {
                    moveSourceFolderAfter(event.newParent, isTestFolder)
                } else if (
                       (event is VFilePropertyChangeEvent)
                    && (event.propertyName == "name")
                ) {
                    renameSourceFolderAfter(event.newPath, isTestFolder)
                }
            }
        }

        super.after(events)
    }

    override fun before(events: MutableList<out VFileEvent>) {
        events.forEach { event ->
            val path           = event.path;
            val isSourceFolder = path.contains(SOURCES_FOLDER)
            val isTestFolder   = path.contains(TESTS_FOLDER)

            if (isSourceFolder || isTestFolder) {
                if (event is VFileDeleteEvent) {
                    removeSourceFolder(event.file)
                } else if (event is VFileMoveEvent) {
                    moveSourceFolderBefore(event.oldParent)
                } else if (
                       (event is VFilePropertyChangeEvent)
                    && (event.propertyName == "name")
                ) {
                    renameSourceFolderBefore(event.oldPath)
                }
            }
        }

        super.before(events)
    }
}
