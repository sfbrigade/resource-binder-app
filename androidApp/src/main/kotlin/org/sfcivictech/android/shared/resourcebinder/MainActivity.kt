package org.sfcivictech.android.shared.resourcebinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import org.sfcivictech.android.shared.resourcebinder.ui.OnboardingFlow
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ResourceBinderTheme {
                // App() is sharedUI's debug/test stub (currently the CMP template
                // placeholder; PR #3 adds a speech-recognition test button there).
                // Onboarding leads into it until a real describe screen replaces it.
                var showOnboarding by remember { mutableStateOf(true) }
                if (showOnboarding) {
                    OnboardingFlow(onFinished = { showOnboarding = false })
                } else {
                    App()
                }
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