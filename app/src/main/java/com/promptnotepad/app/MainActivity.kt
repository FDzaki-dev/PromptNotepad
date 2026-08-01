package com.promptnotepad.app

import android.os.Bundle
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.promptnotepad.app.model.TabItem
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
import com.promptnotepad.app.util.FileUtils
import com.promptnotepad.app.util.RegexOutcome
import com.promptnotepad.app.util.RegexUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

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

/** Permintaan satu-kali agar regex diterapkan oleh [EditorSection] (event-as-state,
 * nonce dipakai supaya permintaan yang sama tidak diproses berulang). */
private data class RegexRequest(val nonce: Int, val pattern: String, val replacement: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptNotepadApp(notesDir: File) {
    val tabManager = rememberSaveable(saver = tabManagerSaver()) { TabManager() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = "Gagal menyimpan/membaca berkas. Perubahan terakhir mungkin belum tersimpan."

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

    var showFileList by remember { mutableStateOf(false) }
    var showRegexDialog by remember { mutableStateOf(false) }
    var previewMode by remember(tabManager.activeTabIndex.value) { mutableStateOf(false) }
    var pendingCloseIndex by remember { mutableStateOf<Int?>(null) }
    var regexRequest by remember { mutableStateOf<RegexRequest?>(null) }

    val evictedTabName = tabManager.lastEvictedTabName.value
    LaunchedEffect(evictedTabName) {
        if (evictedTabName != null) {
            snackbarHostState.showSnackbar("Tab \"$evictedTabName\" ditutup otomatis (batas ${TabManager.MAX_OPEN_TABS} tab). Isinya tetap tersimpan.")
        }
    }

    // Catatan optimasi recomposition: `activeTab`/`isMarkdownFile` di sini hanya
    // berubah saat tab dibuka/ditutup/berpindah (jarang) — BUKAN tiap keystroke.
    // Isi teks yang diedit (fieldValue) sengaja TIDAK disimpan di level ini,
    // melainkan didelegasikan ke EditorSection, supaya TopAppBar/Scaffold tidak
    // ikut recompose setiap kali pengguna mengetik atau menggerakkan kursor.
    val activeTab = tabManager.activeTab()
    val isMarkdownFile = activeTab?.file?.extension == "md"

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
                        Text(".*", color = PremiumAccent, modifier = Modifier.padding(horizontal = 8.dp))
                    }
                    IconButton(onClick = { showFileList = true }) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Buka File", tint = PremiumAccent)
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val result = FileUtils.createNewFile(notesDir, "Catatan")
                            result.onSuccess { newFile ->
                                tabManager.openFileInTab(newFile)
                            }.onFailure {
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
            EditorSection(
                tabManager = tabManager,
                activeTab = activeTab,
                isMarkdownFile = isMarkdownFile,
                previewMode = previewMode,
                coroutineScope = coroutineScope,
                snackbarHostState = snackbarHostState,
                errorMessage = errorMessage,
                regexRequest = regexRequest,
                onCloseTab = { index ->
                    val tab = tabManager.openTabs.getOrNull(index)
                    if (tab != null && tab.isDirty.value) {
                        pendingCloseIndex = index
                    } else {
                        tabManager.closeTab(index)
                    }
                }
            )
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
                regexRequest = RegexRequest((regexRequest?.nonce ?: 0) + 1, pattern, replacement)
                showRegexDialog = false
            }
        )
    }

    val closeIndex = pendingCloseIndex
    if (closeIndex != null) {
        AlertDialog(
            onDismissRequest = { pendingCloseIndex = null },
            title = { Text("Tutup tab ini?") },
            text = { Text("Ada perubahan yang belum tersimpan sepenuhnya. Menutup tab bisa berisiko kehilangan perubahan terakhir.") },
            confirmButton = {
                TextButton(onClick = {
                    tabManager.closeTab(closeIndex)
                    pendingCloseIndex = null
                }) { Text("Tutup Tetap") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCloseIndex = null }) { Text("Batal") }
            }
        )
    }
}

/**
 * Memegang state `fieldValue` (berubah tiap keystroke/kursor) secara TERISOLASI
 * dari composable induk (TopAppBar/Scaffold), sesuai rekomendasi optimasi
 * recomposition: hanya bagian ini yang recompose saat pengguna mengetik.
 */
@Composable
private fun EditorSection(
    tabManager: TabManager,
    activeTab: TabItem?,
    isMarkdownFile: Boolean,
    previewMode: Boolean,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    errorMessage: String,
    regexRequest: RegexRequest?,
    onCloseTab: (Int) -> Unit
) {
    var fieldValue by remember(activeTab?.id) { mutableStateOf(TextFieldValue("")) }

    // Muat ulang isi berkas setiap kali tab aktif berpindah (async, tidak memblokir UI).
    LaunchedEffect(activeTab?.id) {
        if (activeTab != null) {
            val result = FileUtils.readFile(activeTab.file)
            fieldValue = TextFieldValue(result.getOrDefault(""))
            if (result.isFailure) {
                snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: errorMessage)
            }
        }
    }

    fun saveActiveTab(content: String) {
        val tab = activeTab ?: return
        tab.isDirty.value = true
        coroutineScope.launch {
            val result = FileUtils.writeFile(tab.file, content)
            if (result.isSuccess) {
                tab.isDirty.value = false
            } else {
                snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: errorMessage)
            }
        }
    }

    // Bereaksi terhadap permintaan regex dari TopAppBar (dialog ada di composable induk,
    // eksekusinya di sini karena butuh akses ke fieldValue tanpa membocorkannya ke induk).
    LaunchedEffect(regexRequest) {
        val req = regexRequest ?: return@LaunchedEffect
        when (val outcome = RegexUtils.findAndReplaceAsync(fieldValue.text, req.pattern, req.replacement)) {
            is RegexOutcome.Success -> {
                fieldValue = TextFieldValue(outcome.text)
                saveActiveTab(outcome.text)
            }
            RegexOutcome.TimedOut -> {
                snackbarHostState.showSnackbar("Pola regex terlalu kompleks/lambat, dibatalkan agar aplikasi tidak macet.")
            }
        }
    }

    PremiumLayout(
        tabManager = tabManager,
        onCloseTab = onCloseTab,
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
        title = { Text("Cari & Ganti (Regex)") },
        text = {
            Column {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Pola Regex") }
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
