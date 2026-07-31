package com.promptnotepad.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Semua operasi berkas berjalan di luar UI thread (Dispatchers.IO) dan dibungkus
 * runCatching, agar file besar/izin dicabut/memori penuh tidak menyebabkan ANR
 * atau force-close. Penulisan memakai dispatcher IO dengan paralelisme 1 agar
 * urutan auto-save per keystroke tetap terjamin (tidak ada race condition
 * antar-write yang bisa membuat versi lama menimpa versi baru).
 */
object FileUtils {

    private val writeDispatcher = Dispatchers.IO.limitedParallelism(1)

    suspend fun readFile(file: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (file.exists()) file.readText() else ""
        }
    }

    suspend fun writeFile(file: File, content: String): Result<Unit> = withContext(writeDispatcher) {
        runCatching {
            file.parentFile?.let { parent ->
                if (!parent.exists()) parent.mkdirs()
            }
            file.writeText(content)
        }
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
