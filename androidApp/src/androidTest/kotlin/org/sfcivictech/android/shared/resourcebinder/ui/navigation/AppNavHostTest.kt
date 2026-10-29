package org.sfcivictech.android.shared.resourcebinder.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

class AppNavHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var onboardingSeenCount = 0
    private var signedInCount = 0

    private fun string(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    private fun setHost(hasSeenOnboarding: Boolean, hasSignedIn: Boolean) {
        composeTestRule.setContent {
            ResourceBinderTheme {
                AppNavHost(
                    hasSeenOnboarding = hasSeenOnboarding,
                    hasSignedIn = hasSignedIn,
                    onOnboardingSeen = { onboardingSeenCount++ },
                    onSignedIn = { signedInCount++ },
                )
            }
        }
    }

    @Test
    fun startsOnOnboardingWhenNotSeen() {
        setHost(hasSeenOnboarding = false, hasSignedIn = false)

        composeTestRule.onNodeWithText(string(R.string.onboarding_action_skip)).assertExists()
    }

    @Test
    fun finishingOnboardingWhenNotSignedInGoesToLoginAndReportsIt() {
        setHost(hasSeenOnboarding = false, hasSignedIn = false)

        composeTestRule.onNodeWithText(string(R.string.onboarding_action_skip)).performClick()

        composeTestRule.onNodeWithText(string(R.string.login_welcome_title)).assertExists()
        assertEquals(1, onboardingSeenCount)
    }

    @Test
    fun startsOnLoginWhenOnboardedButNotSignedIn() {
        setHost(hasSeenOnboarding = true, hasSignedIn = false)

        composeTestRule.onNodeWithText(string(R.string.login_welcome_title)).assertExists()
    }

    @Test
    fun startsOnHomeWhenSignedIn() {
        setHost(hasSeenOnboarding = true, hasSignedIn = true)

        composeTestRule.onNodeWithText(string(R.string.home_title)).assertExists()
    }

    @Test
    fun debugFooterRegisterOpensRequestAccessAndBackReturnsHome() {
        setHost(hasSeenOnboarding = true, hasSignedIn = true)

        composeTestRule.onNodeWithText("Register").performClick()
        //the title and the submit button share a label, so check the name field's placeholder
        composeTestRule.onNodeWithText(string(R.string.register_name_placeholder)).assertExists()

        Espresso.pressBack()

        composeTestRule.onNodeWithText(string(R.string.home_title)).assertExists()
    }

    @Test
    fun debugFooterLoginOpensLogin() {
        setHost(hasSeenOnboarding = true, hasSignedIn = true)

        composeTestRule.onNodeWithText("Login").performClick()

        composeTestRule.onNodeWithText(string(R.string.login_welcome_title)).assertExists()
    }

    @Test
    fun debugFooterOnboardingReturnsHomeWhenFinishedBecauseTheUserIsSignedIn() {
        setHost(hasSeenOnboarding = true, hasSignedIn = true)

        composeTestRule.onNodeWithText("Onboarding").performClick()
        composeTestRule.onNodeWithText(string(R.string.onboarding_action_skip)).performClick()

        composeTestRule.onNodeWithText(string(R.string.home_title)).assertExists()
        assertEquals(1, onboardingSeenCount)
    }
}
