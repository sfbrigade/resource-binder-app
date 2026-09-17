package org.sfcivictech.android.shared.resourcebinder.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

@Composable
private fun OnboardingStepPreview(index: Int) {
    val step = onboardingSteps[index]
    ResourceBinderTheme {
        OnboardingPage(
            title = stringResource(step.titleRes),
            description = stringResource(step.descriptionRes),
            pageCount = onboardingSteps.size,
            currentPage = index,
            buttonLabel = stringResource(step.buttonLabelRes),
            onButtonClick = {},
            onSkip = {},
            showBackButton = step.showBackButton,
        )
    }
}

@Preview(showBackground = true, name = "1. Welcome")
@Composable
private fun OnboardingWelcomeStepPreview() = OnboardingStepPreview(index = 0)

@Preview(showBackground = true, name = "2. Describe")
@Composable
private fun OnboardingDescribeStepPreview() = OnboardingStepPreview(index = 1)

@Preview(showBackground = true, name = "3. AI helps")
@Composable
private fun OnboardingAiStepPreview() = OnboardingStepPreview(index = 2)

@Preview(showBackground = true, name = "4. Field")
@Composable
private fun OnboardingFieldStepPreview() = OnboardingStepPreview(index = 3)
