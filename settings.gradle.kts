pluginManagement {
    repositories {
        maven {
            url = uri("${System.getProperty("user.home")}/.gradle/local-plugin-repo")
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "maven-templating-for-intellij"
