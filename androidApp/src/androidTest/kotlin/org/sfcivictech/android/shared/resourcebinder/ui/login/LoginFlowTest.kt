package org.sfcivictech.android.shared.resourcebinder.ui.login

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.sfcivictech.android.shared.resourcebinder.FakeAccountService
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.SendSignInLinkResult

class LoginFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    private fun enterEmail(email: String) {
        composeTestRule.onNodeWithTag(LOGIN_EMAIL_FIELD_TEST_TAG).performTextInput(email)
    }

    private fun clickSubmit() {
        composeTestRule.onNodeWithText(string(R.string.login_welcome_action_primary)).performClick()
    }

    @Test
    fun startsOnTheWelcomeScreen() {
        composeTestRule.setContent { LoginFlow() }

        composeTestRule.onNodeWithText(string(R.string.login_welcome_title)).assertExists()
    }

    @Test
    fun submitButtonIsDisabledUntilEmailIsEntered() {
        composeTestRule.setContent { LoginFlow() }

        composeTestRule.onNodeWithText(string(R.string.login_welcome_action_primary)).assertIsNotEnabled()

        enterEmail("nikolai@civic-tech.design")

        composeTestRule.onNodeWithText(string(R.string.login_welcome_action_primary)).assertIsEnabled()
    }

    @Test
    fun invalidEmailShowsFieldErrorAndDoesNotSubmit() {
        composeTestRule.setContent { LoginFlow() }

        enterEmail("nikolai@1234567")
        clickSubmit()

        composeTestRule.onNodeWithText(string(R.string.login_error_invalid_email)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.login_welcome_title)).assertExists()
    }

    @Test
    fun successfulSubmitReachesCheckYourEmail() {
        composeTestRule.setContent { LoginFlow(accountService = FakeAccountService()) }

        enterEmail("nikolai@civic-tech.design")
        clickSubmit()

        composeTestRule.onNodeWithText(string(R.string.login_check_email_title)).assertExists()
    }

    @Test
    fun accountNotFoundReachesTheAccountNotFoundScreen() {
        val fakeAccountService = FakeAccountService().apply {
            sendSignInLinkOutput = SendSignInLinkResult.ACCOUNT_NOT_FOUND
        }
        composeTestRule.setContent { LoginFlow(accountService = fakeAccountService) }

        enterEmail("nikolai@civic-tech.design")
        clickSubmit()

        composeTestRule.onNodeWithText(string(R.string.login_account_not_found_title)).assertExists()
    }

    @Test
    fun networkErrorReturnsToWelcomeWithABanner() {
        val fakeAccountService = FakeAccountService().apply {
            sendSignInLinkOutput = SendSignInLinkResult.NETWORK_ERROR
        }
        composeTestRule.setContent { LoginFlow(accountService = fakeAccountService) }

        enterEmail("nikolai@civic-tech.design")
        clickSubmit()

        composeTestRule.onNodeWithText(string(R.string.login_error_network)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.login_welcome_title)).assertExists()
    }

    @Test
    fun tryAnotherEmailReturnsToTheWelcomeScreen() {
        val fakeAccountService = FakeAccountService().apply {
            sendSignInLinkOutput = SendSignInLinkResult.ACCOUNT_NOT_FOUND
        }
        composeTestRule.setContent { LoginFlow(accountService = fakeAccountService) }

        enterEmail("nikolai@civic-tech.design")
        clickSubmit()
        composeTestRule.onNodeWithText(string(R.string.login_account_not_found_action_secondary)).performClick()

        composeTestRule.onNodeWithText(string(R.string.login_welcome_title)).assertExists()
    }

    @Test
    fun debugSimulateExpiredReachesTheExpiredScreen() {
        composeTestRule.setContent { LoginFlow(accountService = FakeAccountService()) }

        enterEmail("nikolai@civic-tech.design")
        clickSubmit()
        composeTestRule.onNodeWithText(string(R.string.login_check_email_title)).assertExists()

        composeTestRule.onNodeWithTag(LOGIN_DEBUG_VERIFY_EXPIRED_TEST_TAG).performClick()

        //the debug trigger has an artificial delay before it resolves, so the
        //expired screen doesn't appear immediately after the click returns
        val expiredTitle = string(R.string.login_link_expired_title)
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodesWithText(expiredTitle).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
