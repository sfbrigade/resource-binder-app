package org.sfcivictech.android.shared.resourcebinder.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

@Composable
fun OnboardingFieldScreen(
    onSkip: () -> Unit = {},
    onBack: () -> Unit = {},
    onGetStarted: () -> Unit = {},
) {
    OnboardingPage(
        title = stringResource(R.string.onboarding_field_title),
        description = stringResource(R.string.onboarding_field_description),
        pageCount = 4,
        currentPage = 3,
        buttonLabel = stringResource(R.string.onboarding_action_get_started),
        onButtonClick = onGetStarted,
        onSkip = onSkip,
        showBackButton = true,
        onBack = onBack,
    )
}

@Preview(showBackground = true)
@Composable
private fun OnboardingFieldScreenPreview() {
    ResourceBinderTheme {
        OnboardingFieldScreen()
    }
}
