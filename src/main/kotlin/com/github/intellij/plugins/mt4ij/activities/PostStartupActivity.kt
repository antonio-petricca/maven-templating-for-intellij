package com.github.intellij.plugins.mt4ij.activities

import com.github.intellij.plugins.mt4ij.ApiHelpers
import com.github.intellij.plugins.mt4ij.config.SettingsStorage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.startup.StartupActivity
import org.jetbrains.jps.model.java.JavaSourceRootType

/*
    https://www.plugin-dev.com/intellij/general/plugin-initial-load/
    https://www.cqse.eu/en/news/blog/intellij-plugin-tutorial/
 */

class PostStartupActivity : StartupActivity {
    private val log : Logger = Logger.getInstance(PostStartupActivity::class.java)

    override fun runActivity(project: Project) {
        log.info("Scanning project for templates folders...")

        val templatesPath = SettingsStorage.getInstance(project).state.templatesPath
        val sourcesFolder = "main/${templatesPath}"
        val testsFolder   = "test/${templatesPath}"

        ProjectFileIndex
            .getInstance(project)
            .iterateContent { virtualFile ->
                val path           = virtualFile.path;
                val isSourceFolder = path.endsWith(sourcesFolder)
                val isTestFolder   = path.endsWith(testsFolder)

                if (isSourceFolder || isTestFolder) {
                    val model = ApiHelpers.getModelForFile(project, virtualFile)

                    if (null != model) {
                        val contentEntry = ApiHelpers.getContentEntry(model, virtualFile)

                        if (null != contentEntry) {
                            val sourceRoots =
                                model.getSourceRoots(if (!isTestFolder) JavaSourceRootType.SOURCE else JavaSourceRootType.TEST_SOURCE)

                            if (!sourceRoots.contains(virtualFile)) {
                                contentEntry.addSourceFolder(virtualFile, isTestFolder)
                                ApiHelpers.invokeCommit(model, project)

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
