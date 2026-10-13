package org.sfcivictech.android.shared.resourcebinder.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

internal data class OnboardingStep(
    val titleRes: Int,
    val descriptionRes: Int,
    val showBackButton: Boolean,
    val buttonLabelRes: Int,
)

internal val onboardingSteps = listOf(
    OnboardingStep(
        titleRes = R.string.onboarding_welcome_title,
        descriptionRes = R.string.onboarding_welcome_description,
        showBackButton = false,
        buttonLabelRes = R.string.onboarding_action_next,
    ),
    OnboardingStep(
        titleRes = R.string.onboarding_describe_title,
        descriptionRes = R.string.onboarding_describe_description,
        showBackButton = true,
        buttonLabelRes = R.string.onboarding_action_next,
    ),
    OnboardingStep(
        titleRes = R.string.onboarding_ai_title,
        descriptionRes = R.string.onboarding_ai_description,
        showBackButton = true,
        buttonLabelRes = R.string.onboarding_action_next,
    ),
    OnboardingStep(
        titleRes = R.string.onboarding_field_title,
        descriptionRes = R.string.onboarding_field_description,
        showBackButton = true,
        buttonLabelRes = R.string.onboarding_action_get_started,
    ),
)

@Composable
fun OnboardingFlow(onFinished: () -> Unit = {}) {
    var currentPage by rememberSaveable { mutableStateOf(0) }

    // Disabled at page 0: back should exit the app like any other root screen
    BackHandler(enabled = currentPage > 0) {
        currentPage -= 1
    }

    val step = onboardingSteps[currentPage]
    val isLastStep = currentPage == onboardingSteps.lastIndex

    OnboardingPage(
        title = stringResource(step.titleRes),
        description = stringResource(step.descriptionRes),
        pageCount = onboardingSteps.size,
        currentPage = currentPage,
        buttonLabel = stringResource(step.buttonLabelRes),
        onButtonClick = { if (isLastStep) onFinished() else currentPage += 1 },
        onSkip = onFinished,
        showBackButton = step.showBackButton,
        onBack = { if (currentPage > 0) currentPage -= 1 },
    )
}

@Preview(showBackground = true)
@Composable
private fun OnboardingFlowPreview() {
    ResourceBinderTheme {
        OnboardingFlow()
    }
}
