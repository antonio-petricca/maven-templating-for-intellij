package com.github.intellij.plugins.mt4ij.activities

import com.github.intellij.plugins.mt4ij.ApiHelpers
import com.github.intellij.plugins.mt4ij.config.SettingsStorage
import com.github.intellij.plugins.mt4ij.navigation.GeneratedSourceGutterIcon
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/*
    # Project startup hook

    https://www.plugin-dev.com/intellij/general/plugin-initial-load/
    https://www.cqse.eu/en/news/blog/intellij-plugin-tutorial/
 */

class ProjectStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        if (SettingsStorage.getInstance(project).state.scanOnStartup) {
            ApiHelpers.scanProject(project)
        }

        // The editors restored at startup may have been opened before the Maven projects were known
        GeneratedSourceGutterIcon.refresh(project)
    }
}
