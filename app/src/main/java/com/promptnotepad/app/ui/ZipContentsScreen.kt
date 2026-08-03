package com.promptnotepad.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.promptnotepad.app.ui.theme.LocalAppColors
import com.promptnotepad.app.util.ZipUtils
import java.io.File

/**
 * Layar daftar isi ZIP (dibuka lewat "Buka Dengan"): hanya menampilkan entri
 * .txt/.md di dalam arsip — BUKAN pengelola arsip umum (tidak ada
 * ekstrak-semua, hapus, atau kompresi ulang). Ketuk entri untuk mengimpor &
 * membukanya sebagai tab biasa, lewat jalur lokal yang sama persis dengan
 * berkas lain (lihat [ZipUtils.importEntryToNotes]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZipContentsScreen(
    zipFile: File,
    onOpenEntry: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    var entries by remember { mutableStateOf<List<ZipUtils.ZipEntryInfo>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(zipFile) {
        ZipUtils.listTextEntries(zipFile)
            .onSuccess { entries = it }
            .onFailure { errorMessage = it.message ?: "Gagal membaca isi ZIP." }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(zipFile.name, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        },
        containerColor = colors.background
    ) { padding ->
        val currentEntries = entries
        val currentError = errorMessage
        when {
            currentError != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(currentError, color = colors.textSecondary, fontSize = 14.sp)
                }
            }
            currentEntries == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.accent)
                }
            }
            currentEntries.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tidak ada berkas .txt/.md di dalam ZIP ini.",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.padding(padding)) {
                    items(currentEntries, key = { it.name }) { entry ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenEntry(entry.name) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = entry.name,
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${entry.sizeBytes} bytes",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        HorizontalDivider(thickness = 1.dp, color = colors.border)
                    }
                }
            }
        }
    }
}
