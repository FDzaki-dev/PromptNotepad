package com.promptnotepad.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoParserTest {

    @Test
    fun `baris biasa tanpa metadata dikenali sebagai belum selesai`() {
        val task = TodoParser.parseLine("Beli susu")
        assertFalse(task.isCompleted)
        assertNull(task.priority)
        assertNull(task.context)
        assertNull(task.project)
    }

    @Test
    fun `baris diawali x dikenali sebagai selesai`() {
        val task = TodoParser.parseLine("x Beli susu")
        assertTrue(task.isCompleted)
    }

    @Test
    fun `prioritas huruf besar dalam kurung terbaca`() {
        val task = TodoParser.parseLine("(A) Kirim laporan")
        assertEquals('A', task.priority)
    }

    @Test
    fun `context diawali arobase terbaca`() {
        val task = TodoParser.parseLine("Telepon klien @kantor")
        assertEquals("kantor", task.context)
    }

    @Test
    fun `project diawali plus terbaca`() {
        val task = TodoParser.parseLine("Review PR +PromptNotepad")
        assertEquals("PromptNotepad", task.project)
    }

    @Test
    fun `kombinasi prioritas context dan project terbaca sekaligus`() {
        val task = TodoParser.parseLine("(B) Rapat @kantor +ProjekX")
        assertEquals('B', task.priority)
        assertEquals("kantor", task.context)
        assertEquals("ProjekX", task.project)
    }

    @Test
    fun `parseAll mengabaikan baris kosong`() {
        val tasks = TodoParser.parseAll("Tugas 1\n\nTugas 2\n   \nTugas 3")
        assertEquals(3, tasks.size)
    }

    @Test
    fun `isTodoLine true jika ada salah satu metadata`() {
        assertTrue(TodoParser.isTodoLine("@kantor beres-beres"))
        assertTrue(TodoParser.isTodoLine("(C) urgent"))
        assertTrue(TodoParser.isTodoLine("+ProjekX progres"))
    }

    @Test
    fun `isTodoLine false jika baris polos`() {
        assertFalse(TodoParser.isTodoLine("catatan biasa tanpa metadata"))
    }
}
