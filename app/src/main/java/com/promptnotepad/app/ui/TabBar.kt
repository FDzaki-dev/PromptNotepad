package com.promptnotepad.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.promptnotepad.app.state.TabManager
import com.promptnotepad.app.ui.theme.LocalAppColors

@Composable
fun TabBar(
    tabManager: TabManager,
    onCloseTab: (Int) -> Unit = { index -> tabManager.closeTab(index) }
) {
    if (tabManager.openTabs.isEmpty()) return

    val colors = LocalAppColors.current

    ScrollableTabRow(
        selectedTabIndex = tabManager.activeTabIndex.value,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        edgePadding = 0.dp
    ) {
        tabManager.openTabs.forEachIndexed { index, tab ->
            val isActive = tabManager.activeTabIndex.value == index
            val isDirty by tab.isDirty
            Tab(
                selected = isActive,
                onClick = { tabManager.activeTabIndex.value = index },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isDirty) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = tab.title,
                            color = if (isActive) colors.textPrimary else colors.textSecondary,
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
                                tint = if (isActive) colors.accent else colors.textSecondary
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
