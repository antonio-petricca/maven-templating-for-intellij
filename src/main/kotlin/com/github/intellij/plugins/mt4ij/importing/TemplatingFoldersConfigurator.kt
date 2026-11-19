package com.github.intellij.plugins.mt4ij.importing

import com.github.intellij.plugins.mt4ij.config.SettingsStorage
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.project.ProjectManager
import org.jetbrains.idea.maven.importing.MavenStaticSyncAware
import org.jetbrains.idea.maven.importing.MavenWorkspaceConfigurator
import org.jetbrains.idea.maven.importing.MavenWorkspaceConfigurator.AdditionalFolder
import org.jetbrains.idea.maven.importing.MavenWorkspaceConfigurator.FolderType
import org.jetbrains.idea.maven.importing.MavenWorkspaceConfigurator.FoldersContext
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager
import java.io.File
import java.util.stream.Stream

/*
    Declares the templating-maven-plugin folders as additional source roots at Maven import time,
    so that they survive project reimports.
 */

class TemplatingFoldersConfigurator : MavenWorkspaceConfigurator, MavenStaticSyncAware {

    override fun getAdditionalFolders(context: FoldersContext): Stream<AdditionalFolder> {
        val mavenProject = context.mavenProject

        val folders = if (mavenProject.findPlugin(TemplatingFolders.PLUGIN_GROUP_ID, TemplatingFolders.PLUGIN_ARTIFACT_ID) != null) {
            pluginFolders(mavenProject)
        } else {
            fallbackFolders(mavenProject)
        }

        return folders.stream()
    }

    // The generated copies duplicate the template classes, so the IDE must not see them as sources
    override fun getFoldersToExclude(context: FoldersContext): Stream<String> {
        val mavenProject = context.mavenProject
        val folders      = TemplatingFolders.of(mavenProject)

        if (!folders.isPluginDeclared || !isExclusionEnabled(mavenProject)) {
            return Stream.empty()
        }

        return (folders.outputs + folders.testOutputs).distinct().stream()
    }

    // The context does not expose the project: take the open one that manages this Maven project
    private fun projectOf(mavenProject: MavenProject): Project? =
        ProjectManager.getInstance().openProjects.firstOrNull { project ->
            MavenProjectsManager.getInstanceIfCreated(project)?.findProject(mavenProject.file) != null
        } ?: ProjectLocator.getInstance().guessProjectForFile(mavenProject.directoryFile)

    private fun isExclusionEnabled(mavenProject: MavenProject): Boolean {
        val project = projectOf(mavenProject)

        return project?.let { SettingsStorage.getInstance(it).state.excludeGeneratedSources } ?: false
    }

    private fun pluginFolders(mavenProject: MavenProject): List<AdditionalFolder> {
        val folders = TemplatingFolders.of(mavenProject)

        return folders.sources.map { AdditionalFolder(it, FolderType.SOURCE) } +
               folders.testSources.map { AdditionalFolder(it, FolderType.TEST_SOURCE) }
    }

    // Plugin not declared: use the folder name from the settings, if the folders exist
    private fun fallbackFolders(mavenProject: MavenProject): List<AdditionalFolder> {
        val project      = projectOf(mavenProject)
        val templatesDir = project?.let { SettingsStorage.getInstance(it).state.templatesPath }
                           ?: TemplatingFolders.DEFAULT_TEMPLATES_NAME

        if (!TemplatingFolders.isValidName(templatesDir)) {
            return emptyList()
        }

        val folders = TemplatingFolders.of(mavenProject, templatesDir)

        return (folders.sources.map { it to FolderType.SOURCE } + folders.testSources.map { it to FolderType.TEST_SOURCE })
            .filter { (path, _) -> File(path).isDirectory }
            .map { (path, type) -> AdditionalFolder(path, type) }
    }
}
