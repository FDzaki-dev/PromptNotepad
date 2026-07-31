package com.promptnotepad.app.util

import java.io.File

object FileUtils {

    fun readFile(file: File): String {
        return try {
            if (file.exists()) file.readText() else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun writeFile(file: File, content: String) {
        try {
            file.parentFile?.let { parent ->
                if (!parent.exists()) parent.mkdirs()
            }
            file.writeText(content)
        } catch (e: Exception) {
            // Simpan gagal secara diam-diam; UI tetap menampilkan state terakhir.
        }
    }

    fun listTextFiles(dir: File): List<File> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles { f ->
            f.isFile && (f.extension == "txt" || f.extension == "md")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun createNewFile(dir: File, baseName: String, extension: String = "txt"): File {
        if (!dir.exists()) dir.mkdirs()
        var candidate = File(dir, "$baseName.$extension")
        var counter = 1
        while (candidate.exists()) {
            candidate = File(dir, "$baseName-$counter.$extension")
            counter++
        }
        candidate.createNewFile()
        return candidate
    }
}
