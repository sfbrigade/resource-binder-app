package org.sfcivictech.android.shared.resourcebinder

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.sfcivictech.android.shared.resourcebinder.ui.OnboardingFlow
import org.sfcivictech.android.shared.resourcebinder.ui.navigation.AppNavHost
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
                AppNavHost(
                    hasSeenOnboarding = prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false),
                    hasSignedIn = prefs.getBoolean(KEY_HAS_SIGNED_IN, false),
                    onOnboardingSeen = { prefs.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, true).apply() },
                    onSignedIn = { prefs.edit().putBoolean(KEY_HAS_SIGNED_IN, true).apply() },
                )
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    ResourceBinderTheme {
        OnboardingFlow()
    }
}
