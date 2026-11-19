package com.github.intellij.plugins.mt4ij

import com.intellij.notification.Notification
import com.intellij.openapi.application.ApplicationManager
import com.intellij.notification.Notifications
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

class TemplatesScannerTest : MavenTemplatingTestCase() {

    private val scanner get() = project.service<TemplatesScanner>()

    private fun folder(relativePath: String): VirtualFile =
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(projectRoot.path, relativePath))!!

    // Roots are changed on the EDT, as the plugin does
    private fun applyChanges(changes: List<SourceFolderChange>): AppliedChanges {
        var applied = AppliedChanges(0, 0)

        ApplicationManager.getApplication().invokeAndWait { applied = SourceFolders.apply(changes) }

        return applied
    }

    private fun unmark(moduleName: String, relativePath: String, isTest: Boolean = false) =
        applyChanges(listOf(SourceFolderChange.remove(getModule(moduleName), folder(relativePath), isTest)))

    private fun notifications(): MutableList<Notification> {
        val received = CopyOnWriteArrayList<Notification>()

        project.messageBus.connect(testRootDisposable).subscribe(Notifications.TOPIC, object : Notifications {
            override fun notify(notification: Notification) {
                if (notification.groupId == "mt4ij.notifications") {
                    received += notification
                }
            }
        })

        return received
    }

    @Test
    fun testScanMarksTheFolderOfThePluginConfiguration() = runBlocking {
        createProjectSubDirs("src/main/java", "src/main/tpl")
        val configuration = "<configuration><sourceDirectory>src/main/tpl</sourceDirectory></configuration>"
        importProjectAsync(pomWithPlugin("project", configuration))
        unmark("project", "src/main/tpl")
        assertFalse(sourceRoots("project", false).contains("src/main/tpl"))

        scanner.requestScan()

        awaitCondition("src/main/tpl marked as source") { sourceRoots("project", false).contains("src/main/tpl") }
    }

    @Test
    fun testScanMarksTheFolderNamedInTheSettingsWhenThePluginIsNotDeclared() = runBlocking {
        createProjectSubDirs("src/main/java", "src/test/java", "src/main/java-templates", "src/test/java-templates")
        importProjectAsync(pom("project"))
        unmark("project", "src/main/java-templates")
        unmark("project", "src/test/java-templates", isTest = true)

        scanner.requestScan()

        awaitCondition("both templates folders marked") {
            sourceRoots("project", false).contains("src/main/java-templates") &&
            sourceRoots("project", true).contains("src/test/java-templates")
        }
    }

    @Test
    fun testRenamingTheSettingUnmarksOnlyTheModulesWithoutThePluginDeclared() = runBlocking {
        createProjectSubDirs("m1/src/main/java", "m2/src/main/java", "m1/src/main/java-templates", "m2/src/main/java-templates")
        createProjectPom(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1.0</version>
            <packaging>pom</packaging>
            <modules><module>m1</module><module>m2</module></modules>
            """.trimIndent()
        )
        createModulePom("m1", pom("m1"))
        createModulePom("m2", pomWithPlugin("m2"))
        importProjectAsync()

        assertTrue(sourceRoots("m1", false, "m1").contains("src/main/java-templates"))
        assertTrue(sourceRoots("m2", false, "m2").contains("src/main/java-templates"))

        settings().templatesPath = "tpl"
        scanner.requestScan(previousName = "java-templates")

        awaitCondition("the old folder of m1 unmarked") { !sourceRoots("m1", false, "m1").contains("src/main/java-templates") }
        assertTrue("m2 declares the plugin: its folder must stay", sourceRoots("m2", false, "m2").contains("src/main/java-templates"))
    }

    @Test
    fun testCloseRequestsAreMergedIntoOneScan() = runBlocking {
        createProjectSubDirs("src/main/java")
        importProjectAsync(pom("project"))
        val received = notifications()

        repeat(5) { scanner.requestScan(notify = true) }
        awaitCondition("the first summary") { received.isNotEmpty() }

        // This request is served after any other scan that is still pending
        scanner.requestScan(notify = true)
        awaitCondition("the summary of the last request") { received.size >= 2 }

        assertEquals("one scan for the five requests, one for the last", 2, received.size)
    }

    @Test
    fun testAppliedChangesAreCounted() = runBlocking {
        createProjectSubDirs("src/main/java", "src/test/java", "src/main/java-templates", "src/test/java-templates")
        importProjectAsync(pom("project"))
        val module = getModule("project")

        val removed = applyChanges(listOf(
            SourceFolderChange.remove(module, folder("src/main/java-templates"), false),
            SourceFolderChange.remove(module, folder("src/test/java-templates"), true),
        ))
        val added = applyChanges(listOf(
            SourceFolderChange.add(module, folder("src/main/java-templates"), false),
            SourceFolderChange.add(module, folder("src/test/java-templates"), true),
        ))
        val unchanged = applyChanges(listOf(
            SourceFolderChange.add(module, folder("src/main/java-templates"), false),
            SourceFolderChange.add(module, folder("src/test/java-templates"), true),
        ))

        assertEquals(AppliedChanges(added = 0, removed = 2), removed)
        assertEquals(AppliedChanges(added = 2, removed = 0), added)
        assertEquals("already marked folders are not counted", AppliedChanges(added = 0, removed = 0), unchanged)
    }

    @Test
    fun testScanWithAnInvalidNameLeavesTheFoldersAlone() = runBlocking {
        createProjectSubDirs("src/main/java", "src/main/java-templates")
        importProjectAsync(pom("project"))
        val received = notifications()

        settings().templatesPath = ".."
        scanner.requestScan(notify = true)
        awaitCondition("the summary") { received.isNotEmpty() }

        assertTrue(sourceRoots("project", false).contains("src/main/java-templates"))
        assertFalse(sourceRoots("project", false).any { it.startsWith("..") })
    }
}
