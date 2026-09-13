package org.sfcivictech.android.shared.resourcebinder.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoActivityResumedException
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.sfcivictech.android.shared.resourcebinder.R

class OnboardingFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    private fun hasClickLabel(label: String): SemanticsMatcher =
        SemanticsMatcher("has click label '$label'") { node ->
            node.config.getOrNull(SemanticsActions.OnClick)?.label == label
        }

    @Test
    fun nextAdvancesThroughAllFourPagesInOrder() {
        composeTestRule.setContent { OnboardingFlow() }

        composeTestRule.onNodeWithText(string(R.string.onboarding_welcome_title)).assertExists()

        composeTestRule.onNodeWithText(string(R.string.onboarding_action_next)).performClick()
        composeTestRule.onNodeWithText(string(R.string.onboarding_describe_title)).assertExists()

        composeTestRule.onNodeWithText(string(R.string.onboarding_action_next)).performClick()
        composeTestRule.onNodeWithText(string(R.string.onboarding_ai_title)).assertExists()

        composeTestRule.onNodeWithText(string(R.string.onboarding_action_next)).performClick()
        composeTestRule.onNodeWithText(string(R.string.onboarding_field_title)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.onboarding_action_get_started)).assertExists()
    }

    @Test
    fun backButtonReturnsToThePreviousPage() {
        composeTestRule.setContent { OnboardingFlow() }

        composeTestRule.onNodeWithText(string(R.string.onboarding_action_next)).performClick()
        composeTestRule.onNodeWithText(string(R.string.onboarding_describe_title)).assertExists()

        composeTestRule.onNode(hasClickLabel(string(R.string.onboarding_action_back))).performClick()
        composeTestRule.onNodeWithText(string(R.string.onboarding_welcome_title)).assertExists()
    }

    @Test
    fun systemBackNavigatesToThePreviousPageInsteadOfExiting() {
        composeTestRule.setContent { OnboardingFlow() }

        composeTestRule.onNodeWithText(string(R.string.onboarding_action_next)).performClick()
        composeTestRule.onNodeWithText(string(R.string.onboarding_describe_title)).assertExists()

        pressBack()

        composeTestRule.onNodeWithText(string(R.string.onboarding_welcome_title)).assertExists()
    }

    @Test
    fun systemBackOnTheFirstPageExitsWithoutMarkingOnboardingFinished() {
        var finished = false
        composeTestRule.setContent { OnboardingFlow(onFinished = { finished = true }) }

        try {
            // Nothing left to resume is proof back at page 0 actually exits, rather
            // than dismissing onboarding and landing on some other screen.
        }

        // Either way, completion is Skip/Get started's job, not the back button's.
        assertFalse(finished)
    }

    @Test
    fun skipOnTheFirstPageFinishesOnboarding() {
        var finished = false
        composeTestRule.setContent { OnboardingFlow(onFinished = { finished = true }) }

        composeTestRule.onNodeWithText(string(R.string.onboarding_action_skip)).performClick()

        assertTrue(finished)
    }

    @Test
    fun getStartedOnTheLastPageFinishesOnboarding() {
        var finished = false
        composeTestRule.setContent { OnboardingFlow(onFinished = { finished = true }) }

        repeat(3) { composeTestRule.onNodeWithText(string(R.string.onboarding_action_next)).performClick() }
        composeTestRule.onNodeWithText(string(R.string.onboarding_action_get_started)).performClick()

        assertTrue(finished)
    }

    @Test
    fun currentPageSurvivesStateRestoration() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent { OnboardingFlow() }

        composeTestRule.onNodeWithText(string(R.string.onboarding_action_next)).performClick()
        composeTestRule.onNodeWithText(string(R.string.onboarding_action_next)).performClick()
        composeTestRule.onNodeWithText(string(R.string.onboarding_ai_title)).assertExists()

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText(string(R.string.onboarding_ai_title)).assertExists()
    }
}
