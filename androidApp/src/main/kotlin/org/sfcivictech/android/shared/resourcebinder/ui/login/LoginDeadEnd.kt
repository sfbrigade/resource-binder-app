package org.sfcivictech.android.shared.resourcebinder.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.ui.common.DeadEndScreen
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

@Composable
fun LoginAccountNotFoundScreen(
    onTryAnotherEmailClick: () -> Unit,
    onRequestAccessClick: () -> Unit,
) {
    DeadEndScreen(
        title = stringResource(R.string.login_account_not_found_title),
        description = stringResource(R.string.login_account_not_found_description),
        secondaryButtonLabel = stringResource(R.string.login_account_not_found_action_secondary),
        onSecondaryClick = onTryAnotherEmailClick,
        primaryButtonLabel = stringResource(R.string.login_account_not_found_action_primary),
        onPrimaryClick = onRequestAccessClick,
    )
}

@Composable
fun LoginLinkExpiredScreen(onSendNewLinkClick: () -> Unit) {
    DeadEndScreen(
        title = stringResource(R.string.login_link_expired_title),
        description = stringResource(R.string.login_link_expired_description),
        primaryButtonLabel = stringResource(R.string.login_link_expired_action),
        onPrimaryClick = onSendNewLinkClick,
    )
}

@Composable
fun LoginLinkAlreadyUsedScreen(onSendNewLinkClick: () -> Unit) {
    DeadEndScreen(
        title = stringResource(R.string.login_link_already_used_title),
        description = stringResource(R.string.login_link_already_used_description),
        primaryButtonLabel = stringResource(R.string.login_link_already_used_action),
        onPrimaryClick = onSendNewLinkClick,
    )
}

@Preview(showBackground = true, name = "1. Account not found")
@Composable
private fun LoginAccountNotFoundPreview() {
    ResourceBinderTheme {
        LoginAccountNotFoundScreen(
            onTryAnotherEmailClick = {},
            onRequestAccessClick = {},
        )
    }
}

@Preview(showBackground = true, name = "2. Link expired")
@Composable
private fun LoginLinkExpiredPreview() {
    ResourceBinderTheme {
        LoginLinkExpiredScreen(onSendNewLinkClick = {})
    }
}

@Preview(showBackground = true, name = "3. Link already used")
@Composable
private fun LoginLinkAlreadyUsedPreview() {
    ResourceBinderTheme {
        LoginLinkAlreadyUsedScreen(onSendNewLinkClick = {})
    }
}
