package org.sfcivictech.android.shared.resourcebinder.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import java.util.Calendar
import org.sfcivictech.android.shared.resourcebinder.BuildConfig
import org.sfcivictech.android.shared.resourcebinder.careplan.CarePlanCatalog
import org.sfcivictech.android.shared.resourcebinder.ui.OnboardingFlow
import org.sfcivictech.android.shared.resourcebinder.ui.careplan.ManualFlowRoute
import org.sfcivictech.android.shared.resourcebinder.ui.careplan.StartCarePlanScreen
import org.sfcivictech.android.shared.resourcebinder.ui.home.CarePlanStatus
import org.sfcivictech.android.shared.resourcebinder.ui.home.CarePlanSummary
import org.sfcivictech.android.shared.resourcebinder.ui.home.HomeScreen
import org.sfcivictech.android.shared.resourcebinder.ui.home.greetingForHour
import org.sfcivictech.android.shared.resourcebinder.ui.login.LoginFlow
import org.sfcivictech.android.shared.resourcebinder.ui.register.RegisterFlow

// Every screen the app can be on; exactly one is showing at a time
internal enum class Screen { ONBOARDING, LOGIN, REGISTER, HOME, START_CARE_PLAN, MANUAL_FLOW }

internal fun initialScreen(hasSeenOnboarding: Boolean, hasSignedIn: Boolean): Screen = when {
    !hasSeenOnboarding -> Screen.ONBOARDING
    !hasSignedIn -> Screen.LOGIN
    else -> Screen.HOME
}

// Decides which screen is showing and where each screen's exits lead. Whether onboarding
// was seen and whether the user is signed in are stored by the caller, which is told when
// they change.
@Composable
fun AppNavHost(
    hasSeenOnboarding: Boolean,
    hasSignedIn: Boolean,
    onOnboardingSeen: () -> Unit,
    onSignedIn: () -> Unit,
) {
    var screen by rememberSaveable { mutableStateOf(initialScreen(hasSeenOnboarding, hasSignedIn)) }
    var signedIn by rememberSaveable { mutableStateOf(hasSignedIn) }
    // The email carried between sign in and request access, so neither makes the user retype it
    var emailDraft by rememberSaveable { mutableStateOf("") }

    // Where leaving onboarding or request access lands: Home once signed in, else sign in
    val homeOrLogin = if (signedIn) Screen.HOME else Screen.LOGIN

    when (screen) {
        Screen.ONBOARDING -> OnboardingFlow(
            onFinished = {
                onOnboardingSeen()
                screen = homeOrLogin
            },
        )

        Screen.REGISTER -> RegisterFlow(
            initialEmail = emailDraft,
            onBack = { screen = homeOrLogin },
            onGoToSignIn = { email ->
                emailDraft = email
                screen = Screen.LOGIN
            },
            onRegistered = {
                signedIn = true
                onSignedIn()
                screen = Screen.HOME
            },
        )

        Screen.LOGIN -> LoginFlow(
            initialEmail = emailDraft,
            onRequestAccess = { email ->
                emailDraft = email
                screen = Screen.REGISTER
            },
            onSignedIn = {
                signedIn = true
                onSignedIn()
                screen = Screen.HOME
            },
        )

        Screen.START_CARE_PLAN -> StartCarePlanScreen(
            onClose = { screen = Screen.HOME },
            onEnterManually = { screen = Screen.MANUAL_FLOW },
            // Voice recording has no destination yet
            onStartVoice = {},
        )

        Screen.MANUAL_FLOW -> ManualFlowRoute(
            catalog = CarePlanCatalog.default,
            onBack = { screen = Screen.START_CARE_PLAN },
            onExit = { screen = Screen.HOME },
            // The results screen doesn't exist yet
            onFindResources = {},
        )

        Screen.HOME -> {
            val startCarePlan = { screen = Screen.START_CARE_PLAN }
            if (BuildConfig.DEBUG) {
                // Laid out below Home rather than over it so they can't cover content
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    Box(modifier = Modifier.weight(1f)) { HomeDestination(startCarePlan) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        TextButton(onClick = { screen = Screen.ONBOARDING }) {
                            Text("Onboarding")
                        }
                        TextButton(onClick = { screen = Screen.LOGIN }) {
                            Text("Login")
                        }
                        TextButton(
                            onClick = {
                                emailDraft = ""
                                screen = Screen.REGISTER
                            },
                        ) {
                            Text("Register")
                        }
                    }
                }
            } else {
                HomeDestination(startCarePlan)
            }
        }
    }
}

// Placeholder data shown only in debug builds, until profile and care plan data
// exist. Release builds show Home with no name and no care plans. The menu and care
// plan actions have no destinations yet.
private val debugCarePlans = listOf(
    CarePlanSummary("#0003", listOf("Shelter", "Food"), "12 min ago", CarePlanStatus.IN_PROGRESS),
    CarePlanSummary("#0002", listOf("Shelter", "Food"), "Yesterday", CarePlanStatus.SHARED),
    CarePlanSummary("#0001", listOf("Shelter", "Food", "Medical"), "Yesterday", CarePlanStatus.SYNC_PENDING),
)

@Composable
private fun HomeDestination(onStartCarePlanClick: () -> Unit) {
    // Calendar rather than java.time, which needs API 26 and minSdk is 24
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    HomeScreen(
        greeting = stringResource(greetingForHour(hour)),
        userName = if (BuildConfig.DEBUG) "Marcus Guevara" else "",
        teamName = if (BuildConfig.DEBUG) "RAMS Outreach Team" else "",
        recentCarePlans = if (BuildConfig.DEBUG) debugCarePlans else emptyList(),
        onStartCarePlanClick = onStartCarePlanClick,
        onMenuClick = {},
        onCarePlanClick = {},
        onAllCarePlansClick = {},
    )
}
