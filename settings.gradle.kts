pluginManagement {
    repositories {
        maven {
            url = uri("${System.getProperty("user.home")}/.gradle/local-plugin-repo")
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
