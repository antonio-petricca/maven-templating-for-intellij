# Maven Templating assistant: user guide

Italian version: [guide.it.md](https://github.com/antonio-petricca/maven-templating-for-intellij/blob/main/docs/guide.it.md).

## What the plugin does

The official [templating-maven-plugin](https://www.mojohaus.org/templating-maven-plugin/) filters Java files placed in a template folder (by default `src/main/java-templates` and `src/test/java-templates`) and generates the real sources into the build directory. IntelliJ IDEA does not know that these folders contain sources, so you have to mark them as **Sources Root** or **Test Sources Root** by hand, and the IDE often forgets the marking when the Maven project is reimported.

The **Maven Templating assistant** does this for you:

- it declares the template folders as source roots during the Maven import, reading them from the `pom.xml`, so they are kept after every reimport;
- it keeps the folders marked when you create, rename, move, copy or delete them;
- it can exclude the generated sources to avoid duplicate classes;
- it lets you jump between a template and its generated source;
- it completes, resolves and inspects the `${...}` Maven properties used in templates.

## Requirements and installation

- IntelliJ IDEA from 2026.2 (build 262) up to 2026.3 (build 263.*). Older versions are not supported.
- The bundled **Maven** and **Java** plugins must be enabled.

Install from the Marketplace: **Settings** > **Plugins** > **Marketplace**, search for "Maven Templating assistant" and click **Install**.

Install from disk: download the [latest release](https://github.com/antonio-petricca/maven-templating-for-intellij/releases/latest), then **Settings** > **Plugins** > the gear icon > **Install plugin from disk...**.

## How template folders are detected

### Modules that declare the plugin

When the `pom.xml` (including inherited configuration) declares `org.codehaus.mojo:templating-maven-plugin`, the folders are read from its configuration. Without any custom configuration the defaults are used:

| Kind | Default folder |
|---|---|
| Template sources | `src/main/java-templates` |
| Template test sources | `src/test/java-templates` |

You can set custom folders with `sourceDirectory` (goal `filter-sources`) and `testSourceDirectory` (goal `filter-test-sources`), either at plugin level or inside an execution bound to that goal. All the values found are used.

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>templating-maven-plugin</artifactId>
  <configuration>
    <sourceDirectory>${basedir}/src/main/my-templates</sourceDirectory>
  </configuration>
  <executions>
    <execution>
      <id>filter-test-sources</id>
      <goals>
        <goal>filter-test-sources</goal>
      </goals>
      <configuration>
        <testSourceDirectory>src/test/my-test-templates</testSourceDirectory>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Relative paths are resolved against the module folder, and `${basedir}`, `${project.basedir}` and `${project.build.directory}` are supported. For these modules the folder name in the settings is not used.

### Modules without the plugin

If the plugin is not declared, the plugin falls back to the folder name set in the settings (default `java-templates`): `src/main/<name>` is marked as a source root and `src/test/<name>` as a test source root, but only when the folder exists.

### When you change folders in the IDE

The plugin watches the file system for the template folders of each Maven module:

| You do | Result |
|---|---|
| Create a template folder | It is marked as source or test source root. |
| Rename or move it | It is unmarked at the old place and marked at the new one, if the new place is still a template folder; otherwise it stops being a source root. |
| Copy it | The copy is marked if it is a template folder. |
| Delete it | The source root is removed. |

## Scanning the project

The scan looks for the template folders of every Maven module and marks the ones that exist.

- **At startup**: it runs when a project is opened if **Scan the project on startup** is on (default). No notification is shown.
- **Manually**: run **Scan Project for Templates** from **Tools** > **Maven Templating**, or from the context menu of the **Project** view. The action is available only in Maven projects.
- **After changing the folder name** in the settings: a scan runs, first unmarking the folders with the old name.

When a scan you requested ends, a notification shows how many template folders were marked (or that they were already up to date) and, if any, how many were unmarked. A progress indicator shows the Maven module being scanned. Requests arriving close to each other are merged into one scan.

## Settings

Open **Settings** > **Build, Execution, Deployment** > **Build Tools** > **Maven Templating assistant**. The page has an **Open guide** link, which opens the online guide in the browser. The settings are saved per project, except **Show what's new after updates**, which applies to all projects.

| Option | Default | Effect | When it applies |
|---|---|---|---|
| Java templates path | `java-templates` | Folder name under `src/main` and `src/test` for modules without the plugin. It must not be empty, `.` or `..`, start or end with spaces, or contain `/` or `\`. | On apply: a scan runs, unmarking the folders with the old name first. |
| Scan the project on startup | on | Scans the project each time it is opened. | At the next project opening. |
| Exclude generated template sources | off | Excludes the generated output folders, see the next section. | After a Maven reimport. On apply, a notification offers a **Reimport** button. |
| Show what's new after updates | on | Opens the **What's New** tab after a plugin update (and after the first installation). Applies to all projects. | After the next update. |

## Excluding generated sources

The plugin generates copies of the templates into the build directory, so the same classes exist twice and the IDE can report **Duplicate class** errors. With **Exclude generated template sources** on, those output folders are excluded from the project:

- `target/generated-sources/java-templates` and `target/generated-test-sources/java-templates` (defaults);
- the folders set with `outputDirectory` (goal `filter-sources`) and `testOutputDirectory` (goal `filter-test-sources`), at plugin or execution level.

The exclusion is declared during the Maven import, so it only takes effect after a reimport; the notification shown when you change the option has a **Reimport** button. It applies to modules that declare the plugin.

## Navigating between templates and generated sources

For a Java file in a template folder, the plugin finds the generated file with the same relative path in the output folder, and the other way round. The generated file must exist on disk, so build the project first.

- **Gutter icon** next to the top-level class name, with the tooltip **Go to Generated Source** or **Go to Template**.
- **Navigate** > **Go to Generated Source / Go to Template**.
- The same entry in the editor context menu.

The action is visible only when a counterpart exists.

## Maven properties in templates

In the Java files of a template folder, `${name}` occurrences inside string literals and comments are treated as Maven properties.

- **Resolution**: Ctrl+click on a property jumps to its definition. Properties are looked up in the `<properties>` of the module pom and of its parent poms (the nearest wins), then among the model properties `project.version`, `project.groupId`, `project.artifactId`, `project.name`, `project.description`, `project.basedir` and `project.build.directory`, then among the properties already resolved by Maven (profiles, config files and so on, with nothing to jump to).
- **Completion**: after typing `${`, the completion list offers the properties declared in the pom (marked `pom.xml`) and the model or resolved ones (marked `Maven model`).
- **Inspection**: the **Unresolved Maven property in template** inspection (warning, on by default, group **Maven Templating assistant**) reports properties that cannot be found, with the message `Cannot resolve Maven property 'name'`. Properties still being typed (no closing brace) are not reported.
- **Quick fix**: **Add property to pom.xml** adds an empty `<name/>` tag to the `<properties>` of the module pom (creating the section if needed), ready for you to fill in.
- **Ignored properties**: names starting with `env.`, `java.`, `os.`, `user.`, `settings.`, `maven.`, `project.` or `pom.`, `basedir`, and any JVM system property are never reported.

## What's New tab

After the plugin is updated, a **What's New** tab opens with the changes of the version, shown in the language of the IDE, and a link that opens this guide as bundled with the plugin. The tab also opens after the first installation, together with a notification with an **Open Guide** action. Both are skipped when **Show what's new after updates** is off. You can also open the tab from **Tools** > **Maven Templating** > **What's New**.

## Troubleshooting and FAQ

**A template folder is not marked.**
Run **Tools** > **Maven Templating** > **Scan Project for Templates**, or reimport the Maven project. Check that the folder exists, that it matches the path in the plugin configuration (or the name in the settings, for modules without the plugin) and that it belongs to a Maven module.

**I get "Duplicate class" errors.**
Turn on **Exclude generated template sources** in the settings and reimport the Maven project.

**The plugin does nothing in my project.**
It works only with Maven projects: the scan action is hidden otherwise.

**The settings page and messages are in Italian.**
This happens when the IDE uses the Italian language pack.

**Where is the guide?**
In **Help** > **Maven Templating Guide**, **Tools** > **Maven Templating** > **Open Guide**, or the **Open guide** link in the settings.

**Is it safe on future IDE versions?**
To declare the folders during the import, the plugin uses experimental APIs of the Maven plugin. They are verified with the Plugin Verifier on every supported release, which is why the compatibility range is limited.

## Links

- Repository: https://github.com/antonio-petricca/maven-templating-for-intellij
- Issue tracker: https://github.com/antonio-petricca/maven-templating-for-intellij/issues
- Changelog: [CHANGELOG.md](https://github.com/antonio-petricca/maven-templating-for-intellij/blob/main/CHANGELOG.md)
