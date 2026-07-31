package com.promptnotepad.app.state

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.promptnotepad.app.model.TabItem
import java.io.File

private const val TAG_STATE = "PN_STATE"

class TabManager {
    val openTabs = mutableStateListOf<TabItem>()
    var activeTabIndex = mutableStateOf(0)

    fun openFileInTab(file: File) {
        val existingIndex = openTabs.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (existingIndex != -1) {
            activeTabIndex.value = existingIndex
        } else {
            openTabs.add(TabItem(file = file))
            activeTabIndex.value = openTabs.lastIndex
        }
        Log.d(TAG_STATE, "openFileInTab: ${file.name}, activeIndex=${activeTabIndex.value}, totalTab=${openTabs.size}")
    }

    fun closeTab(index: Int) {
        if (openTabs.isEmpty() || index !in openTabs.indices) return
        val closedName = openTabs[index].title
        openTabs.removeAt(index)
        if (activeTabIndex.value >= openTabs.size) {
            activeTabIndex.value = (openTabs.size - 1).coerceAtLeast(0)
        } else if (activeTabIndex.value > index) {
            activeTabIndex.value = activeTabIndex.value - 1
        }
        Log.d(TAG_STATE, "closeTab: $closedName, sisaTab=${openTabs.size}")
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
        Log.d(TAG_STATE, "restoreTabs: ${files.size} tab dipulihkan, activeIndex=${activeTabIndex.value}")
    }
}
