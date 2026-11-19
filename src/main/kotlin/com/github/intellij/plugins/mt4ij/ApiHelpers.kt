package com.github.intellij.plugins.mt4ij

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.project.MavenProjectsManager

class ApiHelpers {
    companion object {

        private const val NOTIFICATION_GROUP_ID = "mt4ij.notifications"

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

        /** Tells the user that a setting needs a Maven reimport to take effect, and offers to run it. */
        fun suggestReimport(project: Project) {
            if (!MavenProjectsManager.getInstance(project).isMavenizedProject) {
                return
            }

            NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(Bundle.message("mt4ij.notification.reimport.message"), NotificationType.INFORMATION)
                .addAction(NotificationAction.createSimpleExpiring(Bundle.message("mt4ij.notification.reimport.action")) {
                    MavenProjectsManager.getInstance(project).forceUpdateAllProjectsOrFindAllAvailablePomFiles()
                })
                .notify(project)
        }

    }
}
