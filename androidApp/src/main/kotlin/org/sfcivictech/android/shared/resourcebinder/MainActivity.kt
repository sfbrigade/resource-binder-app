package org.sfcivictech.android.shared.resourcebinder

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.tooling.preview.Preview
import java.util.Calendar
import org.sfcivictech.android.shared.resourcebinder.ui.OnboardingFlow
import org.sfcivictech.android.shared.resourcebinder.ui.home.CarePlanStatus
import org.sfcivictech.android.shared.resourcebinder.ui.home.CarePlanSummary
import org.sfcivictech.android.shared.resourcebinder.ui.home.HomeScreen
import org.sfcivictech.android.shared.resourcebinder.ui.home.greetingForHour
import org.sfcivictech.android.shared.resourcebinder.ui.login.LoginFlow
import org.sfcivictech.android.shared.resourcebinder.ui.register.RegisterFlow
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

private const val APP_PREFS_NAME = "onboarding_prefs"
private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
private const val KEY_HAS_SIGNED_IN = "has_signed_in"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        // Deliberately Android-only SharedPreferences, not sharedLogic/SQLDelight:
        // onboarding and sign-in state are themselves Android-only for now, so this
        // stays consistent with that rather than sharing state a second platform
        // doesn't use yet.
        val prefs = getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)

        setContent {
            ResourceBinderTheme {
                var showOnboarding by rememberSaveable {
                    mutableStateOf(!prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false))
                }
                var showLogin by rememberSaveable {
                    mutableStateOf(!prefs.getBoolean(KEY_HAS_SIGNED_IN, false))
                }

                // Non-null while the request access flow is showing; holds the email to
                // prefill it with (blank when there's nothing to carry over)
                var registerEmail by rememberSaveable { mutableStateOf<String?>(null) }
                // Carried back to the login screen when the user leaves request access
                var loginEmail by rememberSaveable { mutableStateOf("") }

                if (showOnboarding) {
                    OnboardingFlow(
                        onFinished = {
                            prefs.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, true).apply()
                            showOnboarding = false
                        },
                    )
                } else if (registerEmail != null) {
                    RegisterFlow(
                        initialEmail = registerEmail.orEmpty(),
                        onBack = { registerEmail = null },
                        onGoToSignIn = { email ->
                            loginEmail = email
                            showLogin = true
                            registerEmail = null
                        },
                        onRegistered = {
                            prefs.edit().putBoolean(KEY_HAS_SIGNED_IN, true).apply()
                            showLogin = false
                            registerEmail = null
                        },
                    )
                } else if (showLogin) {
                    LoginFlow(
                        initialEmail = loginEmail,
                        onRequestAccess = { email -> registerEmail = email },
                        onSignedIn = {
                            prefs.edit().putBoolean(KEY_HAS_SIGNED_IN, true).apply()
                            showLogin = false
                        },
                    )
                } else if (BuildConfig.DEBUG) {
                    // Laid out below Home rather than over it so they can't cover content
                    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                        Box(modifier = Modifier.weight(1f)) { HomeDestination() }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TextButton(onClick = { showOnboarding = true }) {
                                Text("Onboarding")
                            }
                            TextButton(onClick = { showLogin = true }) {
                                Text("Login")
                            }
                            TextButton(onClick = { registerEmail = "" }) {
                                Text("Register")
                            }
                        }
                    }
                } else {
                    HomeDestination()
                }
            }
        }
    }
}

// Placeholder data shown only in debug builds, until profile and care plan data
// exist. Release builds show Home with no name and no care plans. The menu, care
// plan and start actions have no destinations yet.
private val debugCarePlans = listOf(
    CarePlanSummary("#0003", listOf("Shelter", "Food"), "12 min ago", CarePlanStatus.IN_PROGRESS),
    CarePlanSummary("#0002", listOf("Shelter", "Food"), "Yesterday", CarePlanStatus.SHARED),
    CarePlanSummary("#0001", listOf("Shelter", "Food", "Medical"), "Yesterday", CarePlanStatus.SYNC_PENDING),
)

@Composable
private fun HomeDestination() {
    // Calendar rather than java.time, which needs API 26 and minSdk is 24
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    HomeScreen(
        greeting = stringResource(greetingForHour(hour)),
        userName = if (BuildConfig.DEBUG) "Marcus Guevara" else "",
        teamName = if (BuildConfig.DEBUG) "RAMS Outreach Team" else "",
        recentCarePlans = if (BuildConfig.DEBUG) debugCarePlans else emptyList(),
        onStartCarePlanClick = {},
        onMenuClick = {},
        onCarePlanClick = {},
        onAllCarePlansClick = {},
    )
}

@Preview
@Composable
fun AppAndroidPreview() {
    ResourceBinderTheme {
        OnboardingFlow()
    }
}
