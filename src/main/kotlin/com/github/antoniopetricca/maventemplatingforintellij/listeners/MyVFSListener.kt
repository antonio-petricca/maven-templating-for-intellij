package com.github.antoniopetricca.maventemplatingforintellij.listeners;

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiManager
import org.jetbrains.jps.model.java.JavaSourceRootType
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
 */

//TODO Make it configurable
//TODO Implement for moving and renaming
//TODO Handle folder removal

internal class MyVFSListener : BulkFileListener {

    private val SOURCE_FOLDER = "java-templates"

    private fun addSourceFolder(virtualFile: VirtualFile?, isTestFolder: Boolean) {
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

                    val contentEntry = model
                        .contentEntries
                        .filter { it.equals(virtualFile) }

                    if (contentEntry.isEmpty()) {
                        val sourceRoots = model.getSourceRoots(if (!isTestFolder) SOURCE else TEST_SOURCE)

                        if (!sourceRoots.contains(virtualFile)) {
                            model
                                .addContentEntry(virtualFile)
                                .addSourceFolder(virtualFile, isTestFolder)

                            model.commit()
                        }
                    }
                }
            }
    }

    override fun after(events: MutableList<out VFileEvent>) {
        events.forEach {
            if (it is VFileCreateEvent) {
                val createEvent = it as VFileCreateEvent

                if (createEvent.isDirectory) {
                    val path           = createEvent.path;
                    val isSourceFolder = path.contains("main/${SOURCE_FOLDER}")
                    val isTestFolder   = path.contains("test/${SOURCE_FOLDER}")

                    if (isSourceFolder || isTestFolder) {
                        addSourceFolder(createEvent.file, isTestFolder)
                    }
                }
            }
        }

        super.after(events)
    }
}
