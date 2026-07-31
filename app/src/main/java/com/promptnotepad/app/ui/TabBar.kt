package com.promptnotepad.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.promptnotepad.app.state.TabManager
import com.promptnotepad.app.ui.theme.DeepGray
import com.promptnotepad.app.ui.theme.PremiumAccent
import com.promptnotepad.app.ui.theme.TextPrimary
import com.promptnotepad.app.ui.theme.TextSecondary

@Composable
fun TabBar(tabManager: TabManager, onCloseTab: (Int) -> Unit = { tabManager.closeTab(it) }) {
    if (tabManager.openTabs.isEmpty()) return

    ScrollableTabRow(
        selectedTabIndex = tabManager.activeTabIndex.value,
        containerColor = DeepGray,
        contentColor = TextPrimary,
        edgePadding = 0.dp
    ) {
        tabManager.openTabs.forEachIndexed { index, tab ->
            val isActive = tabManager.activeTabIndex.value == index
            Tab(
                selected = isActive,
                onClick = { tabManager.activeTabIndex.value = index },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tab.title,
                            color = if (isActive) TextPrimary else TextSecondary,
                            fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = { onCloseTab(index) },
                            modifier = Modifier.width(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Tutup tab ${tab.title}",
                                tint = if (isActive) PremiumAccent else TextSecondary
                            )
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun Spacer(modifier: Modifier) {
    androidx.compose.foundation.layout.Spacer(modifier = modifier)
}
