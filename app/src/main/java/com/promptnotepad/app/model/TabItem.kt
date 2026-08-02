package com.promptnotepad.app.model

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import java.io.File
import java.util.UUID

data class TabItem(
    val id: String = UUID.randomUUID().toString(),
    val file: File,
    val title: String = file.name,
    /** Uri asal jika tab ini dibuka lewat "Buka Dengan" dari aplikasi lain (null untuk
     * tab lokal biasa). Dipakai untuk sinkron-balik konten setiap auto-save. */
    val sourceUri: Uri? = null
) {
    /**
     * Menandai apakah ada perubahan yang belum berhasil tersimpan ke disk.
     * Bukan bagian dari primary constructor agar tidak memengaruhi equals/hashCode/copy.
     */
    val isDirty = mutableStateOf(false)
}
