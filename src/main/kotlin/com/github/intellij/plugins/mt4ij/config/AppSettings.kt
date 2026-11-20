package com.github.intellij.plugins.mt4ij.config

import com.github.intellij.plugins.mt4ij.guide.GuideLogic
import com.github.intellij.plugins.mt4ij.guide.WhatsNewVisit
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/** Settings shared by all the projects: the "What's New" tab option and the last version it was claimed for. */
@State(
    name     = "mt4ij.storage.app-state",
    storages = [Storage("mt4ij-app.xml")]
)
@Service(Service.Level.APP)
class AppSettings : PersistentStateComponent<AppSettingsState> {

    private var state = AppSettingsState()

    companion object {

        // Kept in the application properties, which are never synchronized between machines
        private const val LAST_VERSION_KEY = "mt4ij.whats-new.last-version"

        @JvmStatic
        fun getInstance(): AppSettings = ApplicationManager.getApplication().getService(AppSettings::class.java)
    }

    override fun getState(): AppSettingsState = state

    override fun loadState(state: AppSettingsState) {
        this.state = state
    }

    /**
     * Records the version as seen and tells whether its "What's New" tab has to be shown now, and for what reason.
     * The version is recorded even when the option is off, so that turning it on does not show an old update.
     * Synchronized, so that only one of the projects opened together gets an answer.
     */
    @Synchronized
    fun claimWhatsNew(installedVersion: String?): WhatsNewVisit? {
        if (installedVersion.isNullOrBlank()) {
            return null
        }

        val properties = PropertiesComponent.getInstance()
        val lastVersion = properties.getValue(LAST_VERSION_KEY, "")

        properties.setValue(LAST_VERSION_KEY, installedVersion)

        return GuideLogic.whatsNewVisit(installedVersion, lastVersion, state.showWhatsNew)
    }

    /** Forgets the recorded version, as on a fresh installation. */
    @Synchronized
    internal fun resetClaimedVersion() {
        PropertiesComponent.getInstance().unsetValue(LAST_VERSION_KEY)
    }
}
