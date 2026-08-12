package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

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
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(
        recognizedText = "Preview text",
        onTextChanged = {},
        onSpeechButtonClick = {}
    )
}