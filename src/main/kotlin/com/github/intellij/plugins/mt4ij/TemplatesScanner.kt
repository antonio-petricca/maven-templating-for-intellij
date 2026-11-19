package com.github.intellij.plugins.mt4ij

import com.github.intellij.plugins.mt4ij.config.SettingsStorage
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.idea.maven.project.MavenProjectsManager
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Looks for the templates folders of every Maven project. Requests arriving close to each
 * other are merged, so scans never overlap.
 */
@Service(Service.Level.PROJECT)
class TemplatesScanner(private val project: Project, scope: CoroutineScope) {
    private val log: Logger = Logger.getInstance(TemplatesScanner::class.java)

    // Conflated: while a scan is running, any number of requests collapses into one
    private val requests = Channel<Unit>(Channel.CONFLATED)

    // Options of the merged requests, consumed by the scan that serves them
    private val notifyPending = AtomicBoolean(false)
    private val previousNamePending = AtomicReference<String?>(null)

    init {
        scope.launch {
            for (request in requests) {
                delay(MERGE_DELAY_MS)
                scan()
            }
        }
    }

    /**
     * @param notify show a summary at the end of the scan
     * @param previousName former templates folder name: its folders are unmarked before scanning
     */
    fun requestScan(notify: Boolean = false, previousName: String? = null) {
        if (notify) {
            notifyPending.set(true)
        }

        // Keep the oldest name when several renames are merged into one scan
        previousName?.let { previousNamePending.compareAndSet(null, it) }

        requests.trySend(Unit)
    }

    private suspend fun scan() {
        val title = Bundle.message("mt4ij.actions.project-scanner.title")
        val notify = notifyPending.getAndSet(false)
        val previousName = previousNamePending.getAndSet(null)

        var markedCount = 0

        withBackgroundProgress(project, title) {
            val mavenProjects = MavenProjectsManager.getInstance(project).projects
            val templatesName = SettingsStorage.getInstance(project).state.templatesPath

            if (templatesName.isBlank()) {
                return@withBackgroundProgress
            }

            log.info("Scanning ${mavenProjects.size} Maven project(s) for templates folders...")

            val changes = mutableListOf<SourceFolderChange>()

            reportSequentialProgress(mavenProjects.size) { reporter ->
                mavenProjects.forEachIndexed { index, mavenProject ->
                    val text = Bundle.message("mt4ij.actions.project-scanner.progress", index + 1, mavenProjects.size)

                    reporter.itemStep(text) {
                        val baseDir = mavenProject.directoryFile

                        changes += readAction {
                            val removals = previousName
                                ?.let { findChanges(baseDir, it, SourceFolderChange::remove) }
                                .orEmpty()

                            removals + findChanges(baseDir, templatesName, SourceFolderChange::add)
                        }
                    }
                }
            }

            markedCount = changes.count { it.isAddition }

            withContext(Dispatchers.EDT) {
                SourceFolders.apply(changes)
            }
        }

        if (notify) {
            showSummary(markedCount)
        }
    }

    private fun findChanges(
        baseDir: VirtualFile,
        templatesName: String,
        createChange: (Module, VirtualFile, Boolean) -> SourceFolderChange,
    ): List<SourceFolderChange> {
        return TemplatesFolderKind.entries.mapNotNull { kind ->
            val subFolder = if (kind.isTest) "test" else "main"
            val folder = VfsUtil.findRelativeFile(baseDir, "src", subFolder, templatesName)
            val module = folder?.let { ProjectFileIndex.getInstance(project).getModuleForFile(it) }

            if (folder != null && folder.isDirectory && module != null) {
                createChange(module, folder, kind.isTest)
            } else {
                null
            }
        }
    }

    private fun showSummary(markedCount: Int) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(
                Bundle.message("mt4ij.notification.scan-completed", markedCount),
                NotificationType.INFORMATION
            )
            .notify(project)
    }

    private companion object {
        const val MERGE_DELAY_MS = 300L
        const val NOTIFICATION_GROUP_ID = "mt4ij.notifications"
    }
}
