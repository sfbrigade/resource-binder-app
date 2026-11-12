package org.sfcivictech.android.shared.resourcebinder.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.ui.common.LoadingScreen
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

@Composable
fun LoginSendingLinkScreen() {
    LoadingScreen(
        title = stringResource(R.string.login_sending_title),
        description = stringResource(R.string.login_sending_description),
    )
}

@Preview(showBackground = true)
@Composable
private fun LoginSendingLinkPreview() {
    ResourceBinderTheme {
        LoginSendingLinkScreen()
    }
}
