package com.github.intellij.plugins.mt4ij.config

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

    fun testFirstClaimWinsAndTheSecondOneDoesNot() {
        assertTrue(settings.claimWhatsNew("1.0.0"))
        assertFalse(settings.claimWhatsNew("1.0.0"))
    }

    fun testNewVersionIsClaimedAgain() {
        settings.claimWhatsNew("1.0.0")

        assertTrue(settings.claimWhatsNew("1.1.0"))
    }

    fun testDisabledOptionStillRecordsTheVersion() {
        settings.state.showWhatsNew = false

        assertFalse(settings.claimWhatsNew("1.0.0"))

        // Turning the option on must not show the update that has been skipped
        settings.state.showWhatsNew = true

        assertFalse(settings.claimWhatsNew("1.0.0"))
        assertTrue(settings.claimWhatsNew("1.1.0"))
    }

    fun testUnknownVersionIsNeverClaimed() {
        assertFalse(settings.claimWhatsNew(null))
        assertFalse(settings.claimWhatsNew(""))
    }
}
