package com.github.intellij.plugins.mt4ij.navigation

import com.github.intellij.plugins.mt4ij.Bundle
import com.github.intellij.plugins.mt4ij.MavenTemplatingTestCase
import com.github.intellij.plugins.mt4ij.TemplateLocator
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.ProblemHighlightFilter
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.testFramework.PlatformTestUtil
import com.github.intellij.plugins.mt4ij.MavenTemplatingIcons
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.HyperlinkLabel
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.runBlocking
import org.junit.Test

class NavigationTest : MavenTemplatingTestCase() {

    private val source = "package demo; class Greeter {}"

    private lateinit var template: VirtualFile
    private lateinit var generated: VirtualFile

    // The generated sources are not excluded unless the test says so: their folder is then a generated source root
    private fun importTemplatesProject(withGenerated: Boolean = true, excludeGenerated: Boolean = false) {
        settings().excludeGeneratedSources = excludeGenerated

        template = createProjectSubFile("src/main/java-templates/demo/Greeter.java", source)
        generated = createProjectSubFile(
            if (withGenerated) "target/generated-sources/java-templates/demo/Greeter.java" else "target/other/Greeter.java",
            source
        )

        runBlocking {
            createProjectSubDirs("src/main/java")
            importProjectAsync(pomWithPlugin("project"))
        }
    }

    private fun updateAction(file: VirtualFile): AnActionEvent {
        val action  = GoToCounterpartAction()
        val context = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.VIRTUAL_FILE, file)
            .build()
        val event   = TestActionEvent.createTestEvent(action, context)

        runReadActionBlocking { action.update(event) }

        return event
    }

    private fun bannerOf(file: VirtualFile) =
        runReadActionBlocking { CounterpartBannerProvider().collectNotificationData(project, file) }

    // Builds the banner the way the IDE does, by invoking the function the provider returns for an editor
    private fun panelOf(file: VirtualFile): EditorNotificationPanel = runInEdtAndGet {
        val editor = FileEditorManager.getInstance(project).openFile(file, true).first()

        bannerOf(file)!!.apply(editor) as EditorNotificationPanel
    }

    private fun linksOf(panel: EditorNotificationPanel) = UIUtil.findComponentsOfType(panel, HyperlinkLabel::class.java)

    // Runs the daemon on the file opened in an editor and returns the tooltips of the gutter icons it has drawn
    private fun gutterTooltipsOf(file: VirtualFile): List<String?> = runInEdtAndGet {
        FileEditorManager.getInstance(project).openFile(file, true)
        val editor = FileEditorManager.getInstance(project).selectedTextEditor!!

        CodeInsightTestFixtureImpl.instantiateAndRun(psiOf(file), editor, intArrayOf(), false)

        DaemonCodeAnalyzerImpl.getLineMarkers(editor.document, project).mapNotNull { it.createGutterRenderer()?.tooltipText }
    }

    private fun markersOf(file: PsiFile): List<LineMarkerInfo<*>> = runReadActionBlocking {
        val result = mutableListOf<LineMarkerInfo<*>>()
        val psiClass = (file as PsiJavaFile).classes.single()

        TemplateLineMarkerProvider().collectSlowLineMarkers(listOf(psiClass.nameIdentifier!!), result)

        result
    }

    @Test
    fun testActionIsOfferedOnATemplateWhenTheGeneratedSourceExists() {
        importTemplatesProject()

        val presentation = updateAction(template).presentation

        assertTrue(presentation.isEnabledAndVisible)
        assertTrue(presentation.text, presentation.text.isNotBlank())
    }

    @Test
    fun testActionIsOfferedOnTheGeneratedSourceToo() {
        importTemplatesProject()

        assertTrue(updateAction(generated).presentation.isEnabledAndVisible)
    }

    @Test
    fun testActionTextTellsTheDirection() {
        importTemplatesProject()

        assertFalse(updateAction(template).presentation.text == updateAction(generated).presentation.text)
    }

    @Test
    fun testActionIconDiffersByDirection() {
        importTemplatesProject()

        assertFalse(updateAction(template).presentation.icon == updateAction(generated).presentation.icon)
    }

    @Test
    fun testActionIsShownButDisabledOnATemplateWithoutGeneratedSource() {
        importTemplatesProject(withGenerated = false)

        val presentation = updateAction(template).presentation

        assertTrue(presentation.isVisible)
        assertFalse(presentation.isEnabled)
        assertEquals(Bundle.message("mt4ij.navigation.go-to-generated"), presentation.text)
        assertEquals(Bundle.message("mt4ij.navigation.go-to-generated.not-built"), presentation.description)
    }

    @Test
    fun testActionDescriptionIsTheOrdinaryOneOnceTheGeneratedSourceExists() {
        importTemplatesProject()

        assertEquals(Bundle.message("action.mt4ij.actions.go-to-counterpart.description"), updateAction(template).presentation.description)
    }

    @Test
    fun testActionIsHiddenOnOrdinarySources() {
        importTemplatesProject()
        val plain = createProjectSubFile("src/main/java/demo/Plain.java", "package demo; class Plain {}")

        assertFalse(updateAction(plain).presentation.isEnabledAndVisible)
    }

    @Test
    fun testLineMarkerIsOfferedOnTheTemplateClass() {
        importTemplatesProject()

        assertEquals(1, markersOf(psiOf(template)).size)
    }

    @Test
    fun testNoLineMarkerWithoutCounterpart() {
        importTemplatesProject(withGenerated = false)

        assertTrue(markersOf(psiOf(template)).isEmpty())
    }

    @Test
    fun testLineMarkerIsOfferedOnTheGeneratedClass() {
        importTemplatesProject()

        assertEquals(1, markersOf(psiOf(generated)).size)
    }

    @Test
    fun testMarkerIconsDifferByDirection() {
        importTemplatesProject()

        assertFalse(markersOf(psiOf(template)).single().icon == markersOf(psiOf(generated)).single().icon)
    }

    @Test
    fun testGeneratedSourceIsFoundWhenTheOutputFolderIsExplicitlyExcluded() {
        importTemplatesProject()
        PsiTestUtil.addExcludedRoot(getModule("project"), generated.parent.parent)

        // Excluded files have no module: the project is then found through its directory
        assertNull(runReadActionBlocking { ProjectFileIndex.getInstance(project).getModuleForFile(generated) })

        val counterpart = runReadActionBlocking { TemplateLocator.counterpartOf(project, generated) }

        assertNotNull(counterpart)
        assertEquals(template, counterpart!!.file)
        assertTrue(updateAction(generated).presentation.isEnabledAndVisible)
    }

    @Test
    fun testLineMarkerIsOfferedOnTheGeneratedClassInAGeneratedSourceRoot() {
        importTemplatesProject()

        // The default output folder is a generated source root, so the IDE analyzes its files
        val root = generated.parent.parent
        assertTrue(sourceRoots("project", false).any { root.path.endsWith(it) })

        assertEquals(1, markersOf(psiOf(generated)).size)
    }

    @Test
    fun testBannerIsOfferedOnBothFiles() {
        importTemplatesProject()

        assertNotNull(bannerOf(template))
        assertNotNull(bannerOf(generated))
    }

    @Test
    fun testBannerTellsTheDirectionAndLinksToTheOtherFile() {
        importTemplatesProject()

        val onTemplate  = panelOf(template)
        val onGenerated = panelOf(generated)

        // The text tells what the file it is shown on is: a template turned into a generated source, and not the other way round
        assertEquals(Bundle.message("mt4ij.navigation.banner.template"), onTemplate.text)
        assertEquals(Bundle.message("mt4ij.navigation.banner.generated"), onGenerated.text)
        assertEquals(1, linksOf(onTemplate).size)
        assertEquals(1, linksOf(onGenerated).size)
    }

    // The default output folder is a generated source root, so the daemon runs on its files and draws the marker
    @Test
    fun testGutterIconIsDrawnOnTheGeneratedFileInAGeneratedSourceRoot() {
        importTemplatesProject()

        val tooltips = gutterTooltipsOf(generated)

        assertEquals(tooltips.toString(), 1, tooltips.count { it?.startsWith(Bundle.message("mt4ij.navigation.go-to-template")) == true })
    }

    // The IDE does not highlight the excluded files, so no gutter icon is drawn there: the banner is the way back to the template
    @Test
    fun testNoGutterIconOnTheGeneratedFileInAnExcludedFolderButTheBannerIsThere() {
        importTemplatesProject()
        PsiTestUtil.addExcludedRoot(getModule("project"), generated.parent.parent)

        assertFalse(runReadActionBlocking { ProblemHighlightFilter.shouldHighlightFile(psiOf(generated)) })
        assertNotNull(bannerOf(generated))
    }

    // The folders the plugin writes into are excluded unless the option is turned off, and the navigation still works there
    @Test
    fun testGeneratedFolderIsExcludedByDefaultAndStillHasTheBanner() {
        importTemplatesProject(excludeGenerated = true)

        // Maven would make the output folder a generated source root
        assertFalse(sourceRoots("project", false).toString(), sourceRoots("project", false).contains("target/generated-sources/java-templates"))
        assertNotNull(bannerOf(generated))
        assertTrue(updateAction(generated).presentation.isEnabledAndVisible)
    }

    @Test
    fun testBannerOnATemplateWithoutGeneratedSourceTellsToBuildTheProject() {
        importTemplatesProject(withGenerated = false)

        val panel = panelOf(template)

        assertEquals(Bundle.message("mt4ij.navigation.banner.not-built"), panel.text)
        assertTrue(linksOf(panel).isEmpty())
    }

    // The innermost Maven project wins, even when the module is a child whose target is excluded
    @Test
    fun testInnermostMavenProjectWinsForAChildModule() {
        runBlocking {
            createProjectSubDirs("m1/src/main/java")
            createProjectPom(
                """
                <groupId>test</groupId>
                <artifactId>project</artifactId>
                <version>1.0</version>
                <packaging>pom</packaging>
                <modules>
                    <module>m1</module>
                </modules>
                """.trimIndent()
            )
            createModulePom("m1", pomWithPlugin("m1"))
        }
        val childTemplate  = createProjectSubFile("m1/src/main/java-templates/demo/Greeter.java", source)
        val childGenerated = createProjectSubFile("m1/target/generated-sources/java-templates/demo/Greeter.java", source)

        runBlocking { importProjectAsync() }
        PsiTestUtil.addExcludedRoot(getModule("m1"), childGenerated.parent.parent)

        assertEquals(mavenProjectOf("m1"), runReadActionBlocking { TemplateLocator.mavenProjectOf(project, childGenerated) })
        assertEquals(childTemplate, runReadActionBlocking { TemplateLocator.counterpartOf(project, childGenerated) }?.file)
    }

    @Test
    fun testNoBannerOnOrdinarySources() {
        importTemplatesProject()
        val plain = createProjectSubFile("src/main/java/demo/Plain.java", "package demo; class Plain {}")

        assertNull(bannerOf(plain))
    }

    private fun openedEditorOf(file: VirtualFile) = runInEdtAndGet {
        FileEditorManager.getInstance(project).openFile(file, true)
        FileEditorManager.getInstance(project).selectedTextEditor!!
    }

    // The icons the listener has put in the markup model of the editor, waiting up to the timeout for the background computation
    private fun iconsOf(editor: Editor, expected: Int, timeoutMs: Long = 10_000): List<GutterIconRenderer> {
        val deadline = System.currentTimeMillis() + timeoutMs

        fun icons() = runInEdtAndGet { editor.markupModel.allHighlighters.mapNotNull { it.gutterIconRenderer } }

        while (icons().size < expected && System.currentTimeMillis() < deadline) {
            runInEdtAndGet { PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue() }
            Thread.sleep(50)
        }
        Thread.sleep(300)
        runInEdtAndGet { PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue() }

        return icons()
    }

    private fun listenerIconsOf(file: VirtualFile, expected: Int, timeoutMs: Long = 10_000) =
        iconsOf(openedEditorOf(file), expected, timeoutMs)

    @Test
    fun testGeneratedFileInAnExcludedFolderGetsTheGoToTemplateIcon() {
        importTemplatesProject(excludeGenerated = true)

        val icons = listenerIconsOf(generated, 1)

        assertEquals(1, icons.size)
        assertSame(MavenTemplatingIcons.GoToTemplateGutter, icons.single().icon)
        assertTrue(icons.single().tooltipText!!.startsWith(Bundle.message("mt4ij.navigation.go-to-template")))
    }

    @Test
    fun testListenerAddsNoIconOnTheTemplateNorOnAPlainFile() {
        importTemplatesProject(excludeGenerated = true)
        val plain = createProjectSubFile("src/main/java/demo/Plain.java", "package demo; class Plain {}")

        assertTrue(listenerIconsOf(template, 1, 2_000).isEmpty())
        assertTrue(listenerIconsOf(plain, 1, 2_000).isEmpty())
    }

    // An editor restored before the projects are known has no counterpart yet: the refresh draws the icon, once
    @Test
    fun testRefreshDrawsTheIconOnceTheTemplateIsKnownAndNeverTwice() {
        importTemplatesProject(excludeGenerated = true)
        com.intellij.openapi.application.WriteAction.runAndWait<java.io.IOException> { template.delete(this@NavigationTest) }

        val editor = openedEditorOf(generated)
        assertTrue(iconsOf(editor, 1, 2_000).isEmpty())

        createProjectSubFile("src/main/java-templates/demo/Greeter.java", source)

        GeneratedSourceGutterIcon.refresh(project)
        assertEquals(1, iconsOf(editor, 1).size)

        GeneratedSourceGutterIcon.refresh(project)
        assertEquals(1, iconsOf(editor, 1).size)
    }
}
