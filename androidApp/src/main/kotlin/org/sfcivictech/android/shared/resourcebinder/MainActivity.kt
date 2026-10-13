package org.sfcivictech.android.shared.resourcebinder

import android.Manifest
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerRequest
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizationRequest
import com.google.mlkit.genai.summarization.Summarizer
import com.google.mlkit.genai.summarization.SummarizerOptions
import com.google.mlkit.genai.summarization.SummarizerOptions.InputType
import com.google.mlkit.genai.summarization.SummarizerOptions.Language
import com.google.mlkit.genai.summarization.SummarizerOptions.OutputType
import android.util.Log
import com.google.mlkit.genai.schema.annotations.Generable
import com.google.mlkit.genai.schema.annotations.Guide
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateTypedContentRequest
import com.google.mlkit.genai.common.DownloadCallback
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import java.util.Locale
@Generable
data class Checklist(
    @Guide(description = "A social worker will be using this tool to search for available resources in a specified area. To aid in their search, this will create a list of isolated important keywords that will be vital to helping the search process.", minItems = 1, maxItems = 5)
    val checklist: List<Check>
)
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sfcivictech.android.shared.resourcebinder.ui.OnboardingFlow
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

private const val ONBOARDING_PREFS_NAME = "onboarding_prefs"
private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"

@Generable("Keywords isolated from social worker searches, needs to be helpful to a social worker when finding resources to help.")
data class Check(
    @Guide(description = "isolate important keywords related to issues that a person is facing", minItems = 1, maxItems = 5)
    val keywords: List<String> = listOf("")
)
class MainActivity : ComponentActivity() {
    private var recognizedText by mutableStateOf("")

    private var articleToSummarize = ""
    private lateinit var speechRecognizer: SpeechRecognizer



    private val summarizerOptions by lazy {
        SummarizerOptions.builder(this)
            .setInputType(InputType.ARTICLE)
            .setOutputType(OutputType.ONE_BULLET)
            .setLanguage(Language.ENGLISH)
            .build()
    }

    private val summarizer by lazy {
        Summarization.getClient(summarizerOptions)
    }


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
        // Deliberately Android-only SharedPreferences, not sharedLogic/SQLDelight:
        // onboarding's UI is itself Android-only for now, so this stays consistent
        // with that rather than sharing state a second platform doesn't use yet.
        val prefs = getSharedPreferences(ONBOARDING_PREFS_NAME, Context.MODE_PRIVATE)

        setContent {
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

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startSpeechRecognition() {

        lifecycleScope.launch {
            val status: Int = speechRecognizer.checkStatus()

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
//                    recognizedText = response.text
                }
                is SpeechRecognizerResponse.FinalTextResponse -> {
//                    recognizedText = response.text
                    articleToSummarize = response.text
                    recognizedText = response.text
                    lifecycleScope.launch {
                        checkPrompt();
                    }
                }
                is SpeechRecognizerResponse.ErrorResponse -> {
                    recognizedText = "Error: ${response.e.message}"
                }
                else -> {}
            }
        }
    }

    suspend fun checkPrompt(){
        lifecycleScope.launch {
            val generativeModel = Generation.getClient()
            val status = generativeModel.checkStatus()
            val isAvailable = generativeModel.isStructuredOutputFeatureAvailable()

            if (isAvailable) {
                // You can safely use the Structured Output API here
                println("Structured Output is supported on this device!")
                prepareAndStartPrompt()
            } else {
                // Fallback to standard text output or show an error
                if(status == FeatureStatus.DOWNLOADABLE){
                    generativeModel.download().collect{
                        downloadStatus -> if(downloadStatus is DownloadStatus.DownloadCompleted){
                            prepareAndStartPrompt()
                    }
                    }
                }
                println("Structured Output is not available.")
            }
        }
    }
    suspend fun prepareAndStartPrompt(){
        val generativeModel = Generation.getClient()
        val promptText = recognizedText;
        val baseRequest = GenerateContentRequest.Builder(TextPart(promptText)).build()

        val typedRequest = generateTypedContentRequest(
            generateContentRequest = baseRequest,
            outputClass = Checklist::class
        )

// 4. Run the inference
        try {
            val typedResponse = generativeModel.generateContent(typedRequest)

            // 5. Access the parsed object
            // The response candidates contain the parsed object of type T (PlantList in this case)
            val checkList: Checklist? = typedResponse.candidates.firstOrNull()?.response

            if (checkList != null) {
                // Process the structured data
                for (plant in checkList.checklist) {
                    Log.d("StructuredOutput", "Found plant: ${plant.keywords} ")
                }
            } else {
                Log.e("StructuredOutput", "Failed to parse response into the desired structure.")

                // Inspect finish reason for details
                val finishReason = typedResponse.candidates.firstOrNull()?.finishReason
                Log.d("StructuredOutput", "Finish reason: $finishReason")
            }
        } catch (e: GenAiException) {
            // Handle API errors
            when (e.errorCode) {
                -104-> {
                    Log.e("StructuredOutput", "The class structure is not supported.")
                }
                -105 -> {
                    Log.e("StructuredOutput", "The model generated values that violate the schema constraints.")
                }
                else -> {
                    Log.e("StructuredOutput", "API error: ${e.message}")
                }
            }
        }
    }

    suspend fun prepareAndStartSummarization() {
        // Check feature availability. Status will be one of the following:
        // UNAVAILABLE, DOWNLOADABLE, DOWNLOADING, AVAILABLE
        val featureStatus = summarizer.checkFeatureStatus().await()

        when (featureStatus) {
            FeatureStatus.DOWNLOADABLE -> {
                println("DOWNLOADING");
                summarizer.downloadFeature(object : DownloadCallback {

                    override fun onDownloadStarted(bytesToDownload: Long) {}

                    override fun onDownloadFailed(e: GenAiException) {
                        recognizedText = "Download Failed: ${e.message}"
                    }
                    override fun onDownloadProgress(totalBytesDownloaded: Long) {}
                    override fun onDownloadCompleted() {
                        startSummarizationRequest(articleToSummarize, summarizer)
                    }
                })
            }
            FeatureStatus.DOWNLOADING, FeatureStatus.AVAILABLE -> {

                startSummarizationRequest(articleToSummarize, summarizer)
            }
            FeatureStatus.UNAVAILABLE -> {
                recognizedText = "Summarization Unavailable on this device"
            }
        }
    }

    fun startSummarizationRequest(text: String, summarizer: Summarizer) {
        // Create task request
        val summarizationRequest = SummarizationRequest.builder(text).build()

        // Start summarization request with streaming response
        summarizer.runInference(summarizationRequest) { newText ->
            recognizedText = recognizedText + newText;

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.close()
        }
        summarizer.close()
    }

    @Preview
    @Composable
    fun AppAndroidPreview() {
        App(
            recognizedText = recognizedText ,
            onTextChanged = {},
            onSpeechButtonClick = {}
        )
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    ResourceBinderTheme {
        OnboardingFlow()
    }
}
