package org.sfcivictech.android.shared.resourcebinder.ui.register

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.ui.common.DeadEndScreen
import org.sfcivictech.android.shared.resourcebinder.ui.common.LoadingScreen
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

@Composable
fun RegisterSubmittingScreen() {
    LoadingScreen(
        title = stringResource(R.string.register_submitting_title),
        description = stringResource(R.string.register_submitting_description),
    )
}

@Composable
fun RegisterCreatingAccountScreen() {
    LoadingScreen(
        title = stringResource(R.string.register_creating_title),
        description = stringResource(R.string.register_creating_description),
    )
}

@Composable
fun RegisterWelcomeScreen(onGetStartedClick: () -> Unit) {
    DeadEndScreen(
        title = stringResource(R.string.register_welcome_title),
        description = stringResource(R.string.register_welcome_description),
        primaryButtonLabel = stringResource(R.string.register_welcome_action),
        onPrimaryClick = onGetStartedClick,
    )
}

@Composable
fun RegisterAccountExistsScreen(
    onUseDifferentEmailClick: () -> Unit,
    onGoToSignInClick: () -> Unit,
    onBack: () -> Unit,
) {
    DeadEndScreen(
        title = stringResource(R.string.register_exists_title),
        description = stringResource(R.string.register_exists_description),
        secondaryButtonLabel = stringResource(R.string.register_exists_action_secondary),
        onSecondaryClick = onUseDifferentEmailClick,
        primaryButtonLabel = stringResource(R.string.register_exists_action_primary),
        onPrimaryClick = onGoToSignInClick,
        onBack = onBack,
    )
}

@Composable
fun RegisterInvitationExpiredScreen(onSendNewInvitationClick: () -> Unit) {
    DeadEndScreen(
        title = stringResource(R.string.register_invitation_expired_title),
        description = stringResource(R.string.register_invitation_expired_description),
        primaryButtonLabel = stringResource(R.string.register_invitation_expired_action),
        onPrimaryClick = onSendNewInvitationClick,
    )
}

@Composable
fun RegisterSomethingWentWrongScreen(onBack: () -> Unit) {
    DeadEndScreen(
        title = stringResource(R.string.register_something_wrong_title),
        description = stringResource(R.string.register_something_wrong_description),
        onBack = onBack,
    )
}

@Preview(showBackground = true, name = "1. Submitting")
@Composable
private fun RegisterSubmittingPreview() {
    ResourceBinderTheme { RegisterSubmittingScreen() }
}

@Preview(showBackground = true, name = "2. Creating account")
@Composable
private fun RegisterCreatingAccountPreview() {
    ResourceBinderTheme { RegisterCreatingAccountScreen() }
}

@Preview(showBackground = true, name = "3. Welcome")
@Composable
private fun RegisterWelcomePreview() {
    ResourceBinderTheme { RegisterWelcomeScreen(onGetStartedClick = {}) }
}

@Preview(showBackground = true, name = "4. Account already exists")
@Composable
private fun RegisterAccountExistsPreview() {
    ResourceBinderTheme {
        RegisterAccountExistsScreen(
            onUseDifferentEmailClick = {},
            onGoToSignInClick = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, name = "5. Invitation expired")
@Composable
private fun RegisterInvitationExpiredPreview() {
    ResourceBinderTheme { RegisterInvitationExpiredScreen(onSendNewInvitationClick = {}) }
}

@Preview(showBackground = true, name = "6. Something went wrong")
@Composable
private fun RegisterSomethingWentWrongPreview() {
    ResourceBinderTheme { RegisterSomethingWentWrongScreen(onBack = {}) }
}
