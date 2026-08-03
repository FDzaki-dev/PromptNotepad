package com.promptnotepad.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.zip.ZipFile

/**
 * Membaca isi arsip ZIP yang dibuka lewat "Buka Dengan" (mis. dari file manager),
 * KHUSUS untuk melihat & mengimpor berkas teks (.txt/.md) di dalamnya — bukan
 * pengelola arsip umum (tidak ada UI ekstrak-semua/kompresi/hapus-entri).
 *
 * Deteksi encoding (beda dari [FileUtils.readFile] yang hard-code UTF-8): banyak
 * berkas .txt dari sumber lain (mis. Windows Notepad legacy) sebenarnya
 * ANSI/Windows-1252, bukan UTF-8 — kalau dipaksa dekode sebagai UTF-8, karakter
 * non-ASCII tampil sebagai simbol acak (mojibake). [decodeText] mendeteksi BOM
 * dulu, lalu coba UTF-8 ketat, baru fallback ke Windows-1252.
 */
object ZipUtils {

    private val writeDispatcher = Dispatchers.IO.limitedParallelism(1)

    /** Batas ukuran entri yang dibaca (2MB), konsisten dengan [FileUtils] untuk berkas biasa. */
    private const val MAX_ENTRY_SIZE_BYTES = 2L * 1024 * 1024

    data class ZipEntryInfo(
        val name: String,
        val sizeBytes: Long
    )

    private fun isTextLikeName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".txt") || lower.endsWith(".md")
    }

    suspend fun listTextEntries(zipFile: File): Result<List<ZipEntryInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                ZipFile(zipFile).use { zip ->
                    val result = mutableListOf<ZipEntryInfo>()
                    val enumeration = zip.entries()
                    while (enumeration.hasMoreElements()) {
                        val entry = enumeration.nextElement()
                        if (!entry.isDirectory && isTextLikeName(entry.name)) {
                            result.add(ZipEntryInfo(name = entry.name, sizeBytes = entry.size))
                        }
                    }
                    result.sortedBy { it.name.lowercase() }
                }
            }
        }

    suspend fun readEntryText(zipFile: File, entryName: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                ZipFile(zipFile).use { zip ->
                    val entry = zip.getEntry(entryName)
                        ?: throw IOException("Berkas \"$entryName\" tidak ditemukan di dalam ZIP (mungkin sudah berubah).")
                    if (entry.size > MAX_ENTRY_SIZE_BYTES) {
                        throw IOException(
                            "Berkas \"$entryName\" berukuran ${entry.size / 1024} KB, melebihi batas 2 MB yang aman dibuka."
                        )
                    }
                    val bytes = zip.getInputStream(entry).use { it.readBytes() }
                    if (bytes.size > MAX_ENTRY_SIZE_BYTES) {
                        throw IOException(
                            "Berkas \"$entryName\" berukuran ${bytes.size / 1024} KB, melebihi batas 2 MB yang aman dibuka."
                        )
                    }
                    normalizeNewlines(decodeText(bytes))
                }
            }
        }

    /**
     * Mengimpor satu entri teks dari ZIP ke penyimpanan lokal `notesDir`, lalu
     * bisa dibuka sebagai tab biasa lewat jalur lokal yang sama persis dengan
     * berkas lain (auto-save, undo/redo, dst. — tidak ada perubahan di jalur itu).
     * Nama lokal deterministik dari hash (nama ZIP + nama entri), pola sama
     * seperti [ExternalFileUtils.localNameFor], supaya membuka entri yang sama
     * berulang kali ter-mapping ke satu salinan, bukan duplikat.
     */
    suspend fun importEntryToNotes(zipFile: File, entryName: String, notesDir: File): Result<File> =
        withContext(writeDispatcher) {
            runCatching {
                val text = readEntryText(zipFile, entryName).getOrThrow()
                val hash = "${zipFile.name}:$entryName".hashCode().toUInt().toString(16).take(8)
                val safeBaseName = entryName.substringAfterLast('/').ifBlank { "Impor.txt" }
                if (!notesDir.exists()) notesDir.mkdirs()
                val localFile = File(notesDir, "zip-$hash-$safeBaseName")
                localFile.writeText(text, Charsets.UTF_8)
                localFile
            }
        }

    private fun normalizeNewlines(text: String): String =
        text.replace("\r\n", "\n").replace("\r", "\n")

    /**
     * Deteksi & dekode teks dari byte mentah tanpa memaksa UTF-8:
     * 1. BOM UTF-8 (EF BB BF) atau UTF-16 (FE FF / FF FE) -> dekode sesuai BOM.
     * 2. Tanpa BOM: coba UTF-8 ketat (CodingErrorAction.REPORT, bukan diam-diam
     *    mengganti byte tak valid dengan karakter pengganti) — kalau seluruh byte
     *    valid UTF-8, besar kemungkinan memang UTF-8.
     * 3. Kalau gagal: fallback ke Windows-1252 — superset ISO-8859-1 yang
     *    mencakup hampir semua Latin/Eropa Barat, dan setiap byte 0x00-0xFF
     *    selalu valid didekode olehnya (tidak pernah gagal), jadi dipakai
     *    sebagai jaring pengaman terakhir yang lebih masuk akal daripada
     *    memaksa UTF-8 dan menampilkan simbol acak.
     */
    private fun decodeText(bytes: ByteArray): String {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        val strictDecoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            strictDecoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: CharacterCodingException) {
            String(bytes, Charset.forName("windows-1252"))
        }
    }
}
