package com.github.intellij.plugins.mt4ij.config

import com.github.intellij.plugins.mt4ij.guide.WhatsNewVisit
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AppSettingsTest : BasePlatformTestCase() {

    private val settings get() = AppSettings.getInstance()

    override fun setUp() {
        super.setUp()

        settings.resetClaimedVersion()
        settings.state.showWhatsNew = true
    }

    override fun tearDown() {
        try {
            settings.resetClaimedVersion()
            settings.state.showWhatsNew = true
        } finally {
            super.tearDown()
        }
    }

    fun testFirstClaimIsAnInstallAndTheSecondOneGetsNothing() {
        assertEquals(WhatsNewVisit.INSTALL, settings.claimWhatsNew("1.0.0"))
        assertNull(settings.claimWhatsNew("1.0.0"))
    }

    fun testNewVersionIsClaimedAgainAsAnUpdate() {
        settings.claimWhatsNew("1.0.0")

        assertEquals(WhatsNewVisit.UPDATE, settings.claimWhatsNew("1.1.0"))
    }

    fun testDisabledOptionStillRecordsTheVersion() {
        settings.state.showWhatsNew = false

        assertNull(settings.claimWhatsNew("1.0.0"))

        // Turning the option on must not show the update that has been skipped
        settings.state.showWhatsNew = true

        assertNull(settings.claimWhatsNew("1.0.0"))
        assertEquals(WhatsNewVisit.UPDATE, settings.claimWhatsNew("1.1.0"))
    }

    fun testUnknownVersionIsNeverClaimed() {
        assertNull(settings.claimWhatsNew(null))
        assertNull(settings.claimWhatsNew(""))
    }
}
