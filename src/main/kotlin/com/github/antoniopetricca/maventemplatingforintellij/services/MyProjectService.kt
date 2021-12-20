package com.github.antoniopetricca.maventemplatingforintellij.services

import com.intellij.openapi.project.Project
import com.github.antoniopetricca.maventemplatingforintellij.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
