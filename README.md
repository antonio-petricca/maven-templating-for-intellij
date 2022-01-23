# maven-templating-for-intellij

![Build](https://github.com/antonio-petricca/maven-templating-for-intellij/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/18410.svg)](https://plugins.jetbrains.com/plugin/18410)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/18410.svg)](https://plugins.jetbrains.com/plugin/18410)

## Template ToDo list
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [x] Get familiar with the [template documentation][template].
- [x] Verify the [pluginGroup](/gradle.properties), [plugin ID](/src/main/resources/META-INF/plugin.xml) and [sources package](/src/main/kotlin).
- [x] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html).
- [x] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [x] Set the Plugin ID in the above README badges.
- [x] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html).
- [x] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.

<!-- Plugin description -->
This plugin adds support for the [Maven Templating Plugin](https://www.mojohaus.org/templating-maven-plugin/).

Using **Maven Templating Plugin** you have to create your own `java-templates` folder and mark them as **Sources Root** or **Test Sources Root**.

Unfortunately IntelliJ often looses those settings, especially when you import again the maven project. For huge projects, manually reconfiguring folders, are boring and time-consuming. 

The **Maven Templating Plugin fo IJ** does all for you:

- Managing templates folders on adding/removing/moving/renaming.
- Auto scan project on project opening. 
- Auto scan project on maven project (re)import.
- Manually allow you, by **Project View Context** menù or **Tools** menù, to enforce a project scan. 

The default `java-templates` folder name can be customized by the settings page under **Build Tools** section.
<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "maven-templating-for-intellij"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/antonio-petricca/maven-templating-for-intellij/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
