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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
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
import com.promptnotepad.app.util.RegexUtils
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

@Composable
private fun PromptNotepadApp(notesDir: File) {
    val tabManager = remember { TabManager() }

    // QuickNote: tab coretan instan yang otomatis terbuka saat aplikasi dijalankan.
    remember {
        val quickNoteFile = File(notesDir, "QuickNote.txt")
        if (!quickNoteFile.exists()) quickNoteFile.createNewFile()
        tabManager.openFileInTab(quickNoteFile)
        true
    }

    var fieldValue by remember(tabManager.activeTabIndex.value) {
        val tab = tabManager.activeTab()
        mutableStateOf(
            TextFieldValue(if (tab != null) FileUtils.readFile(tab.file) else "")
        )
    }

    var showFileList by remember { mutableStateOf(false) }
    var showRegexDialog by remember { mutableStateOf(false) }
    var previewMode by remember(tabManager.activeTabIndex.value) { mutableStateOf(false) }

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
                        val newFile = FileUtils.createNewFile(notesDir, "Catatan")
                        tabManager.openFileInTab(newFile)
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "File Baru", tint = PremiumAccent)
                    }
                }
            )
        },
        containerColor = PureBlack
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            PremiumLayout(
                tabManager = tabManager,
                shortcutBar = {
                    if (!previewMode) {
                        ShortcutBar(onInsertText = { insertText ->
                            fieldValue = insertAtCursor(fieldValue, insertText)
                            activeTab?.let { FileUtils.writeFile(it.file, fieldValue.text) }
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
                        onContentChange = { newContent ->
                            activeTab?.let { FileUtils.writeFile(it.file, newContent) }
                        }
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
                val newText = RegexUtils.findAndReplace(fieldValue.text, pattern, replacement)
                fieldValue = TextFieldValue(newText)
                activeTab?.let { FileUtils.writeFile(it.file, newText) }
                showRegexDialog = false
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
    val files = remember { FileUtils.listTextFiles(notesDir) }
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
