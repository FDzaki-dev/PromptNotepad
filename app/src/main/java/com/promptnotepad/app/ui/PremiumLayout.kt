package com.promptnotepad.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.promptnotepad.app.state.TabManager
import com.promptnotepad.app.ui.theme.LocalAppColors

/**
 * Kerangka visual: TabBar di atas (dipisahkan garis tipis dari area editor),
 * lalu area editor mengisi sisa ruang, dan shortcutBar/slot tambahan di bawah.
 */
@Composable
fun PremiumLayout(
    tabManager: TabManager,
    onCloseTab: (Int) -> Unit = { index -> tabManager.closeTab(index) },
    bottomFileBar: @Composable () -> Unit = {},
    shortcutBar: @Composable () -> Unit = {},
    editorContent: @Composable () -> Unit
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Box(modifier = Modifier.background(colors.surface)) {
            TabBar(tabManager = tabManager, onCloseTab = onCloseTab)
        }
        HorizontalDivider(thickness = 1.dp, color = colors.border)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            editorContent()
        }

        HorizontalDivider(thickness = 1.dp, color = colors.border)
        bottomFileBar()
        HorizontalDivider(thickness = 1.dp, color = colors.border)
        shortcutBar()
    }
}
