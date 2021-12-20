package com.github.antoniopetricca.maventemplatingforintellij.listeners;

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

/*
    https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html#defining-application-level-listeners
    https://intellij-support.jetbrains.com/hc/en-us/community/posts/206768495-Mark-directory-as-sources-root-from-plugin
    https://intellij-support.jetbrains.com/hc/en-us/community/posts/360009670979-Mark-directory-as-excluded-from-BulkFileListener
    https://intellij-support.jetbrains.com/hc/en-us/community/posts/360003028820-BulkFileListener-behavior-with-out-of-process-files
 */

internal class MyVFSListener : BulkFileListener {

    private val SOURCE_FOLDER = "java-templates"

    private fun addSourceFolder(virtualFile: VirtualFile?) {
        ProjectManager
            .getInstance()
            .openProjects
            .forEach {
                val fileIndex = ProjectFileIndex.SERVICE.getInstance(it)
                val module    = fileIndex.getModuleForFile(virtualFile!!)

                if (null != module) {
                    val model = ModuleRootManager.getInstance(module).modifiableModel

                    model.addContentEntry(virtualFile)
                    model.commit()
                }
            }
    }

    override fun after(events: MutableList<out VFileEvent>) {
        events.forEach {
            if (it is VFileCreateEvent) {
                val createEvent = it as VFileCreateEvent

                if (createEvent.isDirectory) {
                    val path = createEvent.path;

                    if (
                           path.contains("main/${SOURCE_FOLDER}")
                        || path.contains("tests/${SOURCE_FOLDER}")
                    ) {
                        addSourceFolder(createEvent.file)
                    }
                }
            }
        }

        super.after(events)
    }
}
