package com.github.intellij.plugins.mt4ij

import com.github.intellij.plugins.mt4ij.config.SettingsStorage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jps.model.java.JavaSourceRootType

/*
    // Getting project instance

    https://intellij-support.jetbrains.com/hc/en-us/community/posts/206763335-Getting-active-project-
    https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/openapi/vfs/newvfs/BulkFileListener.java
    https://developerlife.com/2021/03/13/ij-idea-plugin-advanced/

    // UI progress indicator

    https://intellij-support.jetbrains.com/hc/en-us/community/posts/206762095-How-to-run-a-Backgroundable-Task-in-progress-and-synchronously-
 */

class PluginHelpers {
    companion object {

        private val log : Logger = Logger.getInstance(PluginHelpers::class.java)

        fun getContentEntry(model: ModifiableRootModel): ContentEntry? {
            val contentEntries = model.contentEntries
            return (if (contentEntries.isNotEmpty()) contentEntries[0] else null)
        }

        fun getContentEntry(model: ModifiableRootModel, virtualFile: VirtualFile): ContentEntry? {
            val contentEntries = model.contentEntries
            return (if (contentEntries.isEmpty()) model.addContentEntry(virtualFile) else contentEntries[0])
        }

        fun getModelForFile(project: Project, virtualFile: VirtualFile): ModifiableRootModel? {
            val fileIndex = ProjectFileIndex.SERVICE.getInstance(project)
            val module    = fileIndex.getModuleForFile(virtualFile)

            if (null != module) {
                if (module.moduleTypeName.equals("JAVA_MODULE")) {
                    return ModuleRootManager
                        .getInstance(module)
                        .modifiableModel
                }
            }

            log.warn("Not a java project.")

            return null
        }

        fun invokeLater(runnable: Runnable, project: Project) {
            val application = ApplicationManager.getApplication()

            application.invokeLater(
                { application.runWriteAction(runnable) },
                project.disposed
            )
        }

        fun invokeCommit(model: ModifiableRootModel, project: Project) {
            invokeLater(
                {
                    model.commit()
                },
                project
            )
        }

        fun runWithProgress(project: Project, title: String, runnable: Runnable) {
            val task = Backgroundable(project, title)

            ProgressManager
                .getInstance()
                .runProcessWithProgressAsynchronously(
                    task,
                    ProgressManager.getInstance().progressIndicator
                )
        }

        fun scanProject(project: Project) {
            val message = "Scanning project for templates source folders..."

            runWithProgress(project, message) {
                log.info(message)

                val templatesPath = SettingsStorage.getInstance(project).state.templatesPath
                val sourcesFolder = "main/${templatesPath}"
                val testsFolder   = "test/${templatesPath}"

                ProjectFileIndex
                    .getInstance(project)
                    .iterateContent { virtualFile ->
                        val path = virtualFile.path
                        val isSourceFolder = path.endsWith(sourcesFolder)
                        val isTestFolder = path.endsWith(testsFolder)

                        if (isSourceFolder || isTestFolder) {
                            val model = getModelForFile(project, virtualFile)

                            if (null != model) {
                                val contentEntry = getContentEntry(model, virtualFile)

                                if (null != contentEntry) {
                                    val sourceRoots =
                                        model.getSourceRoots(if (!isTestFolder) JavaSourceRootType.SOURCE else JavaSourceRootType.TEST_SOURCE)

                                    if (!sourceRoots.contains(virtualFile)) {
                                        contentEntry.addSourceFolder(virtualFile, isTestFolder)
                                        invokeCommit(model, project)

                                        log.info(
                                            String.format(
                                                "Added source folder (after project opening): { virtualFile: \"%s\", isTestFolder: %s }",
                                                path,
                                                isTestFolder
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        true
                    }

            }
        }

    }
}
