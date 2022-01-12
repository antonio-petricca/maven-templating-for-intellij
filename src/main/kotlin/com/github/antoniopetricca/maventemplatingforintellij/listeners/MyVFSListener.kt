package com.github.antoniopetricca.maventemplatingforintellij.listeners;

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import org.jetbrains.jps.model.java.JavaSourceRootType.SOURCE
import org.jetbrains.jps.model.java.JavaSourceRootType.TEST_SOURCE
import java.net.URL

/*
    https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html#defining-application-level-listeners
    https://intellij-support.jetbrains.com/hc/en-us/community/posts/206768495-Mark-directory-as-sources-root-from-plugin
    https://intellij-support.jetbrains.com/hc/en-us/community/posts/360009670979-Mark-directory-as-excluded-from-BulkFileListener
    https://intellij-support.jetbrains.com/hc/en-us/community/posts/360003028820-BulkFileListener-behavior-with-out-of-process-files
    https://intellij-support.jetbrains.com/hc/en-us/community/posts/360008137000-Mark-directory-as-excluded-programmatically
    https://hub.jetbrains.com/auth/login?response_type=code&client_id=0-0-0-0-0&redirect_uri=https:%2F%2Fhub.jetbrains.com%2Fapi%2Frest%2Fsaml2%2Foauth&scope=0-0-0-0-0&state=ysMztDui

    https://intellij-support.jetbrains.com/hc/en-us/community/posts/4413381081234-Mark-folder-as-source-or-test-root?page=1#community_comment_4414998507282
    https://intellij-support.jetbrains.com/hc/en-us/community/posts/206116979-How-to-set-Sources-Root-directory-and-Test-Sources-Root-directory?page=1#community_comment_206125705
    https://plugins.jetbrains.com/docs/intellij/module.html
 */

//TODO Make it configurable (activation, folder)
//TODO IDE logging

internal class MyVFSListener : BulkFileListener {

    private val TEMPLATES_FOLDER = "java-templates"
    private val SOURCES_FOLDER   = "main/${TEMPLATES_FOLDER}"
    private val TESTS_FOLDER     = "test/${TEMPLATES_FOLDER}"

    private fun addSourceFolder(virtualFile: VirtualFile, isTestFolder: Boolean) {
        getOpenProjects()
            .forEach { project ->
                val model = getModelForFile(project, virtualFile)

                if (null != model) {
                    val contentEntries = model.contentEntries
                    val contentEntry   = (if (contentEntries.isEmpty()) model.addContentEntry(virtualFile) else contentEntries[0])
                    val sourceRoots    = model.getSourceRoots(if (!isTestFolder) SOURCE else TEST_SOURCE)

                    if (!sourceRoots.contains(virtualFile)) {
                        contentEntry.addSourceFolder(virtualFile, isTestFolder)
                        model.commit()
                    }
                }
            }
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
                        val contentEntries = model.contentEntries
                        val contentEntry   = (if (contentEntries.isEmpty()) model.addContentEntry(virtualFile) else contentEntries[0])
                        val sourceRoots    = model.getSourceRoots(if (!isTestFolder) SOURCE else TEST_SOURCE)

                        if (!sourceRoots.contains(virtualFile)) {
                            contentEntry.addSourceFolder(virtualFile, isTestFolder)
                            model.commit()
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
                        val contentEntries = model.contentEntries

                        if (contentEntries.isNotEmpty()) {
                            val contentEntry = contentEntries[0]

                            contentEntry
                                .sourceFolders
                                .filter { it.file?.equals(virtualFile) ?: false }
                                .forEach { sourceFolder ->
                                    contentEntry.removeSourceFolder(sourceFolder)
                                    model.commit()
                                }
                        }
                    }
                }
            }
    }

    private fun removeSourceFolder(virtualFile: VirtualFile) {
        getOpenProjects()
            .forEach { project ->
                val model = getModelForFile(project, virtualFile!!)

                if (null != model) {
                    val contentEntries = model.contentEntries

                    if (contentEntries.isNotEmpty()) {
                        val contentEntry = contentEntries[0]

                        contentEntry
                            .sourceFolders
                            .filter { it.file?.equals(virtualFile) ?: false }
                            .forEach { sourceFolder ->
                                contentEntry.removeSourceFolder(sourceFolder)
                                model.commit()
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
                }
            }
        }

        super.before(events)
    }
}
