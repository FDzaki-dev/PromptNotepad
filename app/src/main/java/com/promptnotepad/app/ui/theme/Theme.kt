package com.promptnotepad.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

val PremiumDarkScheme = darkColorScheme(
    primary = TextPrimary,
    onPrimary = PureBlack,
    secondary = PremiumAccent,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = DeepGray,
    onSurface = TextSecondary,
    surfaceVariant = SurfaceGray,
    outline = PremiumBorder
)

val PremiumEditorStyle = TextStyle(
    fontFamily = FontFamily.Monospace, // Esensial untuk estetika prompt/code
    fontSize = 15.sp,
    lineHeight = 22.sp,                // Jarak antar baris lega agar tidak lelah membaca
    color = TextPrimary
)

@Composable
fun PromptNotepadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PremiumDarkScheme,
        content = content
    )
}
