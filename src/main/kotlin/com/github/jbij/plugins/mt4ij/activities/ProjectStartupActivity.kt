package com.github.jbij.plugins.mt4ij.activities

import com.github.jbij.plugins.mt4ij.ApiHelpers
import com.github.jbij.plugins.mt4ij.listeners.MavenProjectsManagerListener
import com.github.jbij.plugins.mt4ij.listeners.VFSListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.idea.maven.project.MavenProjectsManager

/*
    # Project startup hook

    https://www.plugin-dev.com/intellij/general/plugin-initial-load/
    https://www.cqse.eu/en/news/blog/intellij-plugin-tutorial/

    # VFS listener programmatic registration

    https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html#defining-application-level-listeners
 */

class ProjectStartupActivity : ProjectActivity {
    private val log : Logger = Logger.getInstance(ProjectStartupActivity::class.java)

    private fun registerMavenListener(project: Project) {
        log.info("Registering Maven Projects listener for templates folders...")

        val mavenProjectsManager = MavenProjectsManager.getInstance(project)
        val listener             = MavenProjectsManagerListener(project)

        mavenProjectsManager.addProjectsTreeListener(listener)
    }

    private fun registerVFSListener(project: Project) {
        log.info("Registering VFS listener for templates folders...")

        project
            .messageBus
            .connect()
            .subscribe(VirtualFileManager.VFS_CHANGES, VFSListener(project))
    }

    override suspend fun execute(project: Project) {
        registerMavenListener(project)
        registerVFSListener(project)

        ApiHelpers.scanProject(project)
    }
}
