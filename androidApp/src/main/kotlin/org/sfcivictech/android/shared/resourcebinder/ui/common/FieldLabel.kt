package org.sfcivictech.android.shared.resourcebinder.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/** A form field label whose trailing "*" (the required marker) is drawn in the error color. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    val errorColor = MaterialTheme.colorScheme.error
    Text(
        text = buildAnnotatedString {
            append(text.removeSuffix("*"))
            if (text.endsWith("*")) {
                withStyle(SpanStyle(color = errorColor)) { append("*") }
            }
        },
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier,
    )
}
