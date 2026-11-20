package com.github.intellij.plugins.mt4ij.activities

import com.github.intellij.plugins.mt4ij.config.AppSettings
import com.github.intellij.plugins.mt4ij.guide.GuideTabs
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/** Shows the "What's New" tab once after an install or update, in the first project that gets opened. */
class WhatsNewStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        if (ApplicationManager.getApplication().isHeadlessEnvironment) {
            return
        }

        AppSettings.getInstance().claimWhatsNew(GuideTabs.installedVersion())?.let { GuideTabs.openWhatsNew(project, it) }
    }
}
