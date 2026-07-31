package com.promptnotepad.app.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Semua operasi berkas berjalan di luar UI thread (Dispatchers.IO) dan dibungkus
 * runCatching, agar file besar/izin dicabut/memori penuh tidak menyebabkan ANR
 * atau force-close. Penulisan memakai dispatcher IO dengan paralelisme 1 agar
 * urutan auto-save per keystroke tetap terjamin (tidak ada race condition
 * antar-write yang bisa membuat versi lama menimpa versi baru).
 *
 * Semua kegagalan dicatat dengan tag Logcat [TAG_IO] agar saat troubleshooting
 * cukup filter "PN_IO" untuk langsung tahu error terjadi di layer I/O.
 */
object FileUtils {

    private const val TAG_IO = "PN_IO"
    private val writeDispatcher = Dispatchers.IO.limitedParallelism(1)

    suspend fun readFile(file: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (file.exists()) file.readText() else ""
        }.onFailure { Log.e(TAG_IO, "readFile gagal: ${file.name}", it) }
    }

    suspend fun writeFile(file: File, content: String): Result<Unit> = withContext(writeDispatcher) {
        runCatching {
            file.parentFile?.let { parent ->
                if (!parent.exists()) parent.mkdirs()
            }
            file.writeText(content)
        }.onFailure { Log.e(TAG_IO, "writeFile gagal: ${file.name}", it) }
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
        }.onFailure { Log.e(TAG_IO, "listTextFiles gagal: ${dir.path}", it) }
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
            }.onFailure { Log.e(TAG_IO, "createNewFile gagal: $baseName", it) }
        }
}
