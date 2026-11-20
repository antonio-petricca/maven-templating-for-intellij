# maven-templating-for-intellij

[![Build](https://github.com/antonio-petricca/maven-templating-for-intellij/actions/workflows/build.yml/badge.svg)](https://github.com/antonio-petricca/maven-templating-for-intellij/actions/workflows/build.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/18410.svg)](https://plugins.jetbrains.com/plugin/18410)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/18410.svg)](https://plugins.jetbrains.com/plugin/18410)

<!-- Plugin description -->
This plugin is an assistant for the official [Maven Templating Plugin](https://www.mojohaus.org/templating-maven-plugin/).

Without using the **Maven Templating assistant**, you have to create your own `java-templates` folders and mark them as **Sources Root** or **Test Sources Root**.

Unfortunately, IntelliJ often loses those settings, especially when you re-import the Maven project. For large projects, manually reconfiguring folders is tedious and time-consuming.

The **Maven Templating assistant** does all this for you:

- Declares the template folders (sources and test sources) during the Maven project import, reading them from the `pom.xml`.
- Manages template folders when adding/removing/moving/renaming.
- Optionally scans the project on project opening, and shows a notification with the result when a scan you requested ends.
- Allows you to enforce a project scan via the **Maven Templating** submenu of the right-click menu of the **Project** view (or of the editor and of the editor tabs) or via the **Tools | Maven Templating** menu.
- Includes a user guide, available from the **Help** and **Tools | Maven Templating** menus, and shows what's new after each update.
- Excludes the generated sources (output folders of the templating-maven-plugin), on by default, to avoid duplicate classes; it takes effect after a Maven reimport.
- Navigates between a template and its generated source, with distinct gutter icons, a banner in both files (also before the first build, to remind you to build the project), the Ctrl+Alt+Shift+Space shortcut (Cmd+Alt+Shift+G on macOS), the **Navigate** menu and the **Maven Templating** submenu of the right-click menus (editor, editor tab and **Project** view).
- Completes, resolves and inspects the `${...}` Maven properties used in templates, with a quick fix that adds a missing property to the `pom.xml` and opens it with the cursor in the empty value.

The folder name used by modules that don't declare the templating-maven-plugin (default `java-templates`; for the others the pom decides), the scan on startup and the exclusion of generated sources can be customized on the settings page under the **Build Tools** section. The plugin texts are also available in Italian, when the IDE uses an Italian language pack.

The plugin requires the bundled **Maven** and **Java** plugins.

Read the [user guide](https://github.com/antonio-petricca/maven-templating-for-intellij/blob/main/docs/guide.md) for all the details ([Italiano](https://github.com/antonio-petricca/maven-templating-for-intellij/blob/main/docs/guide.it.md)).
<!-- Plugin description end -->

## Compatibility

- Since build 262 (IntelliJ IDEA 2026.2) up to build 263.* (IntelliJ IDEA 2026.3).
- Verified with the Plugin Verifier against IntelliJ IDEA 2026.2 and 2026.3.
- Requires the bundled Maven (`org.jetbrains.idea.maven`) and Java plugins.
- The plugin uses experimental APIs of the Maven plugin to declare the folders during the import; they are verified on every supported release.

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Maven Templating assistant"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/antonio-petricca/maven-templating-for-intellij/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Building

Versions are defined in `gradle.properties` and `gradle/libs.versions.toml`.

- Build the plugin distribution: `./gradlew buildPlugin`
- Verify compatibility with the configured IDEs: `./gradlew verifyPlugin`

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
