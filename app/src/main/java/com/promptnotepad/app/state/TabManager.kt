package com.promptnotepad.app.state

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.promptnotepad.app.model.TabItem
import java.io.File

class TabManager {
    val openTabs = mutableStateListOf<TabItem>()
    var activeTabIndex = mutableStateOf(0)

    companion object {
        /** Batas jumlah tab terbuka sekaligus, agar alokasi RAM tidak membengkak. */
        const val MAX_OPEN_TABS = 12
    }

    /**
     * Tab yang baru saja tereviksi otomatis karena batas [MAX_OPEN_TABS] tercapai
     * (bukan karena diminta pengguna). Isi konten tetap aman karena sudah auto-save
     * ke disk; nilai ini hanya dipakai untuk menampilkan notifikasi ke pengguna.
     */
    var lastEvictedTabName = mutableStateOf<String?>(null)
        private set

    fun openFileInTab(file: File) {
        val existingIndex = openTabs.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (existingIndex != -1) {
            activeTabIndex.value = existingIndex
            return
        }

        if (openTabs.size >= MAX_OPEN_TABS) {
            val evictIndex = openTabs.indices.firstOrNull { it != activeTabIndex.value } ?: 0
            lastEvictedTabName.value = openTabs[evictIndex].title
            closeTab(evictIndex)
        }

        openTabs.add(TabItem(file = file))
        activeTabIndex.value = openTabs.lastIndex
    }

    fun closeTab(index: Int) {
        if (openTabs.isEmpty() || index !in openTabs.indices) return
        openTabs.removeAt(index)
        if (activeTabIndex.value >= openTabs.size) {
            activeTabIndex.value = (openTabs.size - 1).coerceAtLeast(0)
        } else if (activeTabIndex.value > index) {
            activeTabIndex.value = activeTabIndex.value - 1
        }
    }

    fun activeTab(): TabItem? {
        return openTabs.getOrNull(activeTabIndex.value)
    }

    /**
     * Memulihkan daftar tab yang sebelumnya terbuka (dipanggil saat aplikasi
     * dipulihkan setelah process death oleh OS), agar draf/tab yang sedang
     * dikerjakan pengguna tidak hilang begitu saja.
     */
    fun restoreTabs(files: List<File>, activeIndex: Int) {
        if (files.isEmpty()) return
        openTabs.clear()
        files.forEach { openTabs.add(TabItem(file = it)) }
        activeTabIndex.value = activeIndex.coerceIn(0, openTabs.lastIndex)
    }
}
