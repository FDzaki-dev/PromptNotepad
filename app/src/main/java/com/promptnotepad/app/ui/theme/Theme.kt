package com.promptnotepad.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Slot warna semantik yang dipakai seluruh layar (bar, tab, teks, aksen, dll).
 * Dulu setiap file UI mengimpor konstanta warna statis (PureBlack, DeepGray, ...)
 * langsung — cukup untuk tema gelap tunggal, tapi tidak bisa berubah saat
 * pengguna mengaktifkan tema terang. [AppColors] + [LocalAppColors] menggantikan
 * pola itu: nilai per-field TETAP sama seperti konstanta lama untuk tema gelap
 * (default), UI cukup membaca LocalAppColors.current alih-alih import statis.
 */
data class AppColors(
    val background: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val codeGreen: Color,
    val locked: Color
)

val darkAppColors = AppColors(
    background = PureBlack,
    surface = DeepGray,
    border = PremiumBorder,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    accent = PremiumAccent,
    codeGreen = CodeGreen,
    locked = LockedText
)

val lightAppColors = AppColors(
    background = LightBackground,
    surface = LightSurface,
    border = LightBorder,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    accent = LightAccent,
    codeGreen = LightCodeGreen,
    locked = LightLocked
)

/** Sumber warna dinamis untuk seluruh UI — nilai default dipakai hanya jika
 * suatu composable dirender di luar [PromptNotepadTheme] (seharusnya tidak pernah terjadi). */
val LocalAppColors = staticCompositionLocalOf { darkAppColors }

/** Ukuran font editor yang bisa diatur pengguna (Batch 2). Default 15sp — sama
 * persis dengan nilai tetap [PremiumEditorStyle] sebelumnya. */
val LocalEditorFontSize = compositionLocalOf { 15.sp }

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

val PremiumLightScheme = lightColorScheme(
    primary = LightTextPrimary,
    onPrimary = LightBackground,
    secondary = LightAccent,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextSecondary,
    surfaceVariant = LightSurface,
    outline = LightBorder
)

/** Dipertahankan apa adanya untuk kompatibilitas mundur (tidak ada pemanggil lain
 * yang ditemukan saat audit) — style editor yang sesungguhnya kini dibangun dinamis
 * lewat [editorTextStyle] di TextEditor.kt mengikuti LocalAppColors + LocalEditorFontSize. */
val PremiumEditorStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 15.sp,
    lineHeight = 22.sp,
    color = TextPrimary
)

/** Rasio lineHeight/fontSize asli (22sp / 15sp) dipertahankan persis saat ukuran
 * font berubah, supaya kelegaan baca antar baris tidak berubah proporsinya. */
fun editorTextStyle(fontSize: TextUnit, color: Color): TextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = fontSize,
    lineHeight = fontSize * (22f / 15f),
    color = color
)

@Composable
fun PromptNotepadTheme(
    darkTheme: Boolean = true,
    editorFontSize: TextUnit = 15.sp,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) darkAppColors else lightAppColors
    MaterialTheme(
        colorScheme = if (darkTheme) PremiumDarkScheme else PremiumLightScheme,
        content = {
            CompositionLocalProvider(
                LocalAppColors provides colors,
                LocalEditorFontSize provides editorFontSize
            ) {
                content()
            }
        }
    )
}
