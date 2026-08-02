package com.promptnotepad.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Menangani berkas eksternal yang dibuka lewat "Buka Dengan" (Intent ACTION_VIEW/
 * ACTION_EDIT dari file manager atau aplikasi lain). Sengaja TIDAK mengganti
 * arsitektur penyimpanan yang sudah ada (java.io.File di filesDir) — berkas
 * eksternal disalin ke penyimpanan internal lalu dibuka sebagai tab biasa,
 * kemudian perubahan disinkronkan balik ke Uri asal setiap auto-save.
 */
object ExternalFileUtils {

    private fun displayNameFor(context: Context, uri: Uri): String {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            // Provider tidak mendukung query metadata — pakai fallback di bawah.
        }
        return name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Impor.txt"
    }

    /** Nama file lokal deterministik dari Uri, agar membuka berkas eksternal yang sama
     * berulang kali ter-mapping ke satu salinan lokal yang sama (bukan duplikat tiap kali).
     *
     * 📝 [Ditinjau saat audit v1.4.2, TIDAK diubah] Hash 32-bit (`String.hashCode()`) di
     * bawah ini secara teori berisiko tabrakan antar-Uri berbeda, tapi mengganti algoritma
     * hash akan membuat berkas yang SUDAH diimpor sebelumnya (nama lokal format lama) tidak
     * lagi ter-mapping ke salinan lokalnya sendiri saat dibuka ulang — menghasilkan duplikat
     * nyata bagi pengguna lama. Untuk risiko tabrakan 32-bit yang di praktiknya (jumlah
     * berkas yang diimpor satu pengguna) nyaris tidak pernah terjadi, regresi itu tidak
     * sepadan. Dibiarkan seperti semula — dicatat sebagai keterbatasan yang diterima. */
    private fun localNameFor(uri: Uri, originalName: String): String {
        val hash = uri.toString().hashCode().toUInt().toString(16).take(8)
        val safeName = originalName.ifBlank { "Impor.txt" }
        return "ext-$hash-$safeName"
    }

    suspend fun importFromUri(context: Context, uri: Uri, notesDir: File): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val originalName = displayNameFor(context, uri)
                val localName = localNameFor(uri, originalName)
                val localFile = File(notesDir, localName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    localFile.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IOException("Tidak bisa membaca berkas eksternal (izin akses ditolak atau berkas terhapus).")
                localFile
            }
        }

    /**
     * Tulis balik konten ke Uri eksternal asal. Kegagalan di sini TIDAK berisiko
     * kehilangan data karena salinan lokal sudah tersimpan lebih dulu lewat FileUtils
     * sebelum fungsi ini dipanggil.
     */
    suspend fun writeBackToUri(context: Context, uri: Uri, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                    output.write(content.toByteArray(Charsets.UTF_8))
                } ?: throw IOException("Tidak bisa menulis balik ke berkas asal (izin sudah dicabut).")
            }
        }
}
