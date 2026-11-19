package com.github.intellij.plugins.mt4ij

import com.github.intellij.plugins.mt4ij.config.SettingsStorage
import com.github.intellij.plugins.mt4ij.properties.MavenPropertyReference
import com.github.intellij.plugins.mt4ij.properties.TemplateProperties
import com.github.intellij.plugins.mt4ij.properties.UnresolvedMavenPropertyInspection
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ex.QuickFixWrapper
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.jetbrains.idea.maven.project.MavenProject
import com.intellij.maven.testFramework.MavenMultiVersionImportingTestCase
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.junit.runners.Parameterized
import java.nio.file.Path

/** Base class of the tests that import a Maven project with the templating-maven-plugin. */
abstract class MavenTemplatingTestCase : MavenMultiVersionImportingTestCase() {

    companion object {
        // Same name as the inherited provider, so that only the bundled Maven runs
        @Parameterized.Parameters(name = "with Maven-{0} and model-{1}")
        @JvmStatic
        fun getMavenVersions(): List<Array<String>> = listOf(arrayOf("bundled", "4.0.0"))
    }

    protected val pluginDeclaration = """
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>templating-maven-plugin</artifactId>
            <version>3.0.0</version>
            <executions>
                <execution>
                    <goals>
                        <goal>filter-sources</goal>
                        <goal>filter-test-sources</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    """.trimIndent()

    protected fun pom(artifactId: String, build: String = "", extra: String = "") = """
        <groupId>test</groupId>
        <artifactId>$artifactId</artifactId>
        <version>1.0</version>
        $extra
        $build
    """.trimIndent()

    protected fun pomWithPlugin(artifactId: String, pluginConfiguration: String = "", extra: String = "") =
        pom(
            artifactId,
            """
            <build>
                <plugins>
                    ${pluginDeclaration.replace("<executions>", "$pluginConfiguration<executions>")}
                </plugins>
            </build>
            """.trimIndent(),
            extra
        )

    protected fun settings() = SettingsStorage.getInstance(project).state

    /** Paths, relative to [base] (the project root by default) and with '/', of the source roots of a module. */
    protected fun sourceRoots(moduleName: String, isTest: Boolean, base: String = ""): Set<String> = runReadActionBlocking {
        ModuleRootManager.getInstance(getModule(moduleName)).contentEntries
            .flatMap { it.sourceFolders.toList() }
            .filter { it.isTestSource == isTest }
            .map { relative(it.url, base) }
            .toSet()
    }

    protected fun excludedFolders(moduleName: String, base: String = ""): Set<String> = runReadActionBlocking {
        ModuleRootManager.getInstance(getModule(moduleName)).contentEntries
            .flatMap { it.excludeFolderUrls }
            .map { relative(it, base) }
            .toSet()
    }

    private fun relative(url: String, base: String): String {
        val root = Path.of(projectRoot.path).resolve(base)

        return root.relativize(Path.of(VfsUtilCore.urlToPath(url))).toString().replace('\\', '/')
    }

    // A Maven server that cannot read the pom would leave the tests passing on a half-imported project
    override suspend fun importProjectsAsync(files: List<VirtualFile>) {
        super.importProjectsAsync(files)

        projectsManager.projects.forEach {
            assertFalse("Maven could not read ${it.path}: ${it.problems}", it.hasReadingErrors())
        }
    }

    protected fun mavenProjectOf(moduleDir: String = ""): MavenProject = runReadActionBlocking {
        val pom = if (moduleDir.isEmpty()) projectPom else projectRoot.findFileByRelativePath("$moduleDir/pom.xml")!!

        projectsManager.findProject(pom)!!
    }

    protected fun psiOf(file: VirtualFile): PsiFile = runReadActionBlocking { PsiManager.getInstance(project).findFile(file)!! }

    /** The reference of the first `${name}` found in the file, whatever follows the name. */
    protected fun referenceTo(file: PsiFile, name: String): MavenPropertyReference = runReadActionBlocking {
        val offset  = file.text.indexOf("{$name") + 1
        val literal = file.findElementAt(offset)!!.parent

        literal.references.filterIsInstance<MavenPropertyReference>().first { it.propertyName == name }
    }

    protected fun runInspection(file: PsiFile): List<ProblemDescriptor> = runReadActionBlocking {
        val holder  = ProblemsHolder(InspectionManager.getInstance(project), file, true)
        val visitor = UnresolvedMavenPropertyInspection().buildVisitor(holder, true)

        file.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                element.accept(visitor)
                super.visitElement(element)
            }
        })

        holder.results
    }

    protected fun declaredPomProperties(): Set<String> = runReadActionBlocking {
        TemplateProperties.declared(project, projectsManager.findProject(projectPom)!!).keys
    }

    /** Runs the quick fix of the problem the way the IDE does: through the intention action wrapper, in an editor. */
    protected fun invokeQuickFix(file: PsiFile, problem: ProblemDescriptor) {
        ApplicationManager.getApplication().invokeAndWait {
            val editor = FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptor(project, file.virtualFile), true)!!
            val action = QuickFixWrapper.wrap(problem, 0)

            assertTrue("Quick fix '${action.text}' was not applied", ShowIntentionActionsHandler.chooseActionAndInvoke(file, editor, action, action.text))
        }
    }

    /** Waits, without a fixed pause, for work queued on the event queue by the plugin. */
    protected fun flushEventQueue() {
        ApplicationManager.getApplication().invokeAndWait { }
    }

    protected suspend fun awaitCondition(message: String, condition: () -> Boolean) {
        try {
            withTimeout(30_000) {
                while (!condition()) {
                    delay(50)
                }
            }
        } catch (e: TimeoutCancellationException) {
            fail("Timed out waiting for: $message")
        }
    }

    protected suspend fun reimport() {
        updateAllProjectsFullSync()
        awaitConfiguration()
    }
}
