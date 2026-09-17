package org.sfcivictech.android.shared.resourcebinder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A1B1F),
    onPrimary = Color.White,
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF1A1B1F),
    surfaceVariant = Color(0xFFE7E8EC),
    onSurfaceVariant = Color(0xFF6B7280),
    outlineVariant = Color(0xFFD9DADD),
)

private val baseTypography = Typography()
private val AppTypography = baseTypography.copy(
    headlineMedium = baseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
)

@Composable
fun ResourceBinderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}
