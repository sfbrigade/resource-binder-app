package org.sfcivictech.android.shared.resourcebinder.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

@Composable
fun LoginSigningInScreen() {
    LoginLoadingScreen(
        title = stringResource(R.string.login_signing_in_title),
        description = stringResource(R.string.login_signing_in_description),
    )
}

@Preview(showBackground = true)
@Composable
private fun LoginSigningInPreview() {
    ResourceBinderTheme {
        LoginSigningInScreen()
    }
}
