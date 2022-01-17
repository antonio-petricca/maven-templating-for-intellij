package com.github.intellij.plugins.mt4ij.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/*
    https://programtalk.com/vs/intellij-haxe/src/common/com/intellij/plugins/haxe/config/HaxeProjectSettings.java
    https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html#implementing-the-persistentstatecomponent-interface
    https://plugins.jetbrains.com/docs/intellij/plugin-components.html?from=jetbrains.org#manage-state
    https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html
 */

@State(
    name     = "mf4ij.storage.state",
    storages = [Storage("mt4ij.xml")]
)
class SettingsStorage : PersistentStateComponent<SettingsState?> {
    private var state = SettingsState()

    companion object {
        val instance: SettingsStorage = ApplicationManager.getApplication().getService(SettingsStorage::class.java)
    }

    override fun getState(): SettingsState {
        return state
    }

    override fun loadState(state: SettingsState) {
        this.state = state
    }
}
