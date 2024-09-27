package com.github.jbij.plugins.mt4ij.listeners

import com.github.jbij.plugins.mt4ij.ApiHelpers
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectChanges
import org.jetbrains.idea.maven.project.MavenProjectsTree

class MavenProjectsManagerListener(private val project: Project) : MavenProjectsTree.Listener {
    private val log : Logger = Logger.getInstance(MavenProjectsManagerListener::class.java)
    private val projectRef   = project

    override fun projectsUpdated(updated: List<Pair<MavenProject, MavenProjectChanges>>, deleted: List<MavenProject>) {
        log.info("Processing templates folders on maven project import...")
        ApiHelpers.scanProject(projectRef)
    }
}
