package com.github.intellij.plugins.mt4ij.importing

import com.github.intellij.plugins.mt4ij.MavenTemplatingTestCase
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TemplatingFoldersTest : MavenTemplatingTestCase() {

    private fun root(path: String) = "${projectRoot.path}/$path"

    @Test
    fun testBlankAndRelativeNamesAreNotValid() {
        assertFalse(TemplatingFolders.isValidName(""))
        assertFalse(TemplatingFolders.isValidName("   "))
        assertFalse(TemplatingFolders.isValidName("."))
        assertFalse(TemplatingFolders.isValidName(".."))
        assertTrue(TemplatingFolders.isValidName("java-templates"))
        assertTrue(TemplatingFolders.isValidName("tpl.d"))
    }

    @Test
    fun testInvalidNamesGiveNoSourceFolders() = runBlocking {
        importProjectAsync(pom("project"))
        val mavenProject = mavenProjectOf()

        listOf("", "  ", ".", "..").forEach { name ->
            val folders = TemplatingFolders.of(mavenProject, name)

            assertTrue("sources for [$name]: ${folders.sources}", folders.sources.isEmpty())
            assertTrue("test sources for [$name]: ${folders.testSources}", folders.testSources.isEmpty())
        }
    }

    @Test
    fun testNameFromTheSettingsBuildsTheSourceFoldersWhenThePluginIsNotDeclared() = runBlocking {
        importProjectAsync(pom("project"))

        val folders = TemplatingFolders.of(mavenProjectOf(), "tpl")

        assertFalse(folders.isPluginDeclared)
        assertEquals(listOf(root("src/main/tpl")), folders.sources)
        assertEquals(listOf(root("src/test/tpl")), folders.testSources)
    }

    @Test
    fun testDefaultFoldersAndOutputsWhenThePluginHasNoConfiguration() = runBlocking {
        importProjectAsync(pomWithPlugin("project"))

        val folders = TemplatingFolders.of(mavenProjectOf(), "ignored")

        assertTrue(folders.isPluginDeclared)
        assertEquals(listOf(root("src/main/java-templates")), folders.sources)
        assertEquals(listOf(root("src/test/java-templates")), folders.testSources)
        assertEquals(listOf(root("target/generated-sources/java-templates")), folders.outputs)
        assertEquals(listOf(root("target/generated-test-sources/java-templates")), folders.testOutputs)
    }

    @Test
    fun testCustomOutputDirectoriesAreUsed() = runBlocking {
        val configuration = """
            <configuration>
                <outputDirectory>${'$'}{project.build.directory}/gen/main</outputDirectory>
                <testOutputDirectory>out/test</testOutputDirectory>
            </configuration>
        """.trimIndent()
        importProjectAsync(pomWithPlugin("project", configuration))

        val folders = TemplatingFolders.of(mavenProjectOf())

        assertEquals(listOf(root("target/gen/main")), folders.outputs)
        assertEquals(listOf(root("out/test")), folders.testOutputs)
    }

    @Test
    fun testFoldersOfAllTheExecutionsAreCollected() = runBlocking {
        val build = """
            <build><plugins><plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>templating-maven-plugin</artifactId>
                <version>3.0.0</version>
                <executions>
                    <execution><id>a</id><goals><goal>filter-sources</goal></goals>
                        <configuration><sourceDirectory>src/main/a</sourceDirectory></configuration></execution>
                    <execution><id>b</id><goals><goal>filter-sources</goal></goals>
                        <configuration><sourceDirectory>src/main/b</sourceDirectory></configuration></execution>
                </executions>
            </plugin></plugins></build>
        """.trimIndent()
        importProjectAsync(pom("project", build))

        val folders = TemplatingFolders.of(mavenProjectOf())

        assertEquals(listOf(root("src/main/a"), root("src/main/b")), folders.sources)
    }
}
