import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

// Simple markdown to HTML conversion (replaces org.jetbrains.changelog.markdownToHTML)
fun markdownToHTML(markdown: String): String {
    val sb = StringBuilder()
    var inList = false
    for (line in markdown.lines()) {
        when {
            line.startsWith("### ") -> { if (inList) { sb.append("</ul>\n"); inList = false }; sb.append("<h3>${line.removePrefix("### ").trim()}</h3>\n") }
            line.startsWith("## ")  -> { if (inList) { sb.append("</ul>\n"); inList = false }; sb.append("<h2>${line.removePrefix("## ").trim()}</h2>\n") }
            line.startsWith("# ")   -> { if (inList) { sb.append("</ul>\n"); inList = false }; sb.append("<h1>${line.removePrefix("# ").trim()}</h1>\n") }
            line.startsWith("- ") || line.startsWith("* ") -> {
                if (!inList) { sb.append("<ul>\n"); inList = true }
                sb.append("<li>${line.drop(2).trim()}</li>\n")
            }
            line.isBlank() -> { if (inList) { sb.append("</ul>\n"); inList = false }; sb.append("\n") }
            else -> { if (inList) { sb.append("</ul>\n"); inList = false }; sb.append("<p>${line.trim()}</p>\n") }
        }
    }
    if (inList) sb.append("</ul>\n")
    return sb.toString()
}

// Extract change notes for a given version from CHANGELOG.md
fun extractChangeNotes(changelogFile: java.io.File, version: String): String {
    val lines = changelogFile.readLines()
    val start = lines.indexOfFirst { it.startsWith("## [$version]") || it.startsWith("## $version") }
    if (start < 0) return "<p>See <a href=\"https://github.com/petricca/maven-templating-for-intellij/blob/main/CHANGELOG.md\">CHANGELOG.md</a></p>"
    val end = lines.drop(start + 1).indexOfFirst { it.startsWith("## [") || it.startsWith("## ") }
    val section = if (end >= 0) lines.subList(start + 1, start + 1 + end) else lines.drop(start + 1)
    val items = section.filter { it.trim().startsWith("- ") || it.trim().startsWith("* ") }
    return if (items.isEmpty()) "<p>Maintenance release.</p>"
    else "<ul>${items.joinToString("") { "<li>${it.trim().drop(2).trim()}</li>" }}</ul>"
}

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // IntelliJ Platform Gradle Plugin 2.x
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

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    jvmToolchain(21)
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
// IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
dependencies {
    intellijPlatform {
        create(properties("platformType").get(), properties("platformVersion").get())

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

        changeNotes = properties("pluginVersion").map { pluginVersion ->
            extractChangeNotes(layout.projectDirectory.file("CHANGELOG.md").asFile, pluginVersion)
        }

        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}


tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }
}
