# maven-templating-for-intellij Changelog

## [1.5.1]

- Declared compatibility with 2025.1.

## [1.5.0]
        
- Added a project scan progress indicator.   
- Allowed "intellij" word into plugin id.
- Fixed: "Deprecated class ProjectFileIndex.SERVICE is referenced in ApiHelpers.Companion.getModelForFile$lambda$0(...)".
- Fixed: "Deprecated class ServiceManager is referenced in SettingsStorage.Companion.getInstance(Project)".
- Fixed: "Deprecated constructor URL.<init>(String) is invoked in VFSListener.doMoveSourceFolderBefore(...)" and "Deprecated constructor URL.<init>(String) is invoked in VFSListener.doMoveSourceFolderAfter(...)".
- Fixed: "Deprecated constructor java.net.URL.<init>(java.lang.String spec) is invoked in com.github.intellij.plugins.mt4ij.listeners.VFSListener.doMoveSourceFolderAfter(Project, String, VirtualFile, boolean) : void</init>".
- Fixed: "Deprecated method MavenProjectsManager.addManagerListener(...) is invoked in ProjectStartupActivity.registerMavenListener(...)".
- Fixed: "Deprecated method ServiceManager.getService(Project, Class) is invoked in SettingsStorage.Companion.getInstance(Project)".
- Fixed: "Internal method Module.getModuleTypeName() is invoked in ApiHelpers.Companion.getModelForFile(...). This method is marked with @ApiStatus.Internal annotation or @IntellijInternalApi annotation and indicates that the method is not supposed to be used in client code.".
- Minor code cleanup.
- Settings form: fixed thread context invoke.                                       
- Settings form: not showing fixed.                                       
- Settings form: updated Build Tools label. 
- Updated `README.md` file.

## [1.4.5]

- Declared compatibility with 2024.3.

## [1.4.4]

- Declared compatibility with 2024.2.

## [1.4.3]

- Fixed compatibility with 2024.1.

## [1.4.2]

- Migrated com.github.intellij.plugins.mt4ij.activities.ProjectStartupActivity to ProjectActivity.

## [1.4.1]

- Declared compatibility with 2024.1.

## [1.4.0]

- Declared compatibility with #IC-233.*.

## [1.3.1]

- Fix for "com.intellij.diagnostic.PluginException: No display name is specified for configurable com.github.intellij.plugins.mt4ij.config.SettingsConfigurable in xml file;".

## [1.3.0]

- Declared compatibility with #IC-232.*.

## [1.2.1]

- CHANGELOG not showing 1.2.0 changes fixup.
- Fixed: "Must not use `executable` property on `Test` together with `javaLauncher` property".

## [1.2.0]

- Gradle plugins upgrade.
- Fixed: "java.lang.Throwable: Read access is allowed from inside read-action (or EDT) only (see com.intellij.openapi.application.Application.runReadAction())".

## [1.1.1]

- Declared compatibility with #IC-231.*.

## [1.1.0]

- Declared compatibility with #IC-223.*.
- 
## [1.0.11]

- Declared compatibility with #IC-222.*.

## [1.0.10]

- Declared compatibility with #IC-221.5080.210 (2022.1).

## [1.0.9]

- Declared compatibility with #IC-221.5080.210 (2022.1).

## [1.0.8]

- Implemented listener for Maven project import event.
- Improved dependency from Java IDE and Maven plugin.
- Improved usage instructions.
- Improved/Fixed VFS listener.
- Typo fix.

## [1.0.7]

- Typo fix.

## [1.0.6]

- Implemented listener for Maven project import event.
- Improved dependency from Java IDE and Maven plugin.
- Improved usage instructions.
- Improved/Fixed VFS listener.

## [1.0.5]

- Added project scanning as menu item into project view popup menù.

## [1.0.4]

- Enabled QODANA build for **develop** branch too.
- Fixed QODANA hints.
- Managed (again) get active project timeout.

## [1.0.3]

- Fixed CHANGELOG for missing **Unreleased** which causes build failure on **GitHub**.

## [1.0.2]

- Added project scanning on settings apply.
- Added project scanning as menu item into **Tools** menù.
- Made settings panel searchable.
- Reduced get active project timeout.

## [1.0.1]

- Added the post load project scanning for missing source/test folders.
- Fixed get active project timeout exception.
- Missing `:` on configuration panel label.

## [1.0.0]

### Changed

- Fixed unsafe project source folders updates.
- Moved settings file **mt4ij.xml** to **.idea** project folder (Thank you to **Vojtěch Krása** and **Petr Makhnev**).

## [0.0.3]

### Changed

- Added **org.jetbrains.idea.maven** dependency declaration.
- Customized plugin icon.

## [0.0.2]

### Changed

- Fixed folder name detection from **contains** to **endsWith**.

## [0.0.1]

### Added

- Initial plugin revision (it contains some small architectural bugs).
