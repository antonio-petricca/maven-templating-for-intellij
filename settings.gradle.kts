pluginManagement {
    repositories {
        // Optional local repository, used only when present (not on CI)
        val localRepo = File(System.getProperty("user.home"), ".gradle/local-plugin-repo")
        if (localRepo.isDirectory) {
            maven {
                url = localRepo.toURI()
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Downloads the JDK required by the JVM toolchain when it is not installed locally
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "maven-templating-for-intellij"
