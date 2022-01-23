package com.github.intellij.plugins.mt4ij.activities

import com.github.intellij.plugins.mt4ij.ApiHelpers
import com.github.intellij.plugins.mt4ij.listeners.MavenProjectsManagerListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.jetbrains.idea.maven.project.MavenProjectsManager

/*
    https://www.plugin-dev.com/intellij/general/plugin-initial-load/
    https://www.cqse.eu/en/news/blog/intellij-plugin-tutorial/
 */

class ProjectStartupActivity : StartupActivity {
    private val log : Logger = Logger.getInstance(ProjectStartupActivity::class.java)

    private fun registerMavenListener(project: Project) {
        log.info("Registering listener for templates folders...")

        MavenProjectsManager
            .getInstance(project)
            .addManagerListener(MavenProjectsManagerListener(project))
    }

    override fun runActivity(project: Project) {
        registerMavenListener(project)
        ApiHelpers.scanProject(project)
    }
}
