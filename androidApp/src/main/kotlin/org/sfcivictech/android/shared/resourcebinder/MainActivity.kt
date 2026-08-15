package org.sfcivictech.android.shared.resourcebinder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerRequest
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.ui.unit.dp
import org.sfcivictech.android.shared.resourcebinder.ui.OnboardingFlow
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

private const val ONBOARDING_PREFS_NAME = "onboarding_prefs"
private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"

class MainActivity : ComponentActivity() {
    private var recognizedText by mutableStateOf("")
    private lateinit var speechRecognizer: SpeechRecognizer

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->

        if (isGranted) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startSpeechRecognition()
            }
        }
    }

    private val options: SpeechRecognizerOptions =
        speechRecognizerOptions {
            locale = Locale.US
            preferredMode = SpeechRecognizerOptions.Mode.MODE_BASIC
        }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        // Deliberately Android-only SharedPreferences, not sharedLogic/SQLDelight:
        // onboarding's UI is itself Android-only for now, so this stays consistent
        // with that rather than sharing state a second platform doesn't use yet.
        val prefs = getSharedPreferences(ONBOARDING_PREFS_NAME, Context.MODE_PRIVATE)

        speechRecognizer = SpeechRecognition.getClient(options)

        setContent {
            App(
                recognizedText = recognizedText,
                onTextChanged = { newText ->
                    recognizedText = newText
                },
                onSpeechButtonClick = {

                    checkPermissionAndStart()
                }
            )
        }
    }

    private fun checkPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                    startSpeechRecognition()
                }
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startSpeechRecognition() {

        lifecycleScope.launch {

            val status: Int = speechRecognizer.checkStatus()
            println(FeatureStatus.DOWNLOADABLE)
            println(FeatureStatus.AVAILABLE)
            if (status == FeatureStatus.DOWNLOADABLE ) {
                speechRecognizer.download().collect { downloadStatus ->
                    if (downloadStatus is DownloadStatus.DownloadCompleted) {

                        beginRecognition()
                    }
                }
            } else if (status == FeatureStatus.AVAILABLE) {
                beginRecognition()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun beginRecognition() {
        val request: SpeechRecognizerRequest = speechRecognizerRequest {
            audioSource = AudioSource.fromMic()
        }
        speechRecognizer.startRecognition(request).collect { response ->
            when (response) {
                is SpeechRecognizerResponse.PartialTextResponse -> {
                    recognizedText = response.text
                }
                is SpeechRecognizerResponse.FinalTextResponse -> {
                    recognizedText = response.text
                }
                is SpeechRecognizerResponse.ErrorResponse -> {
                    recognizedText = "Error: ${response.e.message}"
                }
                else -> {}
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.close()
            ResourceBinderTheme {
                var showOnboarding by rememberSaveable {
                    mutableStateOf(!prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false))
                }

                if (showOnboarding) {
                    OnboardingFlow(
                        onFinished = {
                            prefs.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, true).apply()
                            showOnboarding = false
                        },
                    )
                } else if (BuildConfig.DEBUG) {
                    Box {
                        App()
                        Button(
                            onClick = { showOnboarding = true },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .safeContentPadding()
                                .padding(8.dp),
                        ) {
                            Text("Preview onboarding")
                        }
                    }
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
