import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

/** Renders the Markdown user guides and the change notes as HTML pages, bundled in the plugin and opened inside the IDE. */
@CacheableTask
abstract class RenderGuideTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val englishGuide: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val italianGuide: RegularFileProperty

    // Optional: without it (or without the section of the current version) the English notes are used
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val italianChangelog: ConfigurableFileCollection

    @get:Input
    abstract val pluginVersion: Property<String>

    @get:Input
    abstract val changeNotes: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun render() {
        val target = outputDirectory.get().asFile.resolve("guide").apply { mkdirs() }
        val css = STYLE.trimIndent()

        fun write(name: String, text: String) = target.resolve(name).writeText(text, Charsets.UTF_8)

        write("guide.css", css)
        write("guide.html", page("en", css, englishGuide.get().asFile.readText()))
        write("guide_it.html", page("it", css, italianGuide.get().asFile.readText()))

        // Version and change notes are read at runtime from here, without asking the plugin manager
        write("plugin-info.properties", "version=${pluginVersion.get()}\n")
        write("whats-new.html", changeNotes.get())
        write("whats-new_it.html", italianNotes() ?: changeNotes.get())
    }

    /** The "## [version]" section of CHANGELOG.it.md as HTML, or null when the file or the section is missing. */
    private fun italianNotes(): String? {
        val file = italianChangelog.files.firstOrNull { it.isFile } ?: return null
        val lines = file.readLines(Charsets.UTF_8)
        val start = lines.indexOfFirst { it.startsWith("## [${pluginVersion.get()}]") }

        if (start < 0) {
            return null
        }

        val body = lines.drop(start + 1).takeWhile { !it.startsWith("## [") }.joinToString("\n").trim()

        return if (body.isEmpty()) null else markdownToHTML(body)
    }

    private fun page(language: String, css: String, markdown: String) = listOf(
        "<!DOCTYPE html>",
        "<html lang=\"$language\">",
        "<head><meta charset=\"UTF-8\"><style>$css</style></head>",
        "<body>",
        markdownToHTML(markdown),
        "</body>",
        "</html>",
    ).joinToString("\n")

    private companion object {
        // Layout only: colors and font come from the IDE theme, which the plugin appends when it opens a page
        const val STYLE = """
            body { line-height: 1.6; max-width: 860px; margin: 0 auto; padding: 16px 32px; }
            h1, h2, h3 { line-height: 1.25; }
            h2 { border-bottom: 1px solid; padding-bottom: 4px; margin-top: 32px; }
            code { font-family: "JetBrains Mono", Consolas, monospace; background: rgba(127,127,127,.15);
                   padding: 1px 4px; border-radius: 3px; }
            pre { background: rgba(127,127,127,.12); padding: 10px 14px; border-radius: 4px; overflow-x: auto; }
            pre code { background: none; padding: 0; }
            table { border-collapse: collapse; }
            th, td { border: 1px solid; padding: 4px 10px; }
            blockquote { margin-left: 0; padding-left: 14px; border-left: 4px solid; }
        """
    }
}

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
        testFramework(TestFrameworkType.Plugin.Java)
        testFramework(TestFrameworkType.Plugin.Maven)
    }

    testImplementation("junit:junit:4.13.2")
}

// The Maven server of the tests gets only the jar of one OpenTelemetry class on its classpath: the separate
// artifacts brought by the test framework hide the IDE's own merged library and the server then lacks the others
configurations.matching { it.name.startsWith("intellijPlatformTest") || it.name.startsWith("test") }.configureEach {
    exclude(group = "io.opentelemetry")
}

// Render the changelog section of the current version (or the latest one) as HTML
val currentChangeNotes: Provider<String> = project.changelog.let { changelog ->
    properties("pluginVersion").map { pluginVersion ->
        with(changelog) {
            renderItem(
                (getOrNull(pluginVersion) ?: getLatest()).withHeader(false).withEmptySections(false),
                Changelog.OutputType.HTML,
            )
        }
    }
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

        // The changelog section of the current version (or the latest one), as HTML
        changeNotes = currentChangeNotes

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

val renderGuide = tasks.register<RenderGuideTask>("renderGuide") {
    englishGuide = layout.projectDirectory.file("docs/guide.md")
    italianGuide = layout.projectDirectory.file("docs/guide.it.md")
    italianChangelog.from(layout.projectDirectory.file("CHANGELOG.it.md"))
    pluginVersion = properties("pluginVersion")
    changeNotes = currentChangeNotes
    outputDirectory = layout.buildDirectory.dir("generated/guide-resources")
}

// The generated pages end up in the plugin resources (guide/guide.html, guide/guide_it.html, guide/plugin-info.properties, guide/whats-new.html)
sourceSets.main {
    resources.srcDir(renderGuide)
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }
}
