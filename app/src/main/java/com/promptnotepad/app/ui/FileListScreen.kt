package com.promptnotepad.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.promptnotepad.app.ui.theme.LocalAppColors
import com.promptnotepad.app.util.FileUtils
import com.promptnotepad.app.util.PinStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/** Baris data siap-render untuk satu berkas di Daftar File. */
data class FileListEntry(
    val file: File,
    val snippet: String,
    val isPinned: Boolean
)

/**
 * Layar utama app (Batch A — redesain ala TxtPad+): daftar seluruh berkas
 * `.txt`/`.md` di penyimpanan lokal, berkas yang di-pin selalu tampil duluan
 * di bagian atas. Menggantikan perilaku lama (app langsung membuka QuickNote
 * begitu dijalankan) — sekarang pengguna melihat daftar dulu, baru memilih
 * atau membuat berkas, sama seperti alur TxtPad+.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    notesDir: File,
    pinStore: PinStore,
    refreshTrigger: Int,
    onOpenFile: (File) -> Unit,
    onCreateNewFile: () -> Unit
) {
    val colors = LocalAppColors.current
    var allFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var snippets by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var pinVersion by remember { mutableStateOf(0) }
    var searchActive by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(notesDir, refreshTrigger) {
        val files = FileUtils.listTextFiles(notesDir).getOrDefault(emptyList())
        allFiles = files
        snippets = files.associate { it.absolutePath to FileUtils.readSnippet(it) }
    }

    val entries = remember(allFiles, snippets, pinVersion, query) {
        allFiles
            .filter { query.isBlank() || it.nameWithoutExtension.contains(query, ignoreCase = true) }
            .map { f ->
                FileListEntry(
                    file = f,
                    snippet = snippets[f.absolutePath].orEmpty(),
                    isPinned = pinStore.isPinned(f.absolutePath)
                )
            }
            .sortedWith(compareByDescending<FileListEntry> { it.isPinned }.thenByDescending { it.file.lastModified() })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchActive) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            placeholder = { Text("Cari nama berkas...") },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                focusedContainerColor = colors.surface,
                                unfocusedContainerColor = colors.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Catatan", color = colors.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (searchActive) query = ""
                        searchActive = !searchActive
                    }) {
                        Icon(Icons.Filled.Search, contentDescription = "Cari Berkas", tint = colors.accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNewFile,
                containerColor = colors.accent,
                contentColor = colors.background
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Berkas Baru")
            }
        },
        containerColor = colors.background
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (query.isBlank()) "Belum ada catatan. Ketuk + untuk membuat." else "Tidak ada berkas yang cocok.",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(entries, key = { it.file.absolutePath }) { entry ->
                    FileListRow(
                        entry = entry,
                        onClick = { onOpenFile(entry.file) },
                        onTogglePin = {
                            pinStore.togglePin(entry.file.absolutePath)
                            pinVersion++
                        }
                    )
                    HorizontalDivider(thickness = 1.dp, color = colors.border)
                }
            }
        }
    }
}

@Composable
private fun FileListRow(
    entry: FileListEntry,
    onClick: () -> Unit,
    onTogglePin: () -> Unit
) {
    val colors = LocalAppColors.current
    val dateLabel = remember(entry.file.lastModified()) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(entry.file.lastModified())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.file.nameWithoutExtension,
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.snippet.ifBlank { "Kosong" },
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "  ·  $dateLabel",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
        IconButton(onClick = onTogglePin) {
            Icon(
                imageVector = if (entry.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = if (entry.isPinned) "Lepas Pin" else "Pin Berkas",
                tint = if (entry.isPinned) colors.accent else colors.textSecondary
            )
        }
    }
}
