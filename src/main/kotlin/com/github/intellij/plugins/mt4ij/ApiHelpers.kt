package com.github.intellij.plugins.mt4ij

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.project.MavenProjectsManager

class ApiHelpers {
    companion object {

        /**
         * Queues a scan of the Maven projects for templates folders; does nothing for other projects.
         *
         * @param notify show a summary when the scan ends (for scans the user asked for)
         * @param previousName former templates folder name, whose folders get unmarked first
         */
        fun scanProject(project: Project, notify: Boolean = false, previousName: String? = null) {
            if (MavenProjectsManager.getInstance(project).isMavenizedProject) {
                project.service<TemplatesScanner>().requestScan(notify, previousName)
            }
        }

    }
}
