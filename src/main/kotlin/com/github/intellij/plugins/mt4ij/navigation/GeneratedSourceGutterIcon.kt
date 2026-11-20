package com.github.intellij.plugins.mt4ij.navigation

import com.github.intellij.plugins.mt4ij.Counterpart
import com.github.intellij.plugins.mt4ij.TemplateLocator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.AppExecutorUtil
import javax.swing.Icon

/**
 * Draws the "Go to Template" icon in the editor of a generated source.
 * The daemon runs no line marker providers in excluded folders, such as the default output folder,
 * so [TemplateLineMarkerProvider] never gets there: the icon is added here, when the editor is created.
 */
class GeneratedSourceGutterIcon : EditorFactoryListener {

    override fun editorCreated(event: EditorFactoryEvent) = update(event.editor)

    companion object {

        private class Target(val template: Counterpart, val line: Int)

        /** Draws the icon again in every open editor, for when the template becomes known after the editor was created. */
        fun refresh(project: Project) {
            EditorFactory.getInstance().allEditors.filter { it.project == project }.forEach { update(it) }
        }

        // The single path for new editors and refreshes: the icon is first removed, so it is never drawn twice
        private fun update(editor: Editor) {
            val project = editor.project ?: return
            val file    = FileDocumentManager.getInstance().getFile(editor.document) ?: return

            // Diff, console and preview editors are of other kinds; UNTYPED is what the editors of the test IDE report
            if (editor.editorKind !in setOf(EditorKind.MAIN_EDITOR, EditorKind.UNTYPED) || !file.isValid) {
                return
            }

            ReadAction.nonBlocking<Target?> { targetOf(project, file, editor.document) }
                .expireWith(project)
                .coalesceBy(GeneratedSourceGutterIcon::class.java, editor)
                .finishOnUiThread(ModalityState.any()) { target ->
                    if (!editor.isDisposed) {
                        hide(editor)
                        if (target != null) {
                            show(editor, project, target)
                        }
                    }
                }
                .submit(AppExecutorUtil.getAppExecutorService())
        }

        // Files of the source roots are left to the line marker provider of the daemon
        private fun targetOf(project: Project, file: VirtualFile, document: Document): Target? {
            if (ProjectFileIndex.getInstance(project).isInSourceContent(file)) {
                return null
            }

            val template = TemplateLocator.counterpartOf(project, file)?.takeIf { !it.isGenerated } ?: return null
            val offset   = (PsiManager.getInstance(project).findFile(file) as? PsiJavaFile)
                ?.classes?.firstOrNull()?.nameIdentifier?.textOffset ?: 0

            return Target(template, document.getLineNumber(offset.coerceIn(0, document.textLength)))
        }

        private fun hide(editor: Editor) {
            editor.markupModel.allHighlighters
                .filter { it.gutterIconRenderer is GoToTemplateRenderer }
                .forEach { editor.markupModel.removeHighlighter(it) }
        }

        private fun show(editor: Editor, project: Project, target: Target) {
            editor.markupModel.addLineHighlighter(null, target.line, HighlighterLayer.ADDITIONAL_SYNTAX)
                .gutterIconRenderer = GoToTemplateRenderer(project, target.template)
        }
    }

    private class GoToTemplateRenderer(private val project: Project, private val template: Counterpart) : GutterIconRenderer() {

        override fun getIcon(): Icon = template.gutterIcon

        override fun getTooltipText(): String = template.tooltipText

        override fun isNavigateAction(): Boolean = true

        override fun getAlignment(): Alignment = Alignment.LEFT

        override fun getClickAction(): AnAction = object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                OpenFileDescriptor(project, template.file).navigate(true)
            }
        }

        override fun equals(other: Any?): Boolean = other is GoToTemplateRenderer && other.template.file.url == template.file.url

        override fun hashCode(): Int = template.file.url.hashCode()
    }
}
