package org.sfcivictech.android.shared.resourcebinder.ui.careplan

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.sfcivictech.android.shared.resourcebinder.MainActivity
import org.sfcivictech.android.shared.resourcebinder.R

//walks Home -> start screen -> manual flow through the real MainActivity, signed in
//and past onboarding so it opens on Home
class CarePlanNavigationTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    private fun string(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    @Before
    fun launchSignedIn() {
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("has_seen_onboarding", true)
            .putBoolean("has_signed_in", true)
            .commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun closeActivity() {
        scenario?.close()
    }

    private fun openManualFlow() {
        composeTestRule.onNodeWithText(string(R.string.home_action_start)).performClick()
        composeTestRule.onNodeWithText(string(R.string.start_plan_action_manual)).performClick()
    }

    @Test
    fun startOpensScreenFourThenManualFlow() {
        composeTestRule.onNodeWithText(string(R.string.home_action_start)).performClick()
        composeTestRule.onNodeWithText(string(R.string.start_plan_title)).assertExists()

        composeTestRule.onNodeWithText(string(R.string.start_plan_action_manual)).performClick()

        composeTestRule.onNodeWithText(string(R.string.manual_title)).assertExists()
    }

    @Test
    fun backFromManualReturnsToScreenFour() {
        openManualFlow()

        composeTestRule.onNodeWithTag(MANUAL_BACK_TEST_TAG).performClick()

        composeTestRule.onNodeWithText(string(R.string.start_plan_title)).assertExists()
    }

    @Test
    fun closeFromManualReturnsHome() {
        openManualFlow()

        composeTestRule.onNodeWithTag(MANUAL_CLOSE_TEST_TAG).performClick()

        composeTestRule.onNodeWithText(string(R.string.home_title)).assertExists()
    }

    @Test
    fun closeFromScreenFourReturnsHome() {
        composeTestRule.onNodeWithText(string(R.string.home_action_start)).performClick()

        composeTestRule.onNodeWithContentDescription(string(R.string.start_plan_action_close)).performClick()

        composeTestRule.onNodeWithText(string(R.string.home_title)).assertExists()
    }
}
