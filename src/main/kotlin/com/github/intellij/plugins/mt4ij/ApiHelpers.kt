package com.github.intellij.plugins.mt4ij

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.TimeUnit

class ApiHelpers {
    companion object {
        private val GET_ACTIVE_PROJECT_TIMEOUT = 60
        private val log : Logger               = Logger.getInstance(ApiHelpers::class.java)

        fun getContentEntry(model: ModifiableRootModel): ContentEntry? {
            val contentEntries = model.contentEntries
            return (if (contentEntries.isNotEmpty()) contentEntries[0] else null)
        }

        fun getContentEntry(model: ModifiableRootModel, virtualFile: VirtualFile): ContentEntry? {
            val contentEntries = model.contentEntries
            return (if (contentEntries.isEmpty()) model.addContentEntry(virtualFile) else contentEntries[0])
        }

        fun getModelForFile(project: Project, virtualFile: VirtualFile): ModifiableRootModel? {
            val fileIndex = ProjectFileIndex.SERVICE.getInstance(project)
            val module    = fileIndex.getModuleForFile(virtualFile)

            if (null != module) {
                if (module.moduleTypeName.equals("JAVA_MODULE")) {
                    return ModuleRootManager
                        .getInstance(module)
                        .modifiableModel
                }
            }

            return null
        }

        fun getActiveProject() : Project {
            val dataContext = DataManager.getInstance().dataContextFromFocusAsync.blockingGet(GET_ACTIVE_PROJECT_TIMEOUT, TimeUnit.SECONDS)
            val project     = dataContext?.getData(CommonDataKeys.PROJECT.name) as Project

            if (null == project) {
                log.warn("Active project not found.")
            }

            return project
        }

        fun invokeLater(runnable: Runnable, project: Project) {
            val application = ApplicationManager.getApplication()

            application.invokeLater(
                { application.runWriteAction(runnable) },
                project.disposed
            )
        }

        fun invokeCommit(model: ModifiableRootModel, project: Project) {
            invokeLater(
                {
                    model.commit()
                },
                project
            )
        }

    }
}
