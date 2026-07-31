package com.promptnotepad.app

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.promptnotepad.app.state.TabManager
import com.promptnotepad.app.ui.MarkdownViewer
import com.promptnotepad.app.ui.PremiumLayout
import com.promptnotepad.app.ui.ShortcutBar
import com.promptnotepad.app.ui.TextEditor
import com.promptnotepad.app.ui.insertAtCursor
import com.promptnotepad.app.ui.theme.DeepGray
import com.promptnotepad.app.ui.theme.PremiumAccent
import com.promptnotepad.app.ui.theme.PromptNotepadTheme
import com.promptnotepad.app.ui.theme.PureBlack
import com.promptnotepad.app.ui.theme.TextPrimary
import com.promptnotepad.app.ui.theme.TextSecondary
import com.promptnotepad.app.util.FileUtils
import com.promptnotepad.app.util.RegexUtils
import kotlinx.coroutines.launch
import java.io.File

private const val TAG_UI = "PN_UI"
private const val PREFS_NAME = "prompt_notepad_prefs"
private const val KEY_ONBOARDING_SHOWN = "onboarding_shown"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val notesDir = File(filesDir, "notes")
        if (!notesDir.exists()) notesDir.mkdirs()

        setContent {
            PromptNotepadTheme {
                PromptNotepadApp(notesDir = notesDir)
            }
        }
    }
}

/**
 * Menyimpan daftar tab (path absolut file) + index tab aktif ke dalam Bundle
 * saved-instance-state, sehingga jika proses aplikasi di-kill oleh OS (bukan
 * hanya rotasi layar), tab yang sedang dibuka pengguna bisa dipulihkan.
 */
private fun tabManagerSaver(): Saver<TabManager, List<String>> = Saver(
    save = { manager ->
        manager.openTabs.map { it.file.absolutePath } + manager.activeTabIndex.value.toString()
    },
    restore = { saved ->
        val manager = TabManager()
        if (saved.isNotEmpty()) {
            val activeIndex = saved.last().toIntOrNull() ?: 0
            val files = saved.dropLast(1).map { File(it) }.filter { it.exists() }
            manager.restoreTabs(files, activeIndex)
        }
        manager
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptNotepadApp(notesDir: File) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val tabManager = rememberSaveable(saver = tabManagerSaver()) { TabManager() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = "Gagal menyimpan/membaca berkas. Perubahan terakhir mungkin belum tersimpan."
    var showOnboarding by remember { mutableStateOf(!prefs.getBoolean(KEY_ONBOARDING_SHOWN, false)) }

    // QuickNote: tab coretan instan yang otomatis terbuka saat aplikasi dijalankan
    // (hanya jika belum ada tab yang dipulihkan dari saved-instance-state).
    LaunchedEffect(Unit) {
        if (tabManager.openTabs.isEmpty()) {
            val quickNoteFile = File(notesDir, "QuickNote.txt")
            if (!quickNoteFile.exists()) {
                val created = FileUtils.writeFile(quickNoteFile, "")
                if (created.isFailure) {
                    snackbarHostState.showSnackbar(errorMessage)
                }
            }
            tabManager.openFileInTab(quickNoteFile)
        }
    }

    var fieldValue by remember(tabManager.activeTabIndex.value) { mutableStateOf(TextFieldValue("")) }

    // Muat ulang isi berkas setiap kali tab aktif berpindah (async, tidak memblokir UI).
    LaunchedEffect(tabManager.activeTabIndex.value, tabManager.openTabs.size) {
        val tab = tabManager.activeTab()
        if (tab != null) {
            val result = FileUtils.readFile(tab.file)
            fieldValue = TextFieldValue(result.getOrDefault(""))
            if (result.isFailure) {
                snackbarHostState.showSnackbar(errorMessage)
            }
        }
    }

    var showFileList by remember { mutableStateOf(false) }
    var showRegexDialog by remember { mutableStateOf(false) }
    var previewMode by remember(tabManager.activeTabIndex.value) { mutableStateOf(false) }

    val activeTab = tabManager.activeTab()
    val isMarkdownFile = activeTab?.file?.extension == "md"

    fun saveActiveTab(content: String) {
        val tab = activeTab ?: return
        coroutineScope.launch {
            val result = FileUtils.writeFile(tab.file, content)
            if (result.isFailure) {
                Log.e(TAG_UI, "saveActiveTab gagal untuk ${tab.file.name}")
                snackbarHostState.showSnackbar(errorMessage)
            }
        }
    }

    // Menutup tab tidak menghapus berkas di penyimpanan (hanya menutup tampilannya),
    // tapi tetap menyediakan tombol "Buka lagi" agar pengguna awam tidak khawatir
    // salah pencet dan merasa kehilangan catatannya.
    fun closeTabWithUndo(index: Int) {
        val closedFile = tabManager.openTabs.getOrNull(index)?.file
        tabManager.closeTab(index)
        if (closedFile != null) {
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Tab \"${closedFile.name}\" ditutup",
                    actionLabel = "Buka lagi",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    tabManager.openFileInTab(closedFile)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PromptNotepad", color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepGray),
                actions = {
                    if (isMarkdownFile) {
                        IconButton(onClick = { previewMode = !previewMode }) {
                            Icon(Icons.Filled.Visibility, contentDescription = "Pratinjau Markdown", tint = PremiumAccent)
                        }
                    }
                    IconButton(onClick = { showRegexDialog = true }) {
                        Icon(Icons.Filled.FindReplace, contentDescription = "Cari & Ganti", tint = PremiumAccent)
                    }
                    IconButton(onClick = { showFileList = true }) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Buka File", tint = PremiumAccent)
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val result = FileUtils.createNewFile(notesDir, "Catatan")
                            result.onSuccess { newFile ->
                                Log.d(TAG_UI, "File baru dibuat: ${newFile.name}")
                                tabManager.openFileInTab(newFile)
                            }.onFailure {
                                Log.e(TAG_UI, "Gagal membuat file baru")
                                snackbarHostState.showSnackbar(errorMessage)
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "File Baru", tint = PremiumAccent)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = PureBlack
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            PremiumLayout(
                tabManager = tabManager,
                onCloseTab = { index -> closeTabWithUndo(index) },
                shortcutBar = {
                    if (!previewMode) {
                        ShortcutBar(onInsertText = { insertText ->
                            fieldValue = insertAtCursor(fieldValue, insertText)
                            saveActiveTab(fieldValue.text)
                        })
                    }
                }
            ) {
                if (previewMode && isMarkdownFile) {
                    MarkdownViewer(content = fieldValue.text)
                } else {
                    TextEditor(
                        value = fieldValue,
                        onValueChange = { fieldValue = it },
                        onContentChange = { newContent -> saveActiveTab(newContent) }
                    )
                }
            }
        }
    }

    if (showFileList) {
        FileListDialog(
            notesDir = notesDir,
            onDismiss = { showFileList = false },
            onFileSelected = { file ->
                tabManager.openFileInTab(file)
                showFileList = false
            }
        )
    }

    if (showRegexDialog) {
        RegexReplaceDialog(
            onDismiss = { showRegexDialog = false },
            onApply = { pattern, replacement ->
                coroutineScope.launch {
                    val newText = RegexUtils.findAndReplaceAsync(fieldValue.text, pattern, replacement)
                    fieldValue = TextFieldValue(newText)
                    saveActiveTab(newText)
                }
                showRegexDialog = false
            }
        )
    }

    if (showOnboarding) {
        OnboardingDialog(
            onDismiss = {
                showOnboarding = false
                prefs.edit().putBoolean(KEY_ONBOARDING_SHOWN, true).apply()
            }
        )
    }
}

@Composable
private fun FileListDialog(
    notesDir: File,
    onDismiss: () -> Unit,
    onFileSelected: (File) -> Unit
) {
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    LaunchedEffect(Unit) {
        files = FileUtils.listTextFiles(notesDir).getOrDefault(emptyList())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
        title = { Text("Berkas Tersimpan") },
        text = {
            LazyColumn {
                items(files) { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        TextButton(onClick = { onFileSelected(file) }) {
                            Text(file.name)
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun RegexReplaceDialog(
    onDismiss: () -> Unit,
    onApply: (pattern: String, replacement: String) -> Unit
) {
    var pattern by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cari & Ganti") },
        text = {
            Column {
                Text(
                    text = "Ganti semua kemunculan sebuah kata dengan kata lain di catatan ini. " +
                        "Contoh: isi \"kucing\" di kolom pertama, \"anjing\" di kolom kedua, " +
                        "maka semua kata \"kucing\" akan berubah jadi \"anjing\".",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Cari") }
                )
                OutlinedTextField(
                    value = replacement,
                    onValueChange = { replacement = it },
                    label = { Text("Ganti dengan") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(pattern, replacement) }) { Text("Terapkan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

/**
 * Dialog perkenalan singkat yang hanya muncul sekali di percobaan pertama
 * (status disimpan di SharedPreferences), menjelaskan 3 aksi utama dengan
 * bahasa sederhana untuk pengguna awam.
 */
@Composable
private fun OnboardingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selamat datang di PromptNotepad") },
        text = {
            Column {
                Text(
                    "Aplikasi catatan sederhana yang tersimpan langsung di HP kamu, tanpa internet.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OnboardingRow(icon = Icons.Filled.Add, text = "Buat catatan baru")
                OnboardingRow(icon = Icons.Filled.FolderOpen, text = "Buka catatan yang sudah tersimpan")
                OnboardingRow(icon = Icons.Filled.FindReplace, text = "Cari & ganti kata dalam catatan")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Mengerti") }
        }
    )
}

@Composable
private fun OnboardingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Icon(icon, contentDescription = null, tint = PremiumAccent)
        Text(text, color = TextPrimary, modifier = Modifier.padding(start = 12.dp))
    }
}
