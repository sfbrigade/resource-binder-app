package org.sfcivictech.android.shared.resourcebinder.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

@Composable
fun OnboardingDescribeScreen(
    onSkip: () -> Unit = {},
    onBack: () -> Unit = {},
    onNext: () -> Unit = {},
) {
    OnboardingPage(
        title = stringResource(R.string.onboarding_describe_title),
        description = stringResource(R.string.onboarding_describe_description),
        pageCount = 4,
        currentPage = 1,
        buttonLabel = stringResource(R.string.onboarding_action_next),
        onButtonClick = onNext,
        onSkip = onSkip,
        showBackButton = true,
        onBack = onBack,
    )
}

@Preview(showBackground = true)
@Composable
private fun OnboardingDescribeScreenPreview() {
    ResourceBinderTheme {
        OnboardingDescribeScreen()
    }
}
