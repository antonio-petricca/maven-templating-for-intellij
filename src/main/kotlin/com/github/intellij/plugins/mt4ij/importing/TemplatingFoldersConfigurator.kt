package com.github.intellij.plugins.mt4ij.importing

import com.github.intellij.plugins.mt4ij.config.SettingsStorage
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.project.ProjectManager
import org.jdom.Element
import org.jetbrains.idea.maven.importing.MavenStaticSyncAware
import org.jetbrains.idea.maven.importing.MavenWorkspaceConfigurator
import org.jetbrains.idea.maven.importing.MavenWorkspaceConfigurator.AdditionalFolder
import org.jetbrains.idea.maven.importing.MavenWorkspaceConfigurator.FolderType
import org.jetbrains.idea.maven.importing.MavenWorkspaceConfigurator.FoldersContext
import org.jetbrains.idea.maven.model.MavenPlugin
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager
import java.io.File
import java.util.stream.Stream

/*
    Declares the templating-maven-plugin folders as additional source roots at Maven import time,
    so that they survive project reimports.
 */

class TemplatingFoldersConfigurator : MavenWorkspaceConfigurator, MavenStaticSyncAware {

    private companion object {
        const val PLUGIN_GROUP_ID    = "org.codehaus.mojo"
        const val PLUGIN_ARTIFACT_ID = "templating-maven-plugin"

        const val DEFAULT_MAIN_FOLDER = "src/main/java-templates"
        const val DEFAULT_TEST_FOLDER = "src/test/java-templates"

        const val DEFAULT_MAIN_OUTPUT = "\${project.build.directory}/generated-sources/java-templates"
        const val DEFAULT_TEST_OUTPUT = "\${project.build.directory}/generated-test-sources/java-templates"
    }

    override fun getAdditionalFolders(context: FoldersContext): Stream<AdditionalFolder> {
        val mavenProject = context.mavenProject
        val plugin       = mavenProject.findPlugin(PLUGIN_GROUP_ID, PLUGIN_ARTIFACT_ID)

        val folders = if (plugin != null) {
            pluginFolders(mavenProject, plugin)
        } else {
            fallbackFolders(mavenProject)
        }

        return folders.stream()
    }

    // The generated copies duplicate the template classes, so the IDE must not see them as sources
    override fun getFoldersToExclude(context: FoldersContext): Stream<String> {
        val mavenProject = context.mavenProject
        val plugin       = mavenProject.findPlugin(PLUGIN_GROUP_ID, PLUGIN_ARTIFACT_ID)

        if (plugin == null || !isExclusionEnabled(mavenProject)) {
            return Stream.empty()
        }

        val baseDir = mavenProject.directory

        val mainOutputs = configuredPaths(plugin, "filter-sources", "outputDirectory", baseDir, DEFAULT_MAIN_OUTPUT, mavenProject.buildDirectory)
        val testOutputs = configuredPaths(plugin, "filter-test-sources", "testOutputDirectory", baseDir, DEFAULT_TEST_OUTPUT, mavenProject.buildDirectory)

        return (mainOutputs + testOutputs).distinct().stream()
    }

    private fun isExclusionEnabled(mavenProject: MavenProject): Boolean {
        val project = ProjectLocator.getInstance().guessProjectForFile(mavenProject.directoryFile)

        return project?.let { SettingsStorage.getInstance(it).state.excludeGeneratedSources } ?: false
    }

    private fun pluginFolders(mavenProject: MavenProject, plugin: MavenPlugin): List<AdditionalFolder> {
        val baseDir = mavenProject.directory

        val mainPaths = configuredPaths(plugin, "filter-sources", "sourceDirectory", baseDir, DEFAULT_MAIN_FOLDER)
        val testPaths = configuredPaths(plugin, "filter-test-sources", "testSourceDirectory", baseDir, DEFAULT_TEST_FOLDER)

        return mainPaths.map { AdditionalFolder(it, FolderType.SOURCE) } +
               testPaths.map { AdditionalFolder(it, FolderType.TEST_SOURCE) }
    }

    // Collects the directories set at plugin level and in the executions bound to the goal
    private fun configuredPaths(
        plugin: MavenPlugin,
        goal: String,
        parameter: String,
        baseDir: String,
        defaultFolder: String,
        buildDir: String = ""
    ): List<String> {
        val values = mutableListOf<String>()

        values += parameterValue(plugin.configurationElement, parameter)

        plugin.executions
            .filter { goal in it.goals }
            .forEach { values += parameterValue(it.configurationElement, parameter) }

        val resolved = values.map { resolvePath(it, baseDir, buildDir) }

        return resolved.ifEmpty { listOf(resolvePath(defaultFolder, baseDir, buildDir)) }.distinct()
    }

    private fun parameterValue(configuration: Element?, parameter: String): List<String> {
        val value = configuration?.getChildTextTrim(parameter)

        return if (value.isNullOrEmpty()) emptyList() else listOf(value)
    }

    private fun resolvePath(value: String, baseDir: String, buildDir: String = ""): String {
        val path = value
            .replace("\${project.build.directory}", buildDir)
            .replace("\${basedir}", baseDir)
            .replace("\${project.basedir}", baseDir)
            .replace('\\', '/')

        val file = File(path)

        return (if (file.isAbsolute) file else File(baseDir, path)).normalize().path.replace('\\', '/')
    }

    // The context does not expose the project: take the open one that manages this Maven project
    private fun projectOf(mavenProject: MavenProject): Project? =
        ProjectManager.getInstance().openProjects.firstOrNull { project ->
            MavenProjectsManager.getInstanceIfCreated(project)?.findProject(mavenProject.file) != null
        } ?: ProjectLocator.getInstance().guessProjectForFile(mavenProject.directoryFile)

    // Plugin not declared: use the folder name from the settings, if the folders exist
    private fun fallbackFolders(mavenProject: MavenProject): List<AdditionalFolder> {
        val baseDir      = mavenProject.directory
        val project      = ProjectLocator.getInstance().guessProjectForFile(mavenProject.directoryFile)
        val templatesDir = project?.let { SettingsStorage.getInstance(it).state.templatesPath }
                           ?: "java-templates"

        return listOf(
            "src/main/$templatesDir" to FolderType.SOURCE,
            "src/test/$templatesDir" to FolderType.TEST_SOURCE
        )
            .map { (relativePath, type) -> resolvePath(relativePath, baseDir) to type }
            .filter { (path, _) -> File(path).isDirectory }
            .map { (path, type) -> AdditionalFolder(path, type) }
    }
}
