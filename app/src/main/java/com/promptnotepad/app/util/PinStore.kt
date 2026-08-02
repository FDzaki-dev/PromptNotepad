package com.promptnotepad.app.util

import android.content.Context

/**
 * Persistensi status "Pin" per berkas (Batch A — redesain ala TxtPad+: layar
 * utama jadi Daftar File, berkas yang di-pin selalu tampil di bagian atas).
 * Disimpan sebagai kumpulan absolute path di SharedPreferences — pola yang
 * sama seperti [SettingsStore], bukan DB baru, karena hanya perlu menyimpan
 * kumpulan string kecil.
 */
class PinStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isPinned(absolutePath: String): Boolean =
        prefs.getStringSet(KEY_PINNED, emptySet())?.contains(absolutePath) == true

    fun togglePin(absolutePath: String) {
        val current = prefs.getStringSet(KEY_PINNED, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (!current.remove(absolutePath)) {
            current.add(absolutePath)
        }
        prefs.edit().putStringSet(KEY_PINNED, current).apply()
    }

    /** Dipanggil saat berkas dihapus/di-rename agar entri pin tidak jadi sampah. */
    fun clearPin(absolutePath: String) {
        val current = prefs.getStringSet(KEY_PINNED, emptySet())?.toMutableSet() ?: return
        if (current.remove(absolutePath)) {
            prefs.edit().putStringSet(KEY_PINNED, current).apply()
        }
    }

    fun pinnedPaths(): Set<String> = prefs.getStringSet(KEY_PINNED, emptySet()) ?: emptySet()

    companion object {
        private const val PREFS_NAME = "promptnotepad_pins"
        private const val KEY_PINNED = "pinned_paths"
    }
}
