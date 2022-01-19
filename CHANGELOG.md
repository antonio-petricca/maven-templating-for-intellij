# maven-templating-for-intellij Changelog

## [Unreleased]

- Fixed QODANA hints.
- Managed (again) get active project timeout.

## [1.0.3]

- Fixed CHANGELOG for missing **Unreleased** which causes build failure on **github**.

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
