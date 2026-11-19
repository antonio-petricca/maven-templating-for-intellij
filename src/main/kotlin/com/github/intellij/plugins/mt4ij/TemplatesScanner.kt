package com.github.intellij.plugins.mt4ij

import com.github.intellij.plugins.mt4ij.config.SettingsStorage
import com.github.intellij.plugins.mt4ij.importing.TemplatingFolders
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.idea.maven.project.MavenProjectsManager
import java.io.File
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

                // A failing scan must not stop the loop: later requests still need to be served
                try {
                    scan()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.warn("Scan of the templates folders failed", e)
                }
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

        var applied = AppliedChanges(0, 0)

        withBackgroundProgress(project, title) {
            val mavenProjects = MavenProjectsManager.getInstance(project).projects
            val templatesName = SettingsStorage.getInstance(project).state.templatesPath

            if (!TemplatingFolders.isValidName(templatesName)) {
                return@withBackgroundProgress
            }

            log.info("Scanning ${mavenProjects.size} Maven project(s) for templates folders...")

            val changes = mutableListOf<SourceFolderChange>()

            reportSequentialProgress(mavenProjects.size) { reporter ->
                mavenProjects.forEachIndexed { index, mavenProject ->
                    val text = Bundle.message("mt4ij.actions.project-scanner.progress.project", index + 1, mavenProjects.size)

                    reporter.itemStep(text) {
                        changes += readAction {
                            // Projects declaring the plugin keep their folders whatever the name was
                            val removals = previousName
                                ?.let { TemplatingFolders.of(mavenProject, it) }
                                ?.takeUnless { it.isPluginDeclared }
                                ?.let { findChanges(it, SourceFolderChange::remove) }
                                .orEmpty()

                            val folders = TemplatingFolders.of(mavenProject, templatesName)

                            removals + findChanges(folders, SourceFolderChange::add)
                        }
                    }
                }
            }

            applied = withContext(Dispatchers.EDT) {
                SourceFolders.apply(changes)
            }
        }

        if (notify) {
            showSummary(applied)
        }
    }

    // The templates folders of a Maven project that exist on disk and belong to a module
    private fun findChanges(
        folders: TemplatingFolders,
        createChange: (Module, VirtualFile, Boolean) -> SourceFolderChange,
    ): List<SourceFolderChange> {
        val candidates = folders.sources.map { it to false } + folders.testSources.map { it to true }
        val fileIndex = ProjectFileIndex.getInstance(project)

        return candidates.mapNotNull { (path, isTest) ->
            val folder = VfsUtil.findFileByIoFile(File(path), false)
            val module = folder?.let { fileIndex.getModuleForFile(it) }

            if (folder != null && folder.isDirectory && module != null) {
                createChange(module, folder, isTest)
            } else {
                null
            }
        }
    }

    private fun showSummary(applied: AppliedChanges) {
        val messages = mutableListOf<String>()

        if (applied.added > 0) {
            messages += Bundle.message("mt4ij.notification.scan-completed", applied.added)
        }

        if (applied.removed > 0) {
            messages += Bundle.message("mt4ij.notification.scan-unmarked", applied.removed)
        }

        // Nothing changed: say that the folders were already as they should be
        if (messages.isEmpty()) {
            messages += Bundle.message("mt4ij.notification.scan-completed", 0)
        }

        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(messages.joinToString(". "), NotificationType.INFORMATION)
            .notify(project)
    }

    private companion object {
        const val MERGE_DELAY_MS = 300L
        const val NOTIFICATION_GROUP_ID = "mt4ij.notifications"
    }
}
