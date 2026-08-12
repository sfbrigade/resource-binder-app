package com.example.myapplication
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale
class SpeechRecognitionActivity: ComponentActivity(){

    val options: SpeechRecognizerOptions =
        speechRecognizerOptions {
            locale = Locale.US
            preferredMode = SpeechRecognizerOptions.Mode.MODE_ADVANCED
        }
    private lateinit var speechRecognizer: SpeechRecognizer

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speechRecognizer = SpeechRecognition.getClient(options)

        // The launch block MUST be inside a function
        lifecycleScope.launch {
            val status: Int = speechRecognizer.checkStatus()
            if (status == FeatureStatus.DOWNLOADABLE) {
                speechRecognizer.download().collect { downloadStatus ->
                    when (downloadStatus) {
                        is DownloadStatus.DownloadCompleted -> {
                            startMyRecognition(speechRecognizer)
                        }
                        is DownloadStatus.DownloadFailed -> { /* Handle error */ }
                        is DownloadStatus.DownloadProgress -> { /* Update UI */ }
                        else -> {}
                    }
                }
            } else if (status == FeatureStatus.AVAILABLE) {
                startMyRecognition(speechRecognizer)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    suspend fun startMyRecognition(recognizer: SpeechRecognizer) {
        val request: SpeechRecognizerRequest = speechRecognizerRequest {
            audioSource = AudioSource.fromMic()
        }
        recognizer.startRecognition(request).collect { response ->
            speechRecognizer?.startRecognition(request)?.collect { response ->
                when (response) {
                    is SpeechRecognizerResponse.PartialTextResponse -> {
                        runOnUiThread {

                        }
                    }

                    else -> {}
                }

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