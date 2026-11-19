import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // IntelliJ Platform Gradle Plugin 2.x
    alias(libs.plugins.changelog) // Changelog and Markdown to HTML support
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenCentral()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

// IntelliJ IDEA 2026.2+ requires Java 25.
kotlin {
    jvmToolchain(25)

    compilerOptions {
        // Do not generate delegating stubs for Java interface default methods
        jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
// IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
dependencies {
    intellijPlatform {
        intellijIdea(properties("platformVersion"))

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
        bundledPlugins(properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) })

        // IntelliJ Platform Gradle Plugin Dependencies - https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    // Avoid resolving java compiler instrumentation artifacts from remote JetBrains repositories.
    instrumentCode = false

    pluginConfiguration {
        name = properties("pluginName")
        version = properties("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                markdownToHTML(subList(indexOf(start) + 1, indexOf(end)).joinToString("\n"))
            }
        }

        // Render the changelog section of the current version (or the latest one) as HTML
        val changelog = project.changelog
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getLatest()).withHeader(false).withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")
        }
    }

    signing {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = environment("PUBLISH_TOKEN")

        // A version like 1.0.0-beta.1 is published on the "beta" channel, otherwise on "default"
        channels = properties("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        // The plugin is already published with "intellij" in its ID and name
        freeArgs = listOf("-mute", "TemplateWordInPluginId,TemplateWordInPluginName")

        // Explicit IDEs: recommended() resolves "ideaIC" for 2025.3+, which no longer exists
        // since JetBrains unified Community and Ultimate into a single IntelliJ IDEA distribution.
        ides {
            create(IntelliJPlatformType.IntellijIdea, "2026.2")
            create(IntelliJPlatformType.IntellijIdea, "2026.3")
        }
    }
}


// Changelog plugin configuration - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = "https://github.com/antonio-petricca/maven-templating-for-intellij"
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }
}
