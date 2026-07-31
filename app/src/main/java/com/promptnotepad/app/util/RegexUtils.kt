package com.promptnotepad.app.util

object RegexUtils {

    /**
     * Mencari dan mengganti teks berdasarkan pola regex.
     * Mengembalikan konten asli jika pola tidak valid, agar editor tidak crash
     * saat pengguna masih mengetik pola regex-nya.
     */
    fun findAndReplace(
        content: String,
        pattern: String,
        replacement: String,
        ignoreCase: Boolean = false
    ): String {
        if (pattern.isEmpty()) return content
        return try {
            val options = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
            val regex = Regex(pattern, options)
            content.replace(regex, replacement)
        } catch (e: Exception) {
            content
        }
    }

    /**
     * Mengembalikan daftar rentang (range) kemunculan pola, untuk keperluan highlight hasil pencarian.
     */
    fun findMatches(content: String, pattern: String, ignoreCase: Boolean = false): List<IntRange> {
        if (pattern.isEmpty()) return emptyList()
        return try {
            val options = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
            val regex = Regex(pattern, options)
            regex.findAll(content).map { it.range }.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
