package com.github.intellij.plugins.mt4ij.listeners

import com.github.intellij.plugins.mt4ij.MavenTemplatingTestCase
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File

class VFSListenerTest : MavenTemplatingTestCase() {

    private val mainRoot = "src/main/java-templates"

    private fun importTemplatesProject() = runBlocking {
        createProjectSubDirs("src/main/java", "src/other")
        importProjectAsync(pom("project"))
    }

    private fun createDirectory(relativePath: String): VirtualFile = WriteAction.computeAndWait<VirtualFile, Exception> {
        VfsUtil.createDirectories(File(projectRoot.path, relativePath).path)
    }

    private fun existing(relativePath: String): VirtualFile =
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(projectRoot.path, relativePath))!!

    // The listener changes the roots in a later event: wait for the event queue to be served
    private fun isMarked(root: String = mainRoot): Boolean {
        flushEventQueue()

        return sourceRoots("project", false).contains(root)
    }

    @Test
    fun testCreatingTheTemplatesFolderMarksIt() {
        importTemplatesProject()

        createDirectory(mainRoot)

        assertTrue("roots: ${sourceRoots("project", false)}", isMarked())
    }

    @Test
    fun testCreatingTheTestTemplatesFolderMarksItAsTestSource() {
        importTemplatesProject()

        createDirectory("src/test/java-templates")
        flushEventQueue()

        assertTrue("roots: ${sourceRoots("project", true)}", sourceRoots("project", true).contains("src/test/java-templates"))
    }

    @Test
    fun testRenamingAFolderToTheTemplatesNameMarksIt() {
        importTemplatesProject()
        val folder = createDirectory("src/main/draft")

        WriteAction.runAndWait<Exception> { folder.rename(this, "java-templates") }

        assertTrue("roots: ${sourceRoots("project", false)}", isMarked())
    }

    @Test
    fun testRenamingTheTemplatesFolderUnmarksIt() {
        importTemplatesProject()
        val folder = createDirectory(mainRoot)
        assertTrue(isMarked())

        WriteAction.runAndWait<Exception> { folder.rename(this, "draft") }

        assertFalse("roots: ${sourceRoots("project", false)}", isMarked())
        assertFalse("roots: ${sourceRoots("project", false)}", isMarked("src/main/draft"))
    }

    @Test
    fun testMovingAFolderIntoPlaceMarksItAndMovingItAwayUnmarksIt() {
        importTemplatesProject()
        val folder = createDirectory("src/other/java-templates")
        assertFalse(isMarked("src/other/java-templates"))

        WriteAction.runAndWait<Exception> { folder.move(this, existing("src/main")) }
        assertTrue("roots: ${sourceRoots("project", false)}", isMarked())

        WriteAction.runAndWait<Exception> { existing(mainRoot).move(this, existing("src/other")) }
        assertFalse("roots: ${sourceRoots("project", false)}", isMarked())
    }

    @Test
    fun testCopyingAFolderIntoPlaceMarksTheCopy() {
        importTemplatesProject()
        val folder = createDirectory("src/other/java-templates")

        WriteAction.runAndWait<Exception> { folder.copy(this, existing("src/main"), "java-templates") }

        assertTrue("roots: ${sourceRoots("project", false)}", isMarked())
    }

    @Test
    fun testDeletingTheTemplatesFolderUnmarksIt() {
        importTemplatesProject()
        val folder = createDirectory(mainRoot)
        assertTrue(isMarked())

        WriteAction.runAndWait<Exception> { folder.delete(this) }

        assertFalse("roots: ${sourceRoots("project", false)}", isMarked())
    }

    @Test
    fun testAFileWithTheTemplatesNameIsNotMarked() {
        importTemplatesProject()

        WriteAction.runAndWait<Exception> { existing("src/main").createChildData(this, "java-templates") }

        assertFalse("roots: ${sourceRoots("project", false)}", isMarked())
    }

    @Test
    fun testFoldersWithOtherNamesAreNotMarked() {
        importTemplatesProject()

        createDirectory("src/main/not-templates")
        createDirectory("src/main/java/java-templates")

        assertFalse(isMarked("src/main/not-templates"))
        assertFalse(isMarked("src/main/java/java-templates"))
    }
}
