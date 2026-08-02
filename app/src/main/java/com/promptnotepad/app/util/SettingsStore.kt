package com.promptnotepad.app.util

import android.content.Context

/**
 * Persistensi pengaturan tampilan (Batch 2): ukuran font editor + mode tema.
 * Sengaja pakai SharedPreferences biasa (bukan DataStore/DB baru) — konsisten
 * dengan arsitektur proyek yang sejak awal menghindari dependensi penyimpanan
 * eksternal di luar `java.io.File` untuk isi catatan. Ini hanya menyimpan dua
 * nilai kecil non-konten, jadi SharedPreferences sinkron sudah cukup dan tidak
 * menambah kompleksitas/dependensi baru.
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getFontSizeSp(): Float =
        prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE_SP).coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP)

    fun setFontSizeSp(sizeSp: Float) {
        prefs.edit().putFloat(KEY_FONT_SIZE, sizeSp.coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP)).apply()
    }

    /** Default TETAP gelap sesuai kesepakatan user — hanya jadi true jika
     * pengguna secara eksplisit pernah memilih tema terang sebelumnya. */
    fun isDarkTheme(): Boolean = prefs.getBoolean(KEY_DARK_THEME, true)

    fun setDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, isDark).apply()
    }

    companion object {
        private const val PREFS_NAME = "promptnotepad_settings"
        private const val KEY_FONT_SIZE = "editor_font_size_sp"
        private const val KEY_DARK_THEME = "is_dark_theme"

        const val DEFAULT_FONT_SIZE_SP = 15f
        const val MIN_FONT_SIZE_SP = 11f
        const val MAX_FONT_SIZE_SP = 26f
        const val FONT_SIZE_STEP_SP = 1f
    }
}
