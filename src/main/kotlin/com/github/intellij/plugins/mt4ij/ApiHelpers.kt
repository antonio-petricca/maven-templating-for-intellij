package com.github.intellij.plugins.mt4ij

import com.github.intellij.plugins.mt4ij.config.SettingsStorage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jps.model.java.JavaSourceRootType

/*
    // Getting project instance

    https://intellij-support.jetbrains.com/hc/en-us/community/posts/206763335-Getting-active-project-
    https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/openapi/vfs/newvfs/BulkFileListener.java
    https://developerlife.com/2021/03/13/ij-idea-plugin-advanced/
 */

class ApiHelpers {
    companion object {
        private val log : Logger = Logger.getInstance(ApiHelpers::class.java)

        private const val SCAN_DELAY = 10L

        fun getContentEntry(model: ModifiableRootModel): ContentEntry? {
            val contentEntries = model.contentEntries
            return (if (contentEntries.isNotEmpty()) contentEntries[0] else null)
        }

        fun getContentEntry(model: ModifiableRootModel, virtualFile: VirtualFile): ContentEntry? {
            val contentEntries = model.contentEntries
            return (if (contentEntries.isEmpty()) model.addContentEntry(virtualFile) else contentEntries[0])
        }

        fun getModelForFile(project: Project, virtualFile: VirtualFile): ModifiableRootModel? {
            val module = ApplicationManager.getApplication().runReadAction(
                Computable<Module> {
                    ProjectFileIndex
                        .getInstance(project)
                        .getModuleForFile(virtualFile)
                }
            )

            if (
                (null != module)
                && ModuleType.get(module).id == "JAVA_MODULE"
            ) {
                return ModuleRootManager
                    .getInstance(module)
                    .modifiableModel
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

        fun scanProject(project: Project) {
            log.info("Scanning project for templates folders...")

            val templatesPath = SettingsStorage.getInstance(project).state.templatesPath
            val sourcesFolder = "main/${templatesPath}"
            val testsFolder   = "test/${templatesPath}"

            ProgressManager.getInstance().run(object : Task.Backgroundable(
                project,
                Bundle.message("mt4ij.actions.project-scanner.title"),
            ) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = false
                    doScanProject(project, indicator, sourcesFolder, testsFolder)
                }
            })
        }

        private fun doScanProject(project: Project, indicator: ProgressIndicator, sourcesFolder: String, testsFolder: String) {
            val fileIndex      = ProjectFileIndex.getInstance(project)
            var totalFiles     = 0
            var processedFiles = 0

            fileIndex.iterateContent { totalFiles++; true } // Count total files

            fileIndex.iterateContent { virtualFile ->
                if (indicator.isCanceled) {
                    return@iterateContent false
                }

                processedFiles++

                indicator.text     = Bundle.message("mt4ij.actions.project-scanner.progress", processedFiles, totalFiles)
                indicator.text2    = virtualFile.path
                indicator.fraction = processedFiles.toDouble() / totalFiles

                doScanFile(project, virtualFile, sourcesFolder, testsFolder)
                Thread.sleep(SCAN_DELAY)

                true
            }
        }

        private fun doScanFile(project: Project, virtualFile: VirtualFile, sourcesFolder: String, testsFolder: String) {
            val path           = virtualFile.path
            val isSourceFolder = path.endsWith(sourcesFolder)
            val isTestFolder   = path.endsWith(testsFolder)

            if (isSourceFolder || isTestFolder) {
                val model = getModelForFile(project, virtualFile)

                if (model != null) {
                    val contentEntry = getContentEntry(model, virtualFile)

                    if (contentEntry != null) {
                        val sourceRoots =
                            model.getSourceRoots(if (!isTestFolder) JavaSourceRootType.SOURCE else JavaSourceRootType.TEST_SOURCE)

                        if (!sourceRoots.contains(virtualFile)) {
                            contentEntry.addSourceFolder(virtualFile, isTestFolder)
                            invokeCommit(model, project)

                            log.info("Added source folder (after project opening): { virtualFile: \"$path\", isTestFolder: $isTestFolder }")
                        }
                    }
                }
            }
        }

    }
}
