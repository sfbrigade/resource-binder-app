package com.example.myapplication


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource

import myapplication.shared.generated.resources.Res
import myapplication.shared.generated.resources.compose_multiplatform

@Composable
@Preview


fun App(
    recognizedText: String = "",
    onTextChanged: (String) -> Unit = {},
    onSpeechButtonClick: () -> Unit = {}
) {
    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = onSpeechButtonClick) {
                Text("Start Speech Ressssscognition")
            }

            if (recognizedText.isNotEmpty()) {
                TextField(
                    value = recognizedText,
                    onValueChange = onTextChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Recognized Text") }
                )
            }
        }
    }
}