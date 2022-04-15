package com.github.intellij.plugins.mt4ij.listeners

import com.github.intellij.plugins.mt4ij.PluginHelpers
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.project.MavenProjectsManager

class MavenProjectsManagerListener(project: Project) : MavenProjectsManager.Listener {
    private val log : Logger = Logger.getInstance(MavenProjectsManagerListener::class.java)
    private val projectRef   = project

    override fun projectImportCompleted() {
        log.info("Processing templates folders on maven project import...")
        PluginHelpers.scanProject(projectRef)

        super.projectImportCompleted()
    }
}
