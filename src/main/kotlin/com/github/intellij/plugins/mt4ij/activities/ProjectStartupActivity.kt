package com.github.intellij.plugins.mt4ij.activities

import com.github.intellij.plugins.mt4ij.ApiHelpers
import com.github.intellij.plugins.mt4ij.listeners.MavenProjectsManagerListener
import com.github.intellij.plugins.mt4ij.listeners.VFSListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.idea.maven.project.MavenProjectsManager

/*
    # Project startup hook

    https://www.plugin-dev.com/intellij/general/plugin-initial-load/
    https://www.cqse.eu/en/news/blog/intellij-plugin-tutorial/

    # VFS listener programmatic registration

    https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html#defining-application-level-listeners
 */

class ProjectStartupActivity : StartupActivity {
    private val log : Logger = Logger.getInstance(ProjectStartupActivity::class.java)

    private fun registerMavenListener(project: Project) {
        log.info("Registering Maven Projects listener for templates folders...")

        MavenProjectsManager
            .getInstance(project)
            .addManagerListener(MavenProjectsManagerListener(project))
    }

    private fun registerVFSListener(project: Project) {
        log.info("Registering VFS listener for templates folders...")

        project
            .messageBus
            .connect()
            .subscribe(VirtualFileManager.VFS_CHANGES, VFSListener(project))
    }

    override fun runActivity(project: Project) {
        registerMavenListener(project)
        registerVFSListener(project)

        ApiHelpers.scanProject(project)
    }
}
