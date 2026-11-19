package com.github.intellij.plugins.mt4ij

import com.github.intellij.plugins.mt4ij.importing.TemplatingFolders
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

/** Which kind of source root a templates folder is. */
enum class TemplatesFolderKind(val isTest: Boolean) {
    SOURCE(false),
    TEST(true);

    companion object {

        /** Matches whole path segments: `<...>/src/main/<name>` or `<...>/src/test/<name>`. */
        fun of(path: String, templatesName: String): TemplatesFolderKind? {
            if (!TemplatingFolders.isValidName(templatesName)) {
                return null
            }

            val normalizedPath = path.trimEnd('/')

            return when {
                normalizedPath.endsWith("/src/main/$templatesName") -> SOURCE
                normalizedPath.endsWith("/src/test/$templatesName") -> TEST
                else -> null
            }
        }
    }
}

/** A request to add a templates folder to, or remove it from, the source roots of a module. */
class SourceFolderChange private constructor(
    val module: Module,
    val folder: VirtualFile,
    val folderUrl: String,
    val isTest: Boolean,
    val isAddition: Boolean,
) {
    companion object {
        fun add(module: Module, folder: VirtualFile, isTest: Boolean) =
            SourceFolderChange(module, folder, folder.url, isTest, true)

        // The folder may be gone when the change is applied, so removal matches on the URL
        fun remove(module: Module, folder: VirtualFile, isTest: Boolean) =
            SourceFolderChange(module, folder, folder.url, isTest, false)
    }
}

/** How many source roots [SourceFolders.apply] actually added and removed. */
data class AppliedChanges(val added: Int, val removed: Int)

/**
 * The only place where source roots are modified: all the modules are updated in a single
 * write action, so the project gets one roots-changed event instead of one per module.
 */
object SourceFolders {
    private val log: Logger = Logger.getInstance(SourceFolders::class.java)

    private const val JAVA_MODULE_TYPE_ID = "JAVA_MODULE"

    /** Must not be called from a roots-changed event nor from inside a read action. */
    fun apply(changes: List<SourceFolderChange>): AppliedChanges {
        val pendingByModule = runReadActionBlocking {
            changes
                .groupBy { it.module }
                .mapValues { (module, moduleChanges) ->
                    if (module.isDisposed || ModuleType.get(module).id != JAVA_MODULE_TYPE_ID) {
                        emptyList()
                    } else {
                        val contentEntries = ModuleRootManager.getInstance(module).contentEntries
                        moduleChanges.filter { isChangeNeeded(contentEntries, it) }
                    }
                }
                .filterValues { it.isNotEmpty() }
        }

        pendingByModule.entries
            .groupBy({ it.key.project }, { it.key to it.value })
            .forEach { (project, entries) -> commit(project, entries.toMap()) }

        val pending = pendingByModule.values.flatten()

        return AppliedChanges(pending.count { it.isAddition }, pending.count { !it.isAddition })
    }

    // One write action and one merged roots-changed event for all the modules of the project
    private fun commit(project: Project, pendingByModule: Map<Module, List<SourceFolderChange>>) {
        WriteAction.run<RuntimeException> {
            ProjectRootManagerEx.getInstanceEx(project).mergeRootsChangesDuring {
                pendingByModule.forEach { (module, moduleChanges) ->
                    ModuleRootModificationUtil.updateModel(module) { model ->
                        moduleChanges.forEach { apply(model, it) }
                    }
                }
            }
        }
    }

    private fun isChangeNeeded(contentEntries: Array<ContentEntry>, change: SourceFolderChange): Boolean {
        val isMarked = contentEntries.any { entry -> entry.sourceFolders.any { it.url == change.folderUrl } }

        return if (change.isAddition) {
            !isMarked && findContentEntry(contentEntries, change.folder) != null
        } else {
            isMarked
        }
    }

    private fun apply(model: ModifiableRootModel, change: SourceFolderChange) {
        if (change.isAddition) {
            val contentEntry = findContentEntry(model.contentEntries, change.folder) ?: return

            if (contentEntry.sourceFolders.none { it.url == change.folderUrl }) {
                contentEntry.addSourceFolder(change.folder, change.isTest)
                log.info("Added source folder: { url: \"${change.folderUrl}\", isTestFolder: ${change.isTest} }")
            }
        } else {
            model.contentEntries.forEach { contentEntry ->
                contentEntry.sourceFolders
                    .filter { it.url == change.folderUrl }
                    .forEach {
                        contentEntry.removeSourceFolder(it)
                        log.info("Removed source folder: { url: \"${change.folderUrl}\" }")
                    }
            }
        }
    }

    private fun findContentEntry(contentEntries: Array<ContentEntry>, folder: VirtualFile): ContentEntry? {
        return contentEntries.firstOrNull { entry ->
            val entryFile = entry.file
            (entryFile != null) && VfsUtilCore.isAncestor(entryFile, folder, false)
        }
    }
}
