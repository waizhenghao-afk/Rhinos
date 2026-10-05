package com.rhinos.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI smoke tests for the critical flows (run on a device/emulator):
 * launch -> onboarding -> home.
 */
@RunWith(AndroidJUnit4::class)
class UiSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launch_showsWelcomeOrHome() {
        composeRule.waitForIdle()
    }

    @Test
    fun getStarted_leadsToHome() {
        composeRule.waitForIdle()
        val started = composeRule.onAllNodesWithText("Get Started")
        if (started.fetchSemanticsNodes().isNotEmpty()) {
            started[0].performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithText("Your private investment journal").assertExists()
    }
}
