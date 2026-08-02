package com.promptnotepad.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.promptnotepad.app.model.TabItem
import com.promptnotepad.app.state.TabManager
import com.promptnotepad.app.ui.BottomFileBar
import com.promptnotepad.app.ui.FileListScreen
import com.promptnotepad.app.ui.MarkdownViewer
import com.promptnotepad.app.ui.OverflowMenuItem
import com.promptnotepad.app.ui.PremiumLayout
import com.promptnotepad.app.ui.ShortcutBar
import com.promptnotepad.app.ui.TextEditor
import com.promptnotepad.app.ui.insertAtCursor
import com.promptnotepad.app.ui.theme.LocalAppColors
import com.promptnotepad.app.ui.theme.PromptNotepadTheme
import com.promptnotepad.app.util.ExternalFileUtils
import com.promptnotepad.app.util.FileUtils
import com.promptnotepad.app.util.PinStore
import com.promptnotepad.app.util.RegexOutcome
import com.promptnotepad.app.util.RegexUtils
import com.promptnotepad.app.util.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.io.File

class MainActivity : ComponentActivity() {

    /** Menyimpan Intent yang sedang aktif sebagai Compose State, agar Intent baru yang
     * masuk lewat onNewIntent (mis. tap file lain saat app sudah berjalan — didukung
     * berkat launchMode="singleTop") bisa diteruskan ke Composition yang sudah berjalan. */
    private val currentIntentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val notesDir = File(filesDir, "notes")
        if (!notesDir.exists()) notesDir.mkdirs()

        currentIntentState.value = intent

        setContent {
            val settingsStore = remember { SettingsStore(applicationContext) }
            var isDarkTheme by remember { mutableStateOf(settingsStore.isDarkTheme()) }
            var editorFontSizeSp by remember { mutableFloatStateOf(settingsStore.getFontSizeSp()) }

            PromptNotepadTheme(darkTheme = isDarkTheme, editorFontSize = editorFontSizeSp.sp) {
                PromptNotepadApp(
                    notesDir = notesDir,
                    intentState = currentIntentState,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { newValue ->
                        isDarkTheme = newValue
                        settingsStore.setDarkTheme(newValue)
                    },
                    editorFontSizeSp = editorFontSizeSp,
                    onFontSizeChange = { newSize ->
                        editorFontSizeSp = newSize
                        settingsStore.setFontSizeSp(newSize)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntentState.value = intent
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
private fun PromptNotepadApp(
    notesDir: File,
    intentState: MutableState<Intent?>,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    editorFontSizeSp: Float,
    onFontSizeChange: (Float) -> Unit
) {
    val tabManager = rememberSaveable(saver = tabManagerSaver()) { TabManager() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = "Gagal menyimpan/membaca berkas. Perubahan terakhir mungkin belum tersimpan."
    val context = LocalContext.current

    // Batch A (redesain ala TxtPad+): app TIDAK LAGI otomatis membuka QuickNote
    // saat dijalankan — pengguna sekarang mendarat di Daftar File dulu (lihat
    // showFileList di bawah), sama seperti alur TxtPad+. QuickNote.txt lama
    // (jika masih ada dari versi sebelumnya) tetap tampil sebagai berkas biasa
    // di daftar, tidak dihapus, hanya tidak lagi auto-terbuka.
    val pinStore = remember { PinStore(context) }
    var showFileList by rememberSaveable { mutableStateOf(true) }
    var fileListRefreshTrigger by remember { mutableStateOf(0) }

    fun returnToFileList() {
        fileListRefreshTrigger++
        showFileList = true
    }

    // "Buka Dengan": berkas eksternal dari Intent VIEW/EDIT (file manager/app lain)
    // diimpor jadi tab baru. Dipicu ulang otomatis tiap kali intentState berubah,
    // termasuk saat app sudah berjalan (lewat onNewIntent + launchMode singleTop).
    val incomingIntent = intentState.value
    LaunchedEffect(incomingIntent) {
        val uri = incomingIntent?.data ?: return@LaunchedEffect
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // Tidak semua provider/Intent memberi izin permanen — akses sesi ini
            // (dari flag Intent VIEW/EDIT itu sendiri) tetap cukup untuk edit+save.
        }
        val result = ExternalFileUtils.importFromUri(context, uri, notesDir)
        result.onSuccess { localFile ->
            tabManager.openFileInTab(localFile, sourceUri = uri)
            showFileList = false
        }.onFailure {
            snackbarHostState.showSnackbar(it.message ?: "Gagal membuka berkas eksternal.")
        }
        intentState.value = null
    }

    var showRegexDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
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
    val colors = LocalAppColors.current

    // Tombol back sistem: kalau sedang di editor, kembali ke Daftar File dulu
    // (ala TxtPad+) — bukan langsung keluar app. Kalau sudah di Daftar File,
    // biarkan perilaku back bawaan (keluar app) yang berjalan.
    BackHandler(enabled = !showFileList) {
        returnToFileList()
    }

    if (showFileList) {
        FileListScreen(
            notesDir = notesDir,
            pinStore = pinStore,
            refreshTrigger = fileListRefreshTrigger,
            onOpenFile = { file ->
                tabManager.openFileInTab(file)
                showFileList = false
            },
            onCreateNewFile = {
                coroutineScope.launch {
                    val result = FileUtils.createNewFile(notesDir, "Catatan")
                    result.onSuccess { newFile ->
                        tabManager.openFileInTab(newFile)
                        showFileList = false
                    }.onFailure {
                        snackbarHostState.showSnackbar(errorMessage)
                    }
                }
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PromptNotepad", color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { returnToFileList() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali ke Daftar File", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            EditorSection(
                tabManager = tabManager,
                activeTab = activeTab,
                isMarkdownFile = isMarkdownFile,
                previewMode = previewMode,
                onTogglePreview = { previewMode = !previewMode },
                notesDir = notesDir,
                coroutineScope = coroutineScope,
                snackbarHostState = snackbarHostState,
                errorMessage = errorMessage,
                regexRequest = regexRequest,
                onBrowseFiles = { returnToFileList() },
                onOpenRegexDialog = { showRegexDialog = true },
                onOpenSettingsDialog = { showSettingsDialog = true },
                onCloseTab = { index ->
                    val tab = tabManager.openTabs.getOrNull(index)
                    if (tab != null && tab.isDirty.value) {
                        pendingCloseIndex = index
                    } else {
                        tabManager.closeTab(index)
                        if (tabManager.openTabs.isEmpty()) returnToFileList()
                    }
                }
            )
        }
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

    if (showSettingsDialog) {
        DisplaySettingsDialog(
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            fontSizeSp = editorFontSizeSp,
            onFontSizeChange = onFontSizeChange,
            onDismiss = { showSettingsDialog = false }
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
    onTogglePreview: () -> Unit,
    notesDir: File,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    errorMessage: String,
    regexRequest: RegexRequest?,
    onBrowseFiles: () -> Unit,
    onOpenRegexDialog: () -> Unit,
    onOpenSettingsDialog: () -> Unit,
    onCloseTab: (Int) -> Unit
) {
    val context = LocalContext.current
    var fieldValue by remember(activeTab?.id) { mutableStateOf(TextFieldValue("")) }
    var showFileInfoDialog by remember { mutableStateOf(false) }
    var showFindDialog by remember { mutableStateOf(false) }
    var showScrollDialog by remember { mutableStateOf(false) }

    // Undo/Redo: checkpoint per tab, bukan per-karakter (agar stack tidak meledak
    // pada pengetikan cepat). Setiap 600ms jeda mengetik, state sebelumnya disimpan
    // sebagai satu titik pulih. Direset setiap kali tab aktif berpindah.
    val undoStack = remember(activeTab?.id) { mutableStateListOf<String>() }
    val redoStack = remember(activeTab?.id) { mutableStateListOf<String>() }
    var undoCheckpoint by remember(activeTab?.id) { mutableStateOf(fieldValue.text) }
    LaunchedEffect(activeTab?.id, fieldValue.text) {
        delay(600)
        if (fieldValue.text != undoCheckpoint) {
            undoStack.add(undoCheckpoint)
            if (undoStack.size > 100) undoStack.removeAt(0)
            redoStack.clear()
            undoCheckpoint = fieldValue.text
        }
    }

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
                val uri = tab.sourceUri
                if (uri != null) {
                    // Sinkron-balik ke berkas asal ("Buka Dengan"). Salinan lokal SUDAH
                    // aman tersimpan di atas — kegagalan di sini hanya berarti sinkronisasi
                    // ke berkas eksternal yang gagal, bukan kehilangan data pengguna.
                    val mirror = ExternalFileUtils.writeBackToUri(context, uri, content)
                    if (mirror.isFailure) {
                        snackbarHostState.showSnackbar(
                            "Tersimpan lokal, tapi gagal sinkron ke berkas asal: ${mirror.exceptionOrNull()?.message ?: "izin dicabut"}"
                        )
                    }
                }
            } else {
                snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: errorMessage)
            }
        }
    }

    fun performUndo() {
        if (undoStack.isEmpty()) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Tidak ada lagi yang bisa diurungkan.") }
            return
        }
        val previous = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(fieldValue.text)
        undoCheckpoint = previous
        fieldValue = TextFieldValue(previous, selection = TextRange(previous.length))
        saveActiveTab(previous)
    }

    fun performRedo() {
        if (redoStack.isEmpty()) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Tidak ada lagi yang bisa diulangi.") }
            return
        }
        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(fieldValue.text)
        undoCheckpoint = next
        fieldValue = TextFieldValue(next, selection = TextRange(next.length))
        saveActiveTab(next)
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
        bottomFileBar = {
            BottomFileBar(
                activeFileName = activeTab?.title,
                onBrowseFiles = onBrowseFiles,
                onNewFile = {
                    coroutineScope.launch {
                        val result = FileUtils.createNewFile(notesDir, "Catatan")
                        result.onSuccess { newFile ->
                            tabManager.openFileInTab(newFile)
                        }.onFailure {
                            snackbarHostState.showSnackbar(errorMessage)
                        }
                    }
                },
                menuItems = buildList {
                    if (isMarkdownFile) {
                        add(OverflowMenuItem(label = "Pratinjau Markdown", onClick = onTogglePreview))
                    }
                    add(OverflowMenuItem(label = "Cari & Ganti (Regex)", onClick = onOpenRegexDialog))
                    add(OverflowMenuItem(label = "Urungkan (Undo)", onClick = { performUndo() }))
                    add(OverflowMenuItem(label = "Ulangi (Redo)", onClick = { performRedo() }))
                    add(OverflowMenuItem(label = "Cari di Berkas", onClick = { showFindDialog = true }))
                    add(
                        OverflowMenuItem(
                            label = "Cetak",
                            onClick = {
                                if (activeTab != null) {
                                    printDocument(context, activeTab.title, fieldValue.text)
                                }
                            }
                        )
                    )
                    add(OverflowMenuItem(label = "Gulir ke...", onClick = { showScrollDialog = true }))
                    add(
                        OverflowMenuItem(
                            label = "Info Berkas",
                            onClick = { if (activeTab != null) showFileInfoDialog = true }
                        )
                    )
                    add(OverflowMenuItem(label = "Pengaturan Tampilan", onClick = onOpenSettingsDialog))
                }
            )
        },
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

    if (showFileInfoDialog && activeTab != null) {
        FileInfoDialog(tab = activeTab, onDismiss = { showFileInfoDialog = false })
    }

    if (showFindDialog) {
        FindInFileDialog(
            text = fieldValue.text,
            onJumpTo = { range ->
                fieldValue = fieldValue.copy(selection = TextRange(range.first, range.last + 1))
            },
            onDismiss = { showFindDialog = false }
        )
    }

    if (showScrollDialog) {
        ScrollToDialog(
            text = fieldValue.text,
            onScrollToOffset = { offset ->
                val clamped = offset.coerceIn(0, fieldValue.text.length)
                fieldValue = fieldValue.copy(selection = TextRange(clamped))
                showScrollDialog = false
            },
            onDismiss = { showScrollDialog = false }
        )
    }
}

/**
 * Mengirim konten tab aktif ke sistem cetak Android (Print Framework bawaan,
 * tanpa dependensi baru) lewat WebView sebagai perantara render — hanya dipakai
 * sesaat untuk menghasilkan PrintDocumentAdapter, tidak ditampilkan ke pengguna.
 */
private fun printDocument(context: Context, title: String, content: String) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            val adapter = view.createPrintDocumentAdapter(title)
            printManager.print(title, adapter, PrintAttributes.Builder().build())
        }
    }
    val escaped = content
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    val html = "<pre style=\"font-family:monospace;white-space:pre-wrap;word-wrap:break-word;\">$escaped</pre>"
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

/**
 * Dialog cari-di-berkas: pencarian case-insensitive pada teks tab aktif.
 * "Berikutnya"/"Sebelumnya" berputar (wrap-around) melalui seluruh kecocokan.
 * Melompat ke kecocokan dilakukan dengan memindah `selection` field editor —
 * BasicTextField otomatis menggulir agar posisi kursor/seleksi tetap terlihat,
 * jadi tidak perlu state scroll tambahan.
 */
@Composable
private fun FindInFileDialog(
    text: String,
    onJumpTo: (IntRange) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var matchIndex by remember { mutableStateOf(0) }

    val matches = remember(text, query) {
        if (query.isEmpty()) {
            emptyList()
        } else {
            val found = mutableListOf<IntRange>()
            var start = 0
            while (true) {
                val idx = text.indexOf(query, start, ignoreCase = true)
                if (idx == -1) break
                found.add(idx until (idx + query.length))
                start = idx + 1
            }
            found
        }
    }

    fun jumpToCurrent() {
        if (matches.isNotEmpty()) {
            onJumpTo(matches[matchIndex.coerceIn(0, matches.lastIndex)])
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cari di Berkas") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        matchIndex = 0
                    },
                    label = { Text("Kata kunci") }
                )
                Text(
                    text = if (query.isEmpty()) {
                        "Ketik untuk mencari"
                    } else if (matches.isEmpty()) {
                        "Tidak ditemukan"
                    } else {
                        "Kecocokan ${matchIndex + 1} dari ${matches.size}"
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (matches.isNotEmpty()) {
                    matchIndex = (matchIndex + 1) % matches.size
                    jumpToCurrent()
                }
            }) { Text("Berikutnya") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    if (matches.isNotEmpty()) {
                        matchIndex = (matchIndex - 1 + matches.size) % matches.size
                        jumpToCurrent()
                    }
                }) { Text("Sebelumnya") }
                TextButton(onClick = onDismiss) { Text("Tutup") }
            }
        }
    )
}

/**
 * Dialog gulir-ke: pintasan Awal/Akhir berkas, atau nomor baris spesifik.
 * Sama seperti [FindInFileDialog], cukup memindah `selection` — BasicTextField
 * yang menangani penggulirannya secara otomatis.
 */
@Composable
private fun ScrollToDialog(
    text: String,
    onScrollToOffset: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var lineInput by remember { mutableStateOf("") }
    val lineStartOffsets = remember(text) {
        val offsets = mutableListOf(0)
        text.forEachIndexed { index, c -> if (c == '\n') offsets.add(index + 1) }
        offsets
    }
    val lineCount = lineStartOffsets.size

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gulir ke...") },
        text = {
            Column {
                Text("Berkas ini punya $lineCount baris.")
                OutlinedTextField(
                    value = lineInput,
                    onValueChange = { lineInput = it.filter(Char::isDigit) },
                    label = { Text("Nomor baris") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val line = lineInput.toIntOrNull()
                if (line != null && line in 1..lineCount) {
                    onScrollToOffset(lineStartOffsets[line - 1])
                }
            }) { Text("Ke Baris") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onScrollToOffset(0) }) { Text("Ke Awal") }
                TextButton(onClick = { onScrollToOffset(text.length) }) { Text("Ke Akhir") }
                TextButton(onClick = onDismiss) { Text("Batal") }
            }
        }
    )
}

/**
 * Dialog info berkas — satu-satunya item menu ⋮ yang benar-benar berfungsi penuh
 * (bukan placeholder "Segera Hadir"), menampilkan nama, path, ukuran, dan waktu
 * terakhir diubah dari data `File` yang sudah tersedia di [TabItem]. Tidak
 * menyentuh I/O baru — hanya membaca metadata file yang sudah ada.
 */
@Composable
private fun FileInfoDialog(tab: TabItem, onDismiss: () -> Unit) {
    val file = tab.file
    val sizeText = if (file.exists()) "${file.length()} bytes" else "Belum tersimpan ke disk"
    val modifiedText = if (file.exists() && file.lastModified() > 0) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(file.lastModified())
    } else {
        "-"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
        title = { Text("Info Berkas") },
        text = {
            Column {
                Text("Nama: ${tab.title}")
                Text("Lokasi: ${file.absolutePath}")
                Text("Ukuran: $sizeText")
                Text("Terakhir diubah: $modifiedText")
                if (tab.sourceUri != null) {
                    Text("Tersinkron dari berkas eksternal (\"Buka Dengan\")")
                }
            }
        }
    )
}

/**
 * Dialog Pengaturan Tampilan (Batch 2): ukuran font editor (stepper +/-, batas
 * [SettingsStore.MIN_FONT_SIZE_SP]..[SettingsStore.MAX_FONT_SIZE_SP]) dan toggle
 * tema terang/gelap (default tetap gelap). Perubahan langsung diterapkan (live
 * preview) dan dipersist lewat callback ke [SettingsStore] di pemanggil —
 * dialog ini sendiri tidak menyentuh I/O.
 */
@Composable
private fun DisplaySettingsDialog(
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    fontSizeSp: Float,
    onFontSizeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengaturan Tampilan") },
        text = {
            Column {
                Text("Ukuran Font Editor", color = colors.textSecondary, fontSize = 13.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newSize = (fontSizeSp - SettingsStore.FONT_SIZE_STEP_SP)
                            .coerceIn(SettingsStore.MIN_FONT_SIZE_SP, SettingsStore.MAX_FONT_SIZE_SP)
                        onFontSizeChange(newSize)
                    }) { Text("−", fontSize = 20.sp, color = colors.accent) }

                    Text(
                        text = "${fontSizeSp.toInt()}sp",
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    IconButton(onClick = {
                        val newSize = (fontSizeSp + SettingsStore.FONT_SIZE_STEP_SP)
                            .coerceIn(SettingsStore.MIN_FONT_SIZE_SP, SettingsStore.MAX_FONT_SIZE_SP)
                        onFontSizeChange(newSize)
                    }) { Text("+", fontSize = 20.sp, color = colors.accent) }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDarkTheme) "Tema Gelap" else "Tema Terang",
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = !isDarkTheme, onCheckedChange = { onToggleTheme(!it) })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Selesai") }
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
