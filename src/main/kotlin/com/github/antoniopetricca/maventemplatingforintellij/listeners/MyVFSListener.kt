package com.github.antoniopetricca.maventemplatingforintellij.listeners;

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.jetbrains.jps.model.java.JavaSourceRootType.*

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
//TODO Implement for moving and renaming
//TODO Check java project type
//TODO IDE logging

internal class MyVFSListener : BulkFileListener {

    private val TEMPLATES_FOLDER = "java-templates"
    private val SOURCES_FOLDER   = "main/${TEMPLATES_FOLDER}"
    private val TESTS_FOLDER     = "test/${TEMPLATES_FOLDER}"

    private fun addSourceFolder(virtualFile: VirtualFile?, isTestFolder: Boolean) {
        ProjectManager
            .getInstance()
            .openProjects
            .forEach {
                val fileIndex = ProjectFileIndex.SERVICE.getInstance(it)
                val module    = fileIndex.getModuleForFile(virtualFile!!)

                if (null != module) {
                    val model = ModuleRootManager
                        .getInstance(module)
                        .modifiableModel

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

    private fun removeSourceFolder(virtualFile: VirtualFile?) {
        ProjectManager
            .getInstance()
            .openProjects
            .forEach { it ->
                val fileIndex = ProjectFileIndex.SERVICE.getInstance(it)
                val module    = fileIndex.getModuleForFile(virtualFile!!)

                if (null != module) {
                    val model = ModuleRootManager
                        .getInstance(module)
                        .modifiableModel

                    val contentEntries = model.contentEntries
                    val contentEntry   = (if (contentEntries.isEmpty()) model.addContentEntry(virtualFile) else contentEntries[0])

                    contentEntry
                        .sourceFolders
                        .filter { it.file?.equals(virtualFile) ?: false }
                        .forEach {
                            contentEntry.removeSourceFolder(it)
                            model.commit()
                        }
                }
            }
    }

    override fun after(events: MutableList<out VFileEvent>) {
        events.forEach {
            val path           = it.path;
            val isSourceFolder = path.contains(SOURCES_FOLDER)
            val isTestFolder   = path.contains(TESTS_FOLDER)

            if (it is VFileCreateEvent) {
                val createEvent = it

                if (
                       createEvent.isDirectory
                    && (isSourceFolder || isTestFolder)
                ) {
                    addSourceFolder(createEvent.file, isTestFolder)
                }
            }
        }

        super.after(events)
    }

    override fun before(events: MutableList<out VFileEvent>) {
        events.forEach {
            val path           = it.path;
            val isSourceFolder = path.contains(SOURCES_FOLDER)
            val isTestFolder   = path.contains(TESTS_FOLDER)

            if (it is VFileDeleteEvent) {
                val deleteEvent = it

                if (isSourceFolder || isTestFolder) {
                    removeSourceFolder(deleteEvent.file)
                }
            }
        }

        super.before(events)
    }
}
