package org.sfcivictech.android.shared.resourcebinder.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun OnboardingFlow(onFinished: () -> Unit = {}) {
    var currentPage by remember { mutableIntStateOf(0) }

    when (currentPage) {
        0 -> OnboardingWelcomeScreen(
            onSkip = onFinished,
            onNext = { currentPage = 1 },
        )
        1 -> OnboardingDescribeScreen(
            onSkip = onFinished,
            onBack = { currentPage = 0 },
            onNext = { currentPage = 2 },
        )
        2 -> OnboardingAiScreen(
            onSkip = onFinished,
            onBack = { currentPage = 1 },
            onNext = { currentPage = 3 },
        )
        3 -> OnboardingFieldScreen(
            onSkip = onFinished,
            onBack = { currentPage = 2 },
            onGetStarted = onFinished,
        )
    }
}
