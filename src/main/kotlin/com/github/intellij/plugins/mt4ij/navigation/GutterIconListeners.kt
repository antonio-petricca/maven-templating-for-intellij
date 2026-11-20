package com.github.intellij.plugins.mt4ij.navigation

import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.project.MavenImportListener
import org.jetbrains.idea.maven.project.MavenProject

/** Draws the gutter icons of the open generated sources again once the Maven projects are known. */
class GutterIconImportListener(private val project: Project) : MavenImportListener {

    override fun importFinished(importedProjects: Collection<MavenProject>, newModules: List<Module>) {
        GeneratedSourceGutterIcon.refresh(project)
    }
}

/** Draws the gutter icon again when a tab is selected, in case the Maven projects became known after it was opened. */
class GutterIconSelectionListener(private val project: Project) : FileEditorManagerListener {

    override fun selectionChanged(event: FileEditorManagerEvent) {
        GeneratedSourceGutterIcon.refresh(project)
    }
}
