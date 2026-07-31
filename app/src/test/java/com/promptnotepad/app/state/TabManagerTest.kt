package com.promptnotepad.app.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TabManagerTest {

    @Test
    fun `openFileInTab menambah tab baru dan mengaktifkannya`() {
        val manager = TabManager()
        manager.openFileInTab(File("/notes/A.txt"))
        assertEquals(1, manager.openTabs.size)
        assertEquals(0, manager.activeTabIndex.value)
    }

    @Test
    fun `openFileInTab file yang sama tidak membuat tab duplikat`() {
        val manager = TabManager()
        val file = File("/notes/A.txt")
        manager.openFileInTab(file)
        manager.openFileInTab(File("/notes/B.txt"))
        manager.openFileInTab(file)
        assertEquals(2, manager.openTabs.size)
        assertEquals(0, manager.activeTabIndex.value)
    }

    @Test
    fun `closeTab mengurangi jumlah tab dan menyesuaikan index aktif`() {
        val manager = TabManager()
        manager.openFileInTab(File("/notes/A.txt"))
        manager.openFileInTab(File("/notes/B.txt"))
        manager.openFileInTab(File("/notes/C.txt"))
        manager.activeTabIndex.value = 2

        manager.closeTab(2)

        assertEquals(2, manager.openTabs.size)
        assertEquals(1, manager.activeTabIndex.value)
    }

    @Test
    fun `closeTab index tidak valid diabaikan tanpa crash`() {
        val manager = TabManager()
        manager.openFileInTab(File("/notes/A.txt"))
        manager.closeTab(99)
        assertEquals(1, manager.openTabs.size)
    }

    @Test
    fun `closeTab tab terakhir menghasilkan activeTabIndex 0`() {
        val manager = TabManager()
        manager.openFileInTab(File("/notes/A.txt"))
        manager.closeTab(0)
        assertTrue(manager.openTabs.isEmpty())
        assertEquals(0, manager.activeTabIndex.value)
    }

    @Test
    fun `restoreTabs memulihkan daftar tab dan index aktif`() {
        val manager = TabManager()
        val files = listOf(File("/notes/A.txt"), File("/notes/B.txt"))
        manager.restoreTabs(files, 1)
        assertEquals(2, manager.openTabs.size)
        assertEquals(1, manager.activeTabIndex.value)
    }

    @Test
    fun `restoreTabs dengan list kosong tidak mengubah state`() {
        val manager = TabManager()
        manager.openFileInTab(File("/notes/A.txt"))
        manager.restoreTabs(emptyList(), 5)
        assertEquals(1, manager.openTabs.size)
    }

    @Test
    fun `activeTab mengembalikan null jika tidak ada tab`() {
        val manager = TabManager()
        assertEquals(null, manager.activeTab())
    }
}
