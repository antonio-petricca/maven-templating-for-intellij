package com.github.intellij.plugins.mt4ij.importing

import org.junit.Test
import com.github.intellij.plugins.mt4ij.MavenTemplatingTestCase
import kotlinx.coroutines.runBlocking

class TemplatingFoldersImportTest : MavenTemplatingTestCase() {

    private fun createJavaFolders(base: String = "") {
        val prefix = if (base.isEmpty()) "" else "$base/"

        createProjectSubDirs("${prefix}src/main/java", "${prefix}src/test/java")
    }

    @Test
    fun testDefaultTemplatesFoldersAreSourceRoots() = runBlocking {
        createJavaFolders()
        createProjectSubDirs("src/main/java-templates", "src/test/java-templates")

        importProjectAsync(pomWithPlugin("project"))

        assertContainsElements(sourceRoots("project", false), "src/main/java", "src/main/java-templates")
        assertContainsElements(sourceRoots("project", true), "src/test/java", "src/test/java-templates")
    }

    @Test
    fun testTemplatesFoldersSurviveAReimport() = runBlocking {
        createJavaFolders()
        createProjectSubDirs("src/main/java-templates", "src/test/java-templates")

        importProjectAsync(pomWithPlugin("project"))
        reimport()

        assertTrue("roots: ${sourceRoots("project", false)} / src/main/java-templates", sourceRoots("project", false).contains("src/main/java-templates"))
        assertTrue("roots: ${sourceRoots("project", true)} / src/test/java-templates", sourceRoots("project", true).contains("src/test/java-templates"))
    }

    @Test
    fun testCustomFoldersFromPluginConfigurationAreSourceRoots() = runBlocking {
        createJavaFolders()
        createProjectSubDirs("src/main/tpl", "src/test/tpl")

        val configuration = """
            <configuration>
                <sourceDirectory>${'$'}{basedir}/src/main/tpl</sourceDirectory>
                <testSourceDirectory>src/test/tpl</testSourceDirectory>
            </configuration>
        """.trimIndent()

        importProjectAsync(pomWithPlugin("project", configuration))

        assertTrue("roots: ${sourceRoots("project", false)} / src/main/tpl", sourceRoots("project", false).contains("src/main/tpl"))
        assertTrue("roots: ${sourceRoots("project", true)} / src/test/tpl", sourceRoots("project", true).contains("src/test/tpl"))
        assertFalse("roots: ${sourceRoots("project", false)} / src/main/java-templates", sourceRoots("project", false).contains("src/main/java-templates"))
    }

    @Test
    fun testFolderNameFromSettingsIsUsedWhenThePluginIsNotDeclared() = runBlocking {
        createJavaFolders()
        createProjectSubDirs("src/main/java-templates", "src/test/java-templates")

        importProjectAsync(pom("project"))

        assertTrue("roots: ${sourceRoots("project", false)} / src/main/java-templates", sourceRoots("project", false).contains("src/main/java-templates"))
        assertTrue("roots: ${sourceRoots("project", true)} / src/test/java-templates", sourceRoots("project", true).contains("src/test/java-templates"))
    }

    @Test
    fun testCustomFolderNameFromSettingsIsUsedWhenThePluginIsNotDeclared() = runBlocking {
        createJavaFolders()
        createProjectSubDirs("src/main/tpl", "src/main/java-templates")
        settings().templatesPath = "tpl"

        importProjectAsync(pom("project"))

        assertTrue("roots: ${sourceRoots("project", false)} / src/main/tpl", sourceRoots("project", false).contains("src/main/tpl"))
        assertFalse("roots: ${sourceRoots("project", false)} / src/main/java-templates", sourceRoots("project", false).contains("src/main/java-templates"))
    }

    @Test
    fun testMissingFoldersAreNotSourceRootsWhenThePluginIsNotDeclared() = runBlocking {
        createJavaFolders()

        importProjectAsync(pom("project"))

        assertFalse("roots: ${sourceRoots("project", false)} / src/main/java-templates", sourceRoots("project", false).contains("src/main/java-templates"))
        assertFalse("roots: ${sourceRoots("project", true)} / src/test/java-templates", sourceRoots("project", true).contains("src/test/java-templates"))
    }

    @Test
    fun testEachModuleGetsItsOwnTemplatesFolders() = runBlocking {
        createJavaFolders("m1")
        createJavaFolders("m2")
        createProjectSubDirs("m1/src/main/java-templates", "m1/src/test/java-templates", "m2/src/main/java-templates")

        createProjectPom(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0</version>
            <packaging>pom</packaging>
            <modules>
                <module>m1</module>
                <module>m2</module>
            </modules>
            """.trimIndent()
        )
        createModulePom("m1", pomWithPlugin("m1"))
        createModulePom("m2", pomWithPlugin("m2"))

        importProjectAsync()

        assertTrue("roots: ${sourceRoots("m1", false, "m1")} / src/main/java-templates", sourceRoots("m1", false, "m1").contains("src/main/java-templates"))
        assertTrue("roots: ${sourceRoots("m1", true, "m1")} / src/test/java-templates", sourceRoots("m1", true, "m1").contains("src/test/java-templates"))
        assertTrue("roots: ${sourceRoots("m2", false, "m2")} / src/main/java-templates", sourceRoots("m2", false, "m2").contains("src/main/java-templates"))
        assertFalse("roots: ${sourceRoots("m1", false, "m1")} / ../m2/src/main/java-templates", sourceRoots("m1", false, "m1").contains("../m2/src/main/java-templates"))
    }

    // Maven turns an existing target/generated-sources/<folder> into a generated source root:
    // the exclusion option must take it away, so that the template classes are not seen twice
    private val generatedMain = "target/generated-sources/java-templates"
    private val generatedTest = "target/generated-test-sources/java-templates"

    private fun importWithGeneratedFolders(exclude: Boolean) = runBlocking {
        createJavaFolders()
        createProjectSubDirs("src/main/java-templates", generatedMain, generatedTest)
        settings().excludeGeneratedSources = exclude

        importProjectAsync(pomWithPlugin("project"))
        reimport()
    }

    @Test
    fun testGeneratedFoldersAreNotSourceRootsWhenTheOptionIsEnabled() {
        importWithGeneratedFolders(exclude = true)

        assertFalse("roots: ${sourceRoots("project", false)}", sourceRoots("project", false).contains(generatedMain))
        assertFalse("roots: ${sourceRoots("project", true)}", sourceRoots("project", true).contains(generatedTest))
    }

    @Test
    fun testGeneratedFoldersStayGeneratedSourceRootsWhenTheOptionIsDisabled() {
        importWithGeneratedFolders(exclude = false)

        assertTrue("roots: ${sourceRoots("project", false)}", sourceRoots("project", false).contains(generatedMain))
        assertTrue("roots: ${sourceRoots("project", true)}", sourceRoots("project", true).contains(generatedTest))
    }
}
