package org.sfcivictech.android.shared.resourcebinder.ui.register

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.sfcivictech.android.shared.resourcebinder.FakeAccountService
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.RequestAccessResult

class RegisterFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(resId: Int, vararg formatArgs: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId, *formatArgs)

    private fun hasClickLabel(label: String): SemanticsMatcher =
        SemanticsMatcher("has click label '$label'") { node ->
            node.config.getOrNull(SemanticsActions.OnClick)?.label == label
        }

    private fun fillForm(
        name: String = "Nikolai K",
        email: String = "nikolai@civic-tech.design",
        organization: String = "City Outreach Department",
    ) {
        composeTestRule.onNodeWithTag(REGISTER_NAME_FIELD_TEST_TAG).performTextInput(name)
        composeTestRule.onNodeWithTag(REGISTER_EMAIL_FIELD_TEST_TAG).performTextInput(email)
        composeTestRule.onNodeWithTag(REGISTER_ORGANIZATION_FIELD_TEST_TAG).performTextInput(organization)
    }

    // The title and the button are both "Request access", so match the button by role
    private fun submitButton() = composeTestRule.onNode(
        hasText(string(R.string.register_action_submit)) and hasClickAction(),
    )

    // The name field only exists on the form, unlike the title text which the button shares
    private fun assertOnTheForm() {
        composeTestRule.onNodeWithTag(REGISTER_NAME_FIELD_TEST_TAG).assertExists()
    }

    private fun clickSubmit() {
        submitButton().performClick()
    }

    private fun waitForText(text: String) {
        // The debug link triggers and the welcome screen resolve after an artificial delay
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun reachCheckEmail() {
        fillForm()
        clickSubmit()
        composeTestRule.onNodeWithText(string(R.string.register_check_email_title)).assertExists()
    }

    @Test
    fun startsOnTheRequestAccessForm() {
        composeTestRule.setContent { RegisterFlow() }

        assertOnTheForm()
    }

    @Test
    fun emailIsPrefilledFromTheLoginScreen() {
        composeTestRule.setContent { RegisterFlow(initialEmail = "nikolai@civic-tech.design") }

        composeTestRule.onNodeWithText("nikolai@civic-tech.design").assertExists()
    }

    @Test
    fun submitButtonIsDisabledUntilAllFieldsAreFilled() {
        composeTestRule.setContent { RegisterFlow() }

        submitButton().assertIsNotEnabled()

        composeTestRule.onNodeWithTag(REGISTER_NAME_FIELD_TEST_TAG).performTextInput("Nikolai K")
        composeTestRule.onNodeWithTag(REGISTER_EMAIL_FIELD_TEST_TAG).performTextInput("nikolai@civic-tech.design")
        submitButton().assertIsNotEnabled()

        composeTestRule.onNodeWithTag(REGISTER_ORGANIZATION_FIELD_TEST_TAG).performTextInput("City Outreach Department")
        submitButton().assertIsEnabled()
    }

    @Test
    fun leavingAnInvalidFieldShowsItsErrorAndDisablesSubmit() {
        composeTestRule.setContent { RegisterFlow() }

        composeTestRule.onNodeWithTag(REGISTER_NAME_FIELD_TEST_TAG).performTextInput("1")
        // Moving focus to the next field is what "leaving" the name field means
        composeTestRule.onNodeWithTag(REGISTER_EMAIL_FIELD_TEST_TAG).performClick()

        composeTestRule.onNodeWithText(string(R.string.register_error_name)).assertExists()

        composeTestRule.onNodeWithTag(REGISTER_EMAIL_FIELD_TEST_TAG).performTextInput("nikolai@civic-tech.design")
        composeTestRule.onNodeWithTag(REGISTER_ORGANIZATION_FIELD_TEST_TAG).performTextInput("City Outreach Department")
        submitButton().assertIsNotEnabled()
    }

    @Test
    fun correctingAnInvalidFieldClearsItsError() {
        composeTestRule.setContent { RegisterFlow() }

        composeTestRule.onNodeWithTag(REGISTER_NAME_FIELD_TEST_TAG).performTextInput("1")
        composeTestRule.onNodeWithTag(REGISTER_EMAIL_FIELD_TEST_TAG).performClick()
        composeTestRule.onNodeWithText(string(R.string.register_error_name)).assertExists()

        composeTestRule.onNodeWithTag(REGISTER_NAME_FIELD_TEST_TAG).performTextReplacement("Nikolai K")

        composeTestRule.onAllNodesWithText(string(R.string.register_error_name))
            .assertCountEquals(0)
    }

    @Test
    fun correctedFieldFormStaysValidWhenLeftAgain() {
        composeTestRule.setContent { RegisterFlow() }

        composeTestRule.onNodeWithTag(REGISTER_NAME_FIELD_TEST_TAG).performTextInput("1")
        composeTestRule.onNodeWithTag(REGISTER_EMAIL_FIELD_TEST_TAG).performClick()
        composeTestRule.onNodeWithTag(REGISTER_NAME_FIELD_TEST_TAG).performTextReplacement("Nikolai K")
        composeTestRule.onNodeWithTag(REGISTER_EMAIL_FIELD_TEST_TAG).performClick()

        composeTestRule.onAllNodesWithText(string(R.string.register_error_name))
            .assertCountEquals(0)
    }

    @Test
    fun successfulSubmitReachesCheckYourEmail() {
        composeTestRule.setContent { RegisterFlow(accountService = FakeAccountService()) }

        reachCheckEmail()

        composeTestRule.onNodeWithText(
            string(R.string.register_check_email_description, "nikolai@civic-tech.design"),
        ).assertExists()
    }

    @Test
    fun accountAlreadyExistsReachesTheAlreadyExistsScreen() {
        val fakeAccountService = FakeAccountService().apply {
            requestAccessOutput = RequestAccessResult.ACCOUNT_ALREADY_EXISTS
        }
        composeTestRule.setContent { RegisterFlow(accountService = fakeAccountService) }

        fillForm()
        clickSubmit()

        composeTestRule.onNodeWithText(string(R.string.register_exists_title)).assertExists()
    }

    @Test
    fun useADifferentEmailReturnsToTheFormWithEntriesKept() {
        val fakeAccountService = FakeAccountService().apply {
            requestAccessOutput = RequestAccessResult.ACCOUNT_ALREADY_EXISTS
        }
        composeTestRule.setContent { RegisterFlow(accountService = fakeAccountService) }

        fillForm()
        clickSubmit()
        composeTestRule.onNodeWithText(string(R.string.register_exists_action_secondary)).performClick()

        assertOnTheForm()
        composeTestRule.onNodeWithText("Nikolai K").assertExists()
        composeTestRule.onNodeWithText("City Outreach Department").assertExists()
    }

    @Test
    fun goToSignInLeavesTheFlow() {
        val fakeAccountService = FakeAccountService().apply {
            requestAccessOutput = RequestAccessResult.ACCOUNT_ALREADY_EXISTS
        }
        var signInEmail: String? = null
        composeTestRule.setContent {
            RegisterFlow(accountService = fakeAccountService, onGoToSignIn = { signInEmail = it })
        }

        fillForm()
        clickSubmit()
        composeTestRule.onNodeWithText(string(R.string.register_exists_action_primary)).performClick()

        // The email carries back so sign-in doesn't make the user type it again
        assertEquals("nikolai@civic-tech.design", signInEmail)
    }

    @Test
    fun checkYourEmailShowsTheTrimmedEmail() {
        composeTestRule.setContent { RegisterFlow(accountService = FakeAccountService()) }

        fillForm(email = "  nikolai@civic-tech.design ")
        clickSubmit()

        composeTestRule.onNodeWithText(
            string(R.string.register_check_email_description, "nikolai@civic-tech.design"),
        ).assertExists()
    }

    @Test
    fun formEntriesSurviveStateRestoration() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent { RegisterFlow() }

        fillForm()
        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText("Nikolai K").assertExists()
        composeTestRule.onNodeWithText("nikolai@civic-tech.design").assertExists()
        composeTestRule.onNodeWithText("City Outreach Department").assertExists()
    }

    @Test
    fun checkYourEmailSurvivesStateRestoration() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent { RegisterFlow(accountService = FakeAccountService()) }

        reachCheckEmail()
        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText(string(R.string.register_check_email_title)).assertExists()
    }

    @Test
    fun welcomeSurvivesStateRestoration() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent { RegisterFlow(accountService = FakeAccountService()) }

        reachCheckEmail()
        composeTestRule.onNodeWithTag(REGISTER_DEBUG_VERIFY_SUCCESS_TEST_TAG).performClick()
        waitForText(string(R.string.register_welcome_action))
        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText(string(R.string.register_welcome_action)).assertExists()
    }

    @Test
    fun fieldsAreNamedForScreenReaders() {
        composeTestRule.setContent { RegisterFlow() }

        composeTestRule.onNodeWithContentDescription(string(R.string.register_name_field_description)).assertExists()
        composeTestRule.onNodeWithContentDescription(string(R.string.register_email_field_description)).assertExists()
        composeTestRule.onNodeWithContentDescription(string(R.string.register_organization_field_description)).assertExists()
    }

    @Test
    fun networkErrorBannerIsAnnouncedToScreenReaders() {
        val fakeAccountService = FakeAccountService().apply {
            requestAccessOutput = RequestAccessResult.NETWORK_ERROR
        }
        composeTestRule.setContent { RegisterFlow(accountService = fakeAccountService) }

        fillForm()
        clickSubmit()

        composeTestRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
        ).assertExists()
    }

    @Test
    fun networkErrorReturnsToTheFormWithABannerAndEntriesKept() {
        val fakeAccountService = FakeAccountService().apply {
            requestAccessOutput = RequestAccessResult.NETWORK_ERROR
        }
        composeTestRule.setContent { RegisterFlow(accountService = fakeAccountService) }

        fillForm()
        clickSubmit()

        composeTestRule.onNodeWithText(string(R.string.register_error_network)).assertExists()
        assertOnTheForm()
        composeTestRule.onNodeWithText("Nikolai K").assertExists()
    }

    @Test
    fun backOnTheFormLeavesTheFlow() {
        var wentBack = false
        composeTestRule.setContent { RegisterFlow(onBack = { wentBack = true }) }

        composeTestRule.onNode(hasClickLabel(string(R.string.onboarding_action_back))).performClick()

        assertTrue(wentBack)
    }

    @Test
    fun backOnCheckYourEmailReturnsToTheFormWithEntriesKept() {
        composeTestRule.setContent { RegisterFlow(accountService = FakeAccountService()) }

        reachCheckEmail()
        composeTestRule.onNode(hasClickLabel(string(R.string.onboarding_action_back))).performClick()

        assertOnTheForm()
        composeTestRule.onNodeWithText("Nikolai K").assertExists()
    }

    @Test
    fun resendIsDisabledDuringTheCooldown() {
        composeTestRule.setContent { RegisterFlow(accountService = FakeAccountService()) }

        reachCheckEmail()

        composeTestRule.onNodeWithText(string(R.string.register_check_email_resend_cooldown, 30)).assertIsNotEnabled()
    }

    @Test
    fun debugSimulateSuccessCreatesTheAccountAndShowsWelcome() {
        composeTestRule.setContent { RegisterFlow(accountService = FakeAccountService()) }

        reachCheckEmail()
        composeTestRule.onNodeWithTag(REGISTER_DEBUG_VERIFY_SUCCESS_TEST_TAG).performClick()

        waitForText(string(R.string.register_welcome_title))
    }

    @Test
    fun welcomeButtonFinishesRegistration() {
        var registered = false
        composeTestRule.setContent {
            RegisterFlow(accountService = FakeAccountService(), onRegistered = { registered = true })
        }

        reachCheckEmail()
        composeTestRule.onNodeWithTag(REGISTER_DEBUG_VERIFY_SUCCESS_TEST_TAG).performClick()
        waitForText(string(R.string.register_welcome_action))
        assertEquals(false, registered)

        composeTestRule.onNodeWithText(string(R.string.register_welcome_action)).performClick()

        assertTrue(registered)
    }

    @Test
    fun debugSimulateExpiredShowsInvitationExpired() {
        composeTestRule.setContent { RegisterFlow(accountService = FakeAccountService()) }

        reachCheckEmail()
        composeTestRule.onNodeWithTag(REGISTER_DEBUG_VERIFY_EXPIRED_TEST_TAG).performClick()

        waitForText(string(R.string.register_invitation_expired_title))
    }

    @Test
    fun sendNewInvitationResubmitsTheSameDetails() {
        composeTestRule.setContent { RegisterFlow(accountService = FakeAccountService()) }

        reachCheckEmail()
        composeTestRule.onNodeWithTag(REGISTER_DEBUG_VERIFY_EXPIRED_TEST_TAG).performClick()
        waitForText(string(R.string.register_invitation_expired_title))

        composeTestRule.onNodeWithText(string(R.string.register_invitation_expired_action)).performClick()

        waitForText(string(R.string.register_check_email_title))
    }

    @Test
    fun debugSimulateErrorShowsSomethingWentWrong() {
        composeTestRule.setContent { RegisterFlow(accountService = FakeAccountService()) }

        reachCheckEmail()
        composeTestRule.onNodeWithTag(REGISTER_DEBUG_VERIFY_ERROR_TEST_TAG).performClick()

        waitForText(string(R.string.register_something_wrong_title))
    }
}
