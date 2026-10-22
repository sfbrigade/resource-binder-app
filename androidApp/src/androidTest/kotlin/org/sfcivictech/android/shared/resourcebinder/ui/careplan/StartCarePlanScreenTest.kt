package org.sfcivictech.android.shared.resourcebinder.ui.careplan

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

class StartCarePlanScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    private fun setScreen(
        onClose: () -> Unit = {},
        onEnterManually: () -> Unit = {},
        onStartVoice: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ResourceBinderTheme {
                StartCarePlanScreen(
                    onClose = onClose,
                    onEnterManually = onEnterManually,
                    onStartVoice = onStartVoice,
                )
            }
        }
    }

    @Test
    fun showsTitleAndBothActions() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.start_plan_title)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.start_plan_subtitle)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.start_plan_step_understand)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.start_plan_step_find)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.start_plan_action_manual)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.start_plan_action_voice)).assertExists()
    }

    @Test
    fun enterManuallyInvokesCallback() {
        var count = 0
        setScreen(onEnterManually = { count++ })

        composeTestRule.onNodeWithText(string(R.string.start_plan_action_manual)).performClick()

        assertEquals(1, count)
    }

    @Test
    fun voiceInvokesCallback() {
        var count = 0
        setScreen(onStartVoice = { count++ })

        composeTestRule.onNodeWithText(string(R.string.start_plan_action_voice)).performClick()

        assertEquals(1, count)
    }

    @Test
    fun closeButtonInvokesOnClose() {
        var count = 0
        setScreen(onClose = { count++ })

        composeTestRule.onNodeWithContentDescription(string(R.string.start_plan_action_close)).performClick()

        assertEquals(1, count)
    }

    @Test
    fun systemBackInvokesOnClose() {
        var count = 0
        setScreen(onClose = { count++ })

        Espresso.pressBack()

        assertEquals(1, count)
    }
}
