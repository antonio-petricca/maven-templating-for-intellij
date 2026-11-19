package com.github.intellij.plugins.mt4ij.importing

import org.jdom.Element
import org.jetbrains.idea.maven.model.MavenPlugin
import org.jetbrains.idea.maven.project.MavenProject
import java.io.File

/**
 * The templating-maven-plugin folders of a Maven project, read from the pom:
 * the template source folders and the folders the plugin generates the sources into.
 * All paths are absolute, normalized and use '/' as separator.
 */
class TemplatingFolders private constructor(
    val sources: List<String>,
    val testSources: List<String>,
    val outputs: List<String>,
    val testOutputs: List<String>,
    val isPluginDeclared: Boolean,
) {

    companion object {
        const val PLUGIN_GROUP_ID    = "org.codehaus.mojo"
        const val PLUGIN_ARTIFACT_ID = "templating-maven-plugin"

        const val DEFAULT_TEMPLATES_NAME = "java-templates"

        private const val DEFAULT_MAIN_OUTPUT = "\${project.build.directory}/generated-sources/java-templates"
        private const val DEFAULT_TEST_OUTPUT = "\${project.build.directory}/generated-test-sources/java-templates"

        /** A folder name is usable when it is not blank and is not a relative path marker. */
        fun isValidName(name: String): Boolean = name.isNotBlank() && name != "." && name != ".."

        /**
         * Without the plugin in the pom the source folders are named after [templatesName]
         * (the plugin default is used when it is declared without a custom folder);
         * an invalid name gives no source folders.
         */
        fun of(mavenProject: MavenProject, templatesName: String = DEFAULT_TEMPLATES_NAME): TemplatingFolders {
            val plugin  = mavenProject.findPlugin(PLUGIN_GROUP_ID, PLUGIN_ARTIFACT_ID)
            val baseDir = mavenProject.directory
            val buildDir = mavenProject.buildDirectory

            if (plugin == null) {
                val isValid = isValidName(templatesName)

                return TemplatingFolders(
                    sources         = if (isValid) listOf(resolvePath("src/main/$templatesName", baseDir)) else emptyList(),
                    testSources     = if (isValid) listOf(resolvePath("src/test/$templatesName", baseDir)) else emptyList(),
                    outputs         = listOf(resolvePath(DEFAULT_MAIN_OUTPUT, baseDir, buildDir)),
                    testOutputs     = listOf(resolvePath(DEFAULT_TEST_OUTPUT, baseDir, buildDir)),
                    isPluginDeclared = false,
                )
            }

            return TemplatingFolders(
                sources         = configuredPaths(plugin, "filter-sources", "sourceDirectory", baseDir, "src/main/$DEFAULT_TEMPLATES_NAME"),
                testSources     = configuredPaths(plugin, "filter-test-sources", "testSourceDirectory", baseDir, "src/test/$DEFAULT_TEMPLATES_NAME"),
                outputs         = configuredPaths(plugin, "filter-sources", "outputDirectory", baseDir, DEFAULT_MAIN_OUTPUT, buildDir),
                testOutputs     = configuredPaths(plugin, "filter-test-sources", "testOutputDirectory", baseDir, DEFAULT_TEST_OUTPUT, buildDir),
                isPluginDeclared = true,
            )
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

        fun resolvePath(value: String, baseDir: String, buildDir: String = ""): String {
            val path = value
                .replace("\${project.build.directory}", buildDir)
                .replace("\${basedir}", baseDir)
                .replace("\${project.basedir}", baseDir)
                .replace('\\', '/')

            val file = File(path)

            return (if (file.isAbsolute) file else File(baseDir, path)).normalize().path.replace('\\', '/')
        }
    }
}
