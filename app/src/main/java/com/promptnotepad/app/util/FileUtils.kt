package com.promptnotepad.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.charset.Charset

/**
 * Semua operasi berkas berjalan di luar UI thread (Dispatchers.IO) dan dibungkus
 * runCatching, agar file besar/izin dicabut/memori penuh tidak menyebabkan ANR
 * atau force-close. Penulisan memakai dispatcher IO dengan paralelisme 1 agar
 * urutan auto-save per keystroke tetap terjamin (tidak ada race condition
 * antar-write yang bisa membuat versi lama menimpa versi baru).
 */
object FileUtils {

    private val writeDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val charset = Charset.forName("UTF-8")

    /** Batas ukuran file yang boleh dibuka di editor (2MB) — di atas ini berisiko OOM/UI freeze
     * karena BasicTextField menyimpan seluruh konten sebagai satu TextFieldValue di memori. */
    private const val MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024

    suspend fun readFile(file: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) {
                ""
            } else if (file.length() > MAX_FILE_SIZE_BYTES) {
                throw IOException(
                    "Berkas berukuran ${file.length() / 1024} KB, melebihi batas 2 MB yang aman dibuka di editor ini."
                )
            } else {
                normalizeNewlines(file.readText(charset))
            }
        }
    }

    suspend fun writeFile(file: File, content: String): Result<Unit> = withContext(writeDispatcher) {
        runCatching {
            if (file.exists() && !file.canWrite()) {
                throw IOException("Berkas bersifat read-only atau sedang dikunci sistem.")
            }
            file.parentFile?.let { parent ->
                if (!parent.exists()) parent.mkdirs()
            }
            file.writeText(content, charset)
        }
    }

    /** Menyeragamkan akhir baris (CRLF/CR -> LF) agar tampilan & regex konsisten lintas sumber file. */
    private fun normalizeNewlines(text: String): String {
        return text.replace("\r\n", "\n").replace("\r", "\n")
    }

    suspend fun listTextFiles(dir: File): Result<List<File>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!dir.exists() || !dir.isDirectory) {
                emptyList()
            } else {
                dir.listFiles { f ->
                    f.isFile && (f.extension == "txt" || f.extension == "md")
                }?.sortedByDescending { it.lastModified() } ?: emptyList()
            }
        }
    }

    suspend fun createNewFile(dir: File, baseName: String, extension: String = "txt"): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!dir.exists()) dir.mkdirs()
                var candidate = File(dir, "$baseName.$extension")
                var counter = 1
                while (candidate.exists()) {
                    candidate = File(dir, "$baseName-$counter.$extension")
                    counter++
                }
                candidate.createNewFile()
                candidate
            }
        }
}
