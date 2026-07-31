package com.promptnotepad.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegexUtilsTest {

    @Test
    fun `findAndReplace mengganti semua kemunculan pola`() {
        val result = RegexUtils.findAndReplace("kucing dan kucing", "kucing", "anjing")
        assertEquals("anjing dan anjing", result)
    }

    @Test
    fun `findAndReplace pola invalid mengembalikan teks asli tanpa crash`() {
        val original = "teks apa adanya"
        val result = RegexUtils.findAndReplace(original, "(((", "x")
        assertEquals(original, result)
    }

    @Test
    fun `findAndReplace pattern kosong mengembalikan teks asli`() {
        val original = "tidak berubah"
        val result = RegexUtils.findAndReplace(original, "", "x")
        assertEquals(original, result)
    }

    @Test
    fun `findAndReplace ignoreCase bekerja sesuai flag`() {
        val result = RegexUtils.findAndReplace("Kucing", "kucing", "anjing", ignoreCase = true)
        assertEquals("anjing", result)
    }

    @Test
    fun `findMatches mengembalikan seluruh rentang kecocokan`() {
        val matches = RegexUtils.findMatches("abcabc", "abc")
        assertEquals(2, matches.size)
    }

    @Test
    fun `findMatches pola invalid mengembalikan list kosong tanpa crash`() {
        val matches = RegexUtils.findMatches("teks", "(((")
        assertTrue(matches.isEmpty())
    }
}
