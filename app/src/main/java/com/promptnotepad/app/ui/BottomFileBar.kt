package com.promptnotepad.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.promptnotepad.app.ui.theme.DeepGray
import com.promptnotepad.app.ui.theme.LockedText
import com.promptnotepad.app.ui.theme.PremiumAccent
import com.promptnotepad.app.ui.theme.TextPrimary

/**
 * Item menu di dalam overflow ("⋮"). Item dengan [available] = false ditampilkan
 * abu-abu dan berlabel "(Segera Hadir)" — sama seperti pola item terkunci pada
 * aplikasi notepad pembanding, tapi memakai istilah yang jujur (bukan "Premium")
 * karena PromptNotepad memang belum punya sistem berlangganan/pembelian apa pun.
 */
data class OverflowMenuItem(
    val label: String,
    val available: Boolean = true,
    val onClick: () -> Unit = {}
)

/**
 * Bar bawah minimal: [ikon buka berkas] [nama tab aktif] [+] [⋮ menu lainnya].
 * Menggantikan deretan ikon di TopAppBar yang sebelumnya tidak berlabel dan
 * membingungkan — hanya dua aksi paling sering dipakai (buka & baru) yang
 * tetap tampil langsung, sisanya dikelompokkan ke menu ⋮ dengan label jelas.
 */
@Composable
fun BottomFileBar(
    activeFileName: String?,
    onBrowseFiles: () -> Unit,
    onNewFile: () -> Unit,
    menuItems: List<OverflowMenuItem>
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepGray)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBrowseFiles) {
            Icon(Icons.Filled.FolderOpen, contentDescription = "Buka Berkas", tint = PremiumAccent)
        }

        Text(
            text = activeFileName ?: "Tidak ada berkas terbuka",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )

        IconButton(onClick = onNewFile) {
            Icon(Icons.Filled.Add, contentDescription = "Berkas Baru", tint = PremiumAccent)
        }

        androidx.compose.foundation.layout.Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Menu Lainnya", tint = PremiumAccent)
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                menuItems.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (item.available) item.label else "${item.label} (Segera Hadir)",
                                color = if (item.available) TextPrimary else LockedText
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            item.onClick()
                        }
                    )
                }
            }
        }
    }
}
