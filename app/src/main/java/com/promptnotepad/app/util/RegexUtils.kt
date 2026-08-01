package com.promptnotepad.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed class RegexOutcome {
    data class Success(val text: String) : RegexOutcome()
    object TimedOut : RegexOutcome()
}

object RegexUtils {

    /** Dispatcher tersendiri (1 thread) agar pola regex yang "meledak" (catastrophic
     * backtracking) tidak ikut menyita thread pool Dispatchers.Default yang dipakai
     * bagian lain aplikasi. */
    private val regexDispatcher = Dispatchers.Default.limitedParallelism(1)
    private const val REGEX_TIMEOUT_MS = 2000L

    /**
     * Versi asinkron dari [findAndReplace] dengan batas waktu 2 detik.
     * Catatan: mesin regex JVM/Android tidak selalu responsif terhadap pembatalan
     * di tengah backtracking, sehingga timeout ini menghentikan *penantian* UI
     * (coroutine dibatalkan & TimedOut dikembalikan) meski thread pekerja pada
     * kasus terburuk masih berjalan di background pada dispatcher terisolasi —
     * bukan solusi sempurna, tapi UI dan fitur lain tidak akan ikut membeku.
     */
    suspend fun findAndReplaceAsync(
        content: String,
        pattern: String,
        replacement: String,
        ignoreCase: Boolean = false
    ): RegexOutcome = withContext(regexDispatcher) {
        val result = withTimeoutOrNull(REGEX_TIMEOUT_MS) {
            findAndReplace(content, pattern, replacement, ignoreCase)
        }
        if (result != null) RegexOutcome.Success(result) else RegexOutcome.TimedOut
    }

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
