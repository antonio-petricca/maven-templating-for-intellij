package com.github.intellij.plugins.mt4ij

import com.github.intellij.plugins.mt4ij.config.SettingsStorage
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.idea.maven.project.MavenProjectsManager

/**
 * Looks for the templates folders of every Maven project. Requests arriving close to each
 * other are merged, so scans never overlap.
 */
@Service(Service.Level.PROJECT)
class TemplatesScanner(private val project: Project, scope: CoroutineScope) {
    private val log: Logger = Logger.getInstance(TemplatesScanner::class.java)

    // Conflated: while a scan is running, any number of requests collapses into one
    private val requests = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            for (request in requests) {
                delay(MERGE_DELAY_MS)
                scan()
            }
        }
    }

    fun requestScan() {
        requests.trySend(Unit)
    }

    private suspend fun scan() {
        val title = Bundle.message("mt4ij.actions.project-scanner.title")

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
                            TemplatesFolderKind.entries.mapNotNull { kind ->
                                val subFolder = if (kind.isTest) "test" else "main"
                                val folder = VfsUtil.findRelativeFile(baseDir, "src", subFolder, templatesName)
                                val module = folder?.let { ProjectFileIndex.getInstance(project).getModuleForFile(it) }

                                if (folder != null && folder.isDirectory && module != null) {
                                    SourceFolderChange.add(module, folder, kind.isTest)
                                } else {
                                    null
                                }
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.EDT) {
                SourceFolders.apply(changes)
            }
        }
    }

    private companion object {
        const val MERGE_DELAY_MS = 300L
    }
}
