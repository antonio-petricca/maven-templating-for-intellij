package com.github.jbij.plugins.mt4ij.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

/*
    https://programtalk.com/vs/intellij-haxe/src/common/com/intellij/plugins/haxe/config/HaxeProjectSettings.java
    https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html#implementing-the-persistentstatecomponent-interface
    https://plugins.jetbrains.com/docs/intellij/plugin-components.html?from=jetbrains.org#manage-state
    https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html

    https://intellij-support.jetbrains.com/hc/en-us/community/posts/206875729-Persistent-state-storage-throwing-IllegalArgumentException-while-using-StoragePathMacros-WORKSPACE-FILE
    https://jetbrains-platform.slack.com/archives/C5U8BM1MK/p1642429147167000
 */

@State(
    name     = "mt4ij.storage.state",
    storages = [Storage("mt4ij.xml")]
)
@Service(Service.Level.PROJECT)
class SettingsStorage : PersistentStateComponent<SettingsState?> {
    private var state = SettingsState()

    companion object {

        @JvmStatic
        fun getInstance(project: Project) : SettingsStorage {
            return project.getService(SettingsStorage::class.java)
        }

    }

    override fun getState(): SettingsState {
        return state
    }

    override fun loadState(state: SettingsState) {
        this.state = state
    }
}
