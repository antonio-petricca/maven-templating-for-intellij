package com.github.intellij.plugins.mt4ij.actions

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/** Apart from the Maven tests, whose project is always imported from a pom. */
class MavenTemplatingGroupHiddenTest : BasePlatformTestCase() {

    fun testGroupIsHiddenInAProjectThatIsNotMaven() {
        val group   = MavenTemplatingGroup()
        val context = SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build()
        val event   = TestActionEvent.createTestEvent(group, context)

        group.update(event)

        assertFalse(event.presentation.isVisible)
    }
}
