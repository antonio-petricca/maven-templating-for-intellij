package com.github.intellij.plugins.mt4ij

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.project.MavenProjectsManager

class ApiHelpers {
    companion object {

        /** Queues a scan of the Maven projects for templates folders; does nothing for other projects. */
        fun scanProject(project: Project) {
            if (MavenProjectsManager.getInstance(project).isMavenizedProject) {
                project.service<TemplatesScanner>().requestScan()
            }
        }

    }
}
