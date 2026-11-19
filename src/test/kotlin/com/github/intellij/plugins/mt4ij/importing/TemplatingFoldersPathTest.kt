package com.github.intellij.plugins.mt4ij.importing

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class TemplatingFoldersPathTest {

    // An absolute path valid on the OS running the tests, with '/' as separator
    private val baseDir  = File("project-root").absoluteFile.path.replace('\\', '/')
    private val buildDir = "$baseDir/target"

    @Test
    fun relativePathIsResolvedAgainstTheBaseDirectory() {
        assertEquals("$baseDir/src/main/tpl", TemplatingFolders.resolvePath("src/main/tpl", baseDir))
    }

    @Test
    fun basedirPlaceholdersAreReplaced() {
        assertEquals("$baseDir/src/main/tpl", TemplatingFolders.resolvePath("\${basedir}/src/main/tpl", baseDir))
        assertEquals("$baseDir/src/main/tpl", TemplatingFolders.resolvePath("\${project.basedir}/src/main/tpl", baseDir))
    }

    @Test
    fun buildDirectoryPlaceholderIsReplaced() {
        assertEquals(
            "$buildDir/generated-sources/java-templates",
            TemplatingFolders.resolvePath("\${project.build.directory}/generated-sources/java-templates", baseDir, buildDir)
        )
    }

    @Test
    fun windowsSeparatorsAreNormalized() {
        assertEquals("$baseDir/src/main/tpl", TemplatingFolders.resolvePath("src\\main\\tpl", baseDir))
    }

    @Test
    fun dotSegmentsAreNormalized() {
        assertEquals("$baseDir/src/main/tpl", TemplatingFolders.resolvePath("src/test/../main/./tpl", baseDir))
    }

    @Test
    fun absolutePathIsKeptAsIs() {
        val absolute = "$baseDir/elsewhere/tpl"

        assertEquals(absolute, TemplatingFolders.resolvePath(absolute, "$baseDir/other"))
    }
}
