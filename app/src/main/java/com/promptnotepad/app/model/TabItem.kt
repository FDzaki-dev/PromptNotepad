package com.promptnotepad.app.model

import androidx.compose.runtime.mutableStateOf
import java.io.File
import java.util.UUID

data class TabItem(
    val id: String = UUID.randomUUID().toString(),
    val file: File,
    val title: String = file.name
) {
    /**
     * Menandai apakah ada perubahan yang belum berhasil tersimpan ke disk.
     * Bukan bagian dari primary constructor agar tidak memengaruhi equals/hashCode/copy.
     */
    val isDirty = mutableStateOf(false)
}
