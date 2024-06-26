# maven-templating-for-intellij Changelog

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
