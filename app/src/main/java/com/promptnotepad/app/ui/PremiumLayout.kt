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
import com.promptnotepad.app.ui.theme.DeepGray
import com.promptnotepad.app.ui.theme.PremiumBorder
import com.promptnotepad.app.ui.theme.PureBlack

/**
 * Kerangka visual: TabBar di atas (dipisahkan garis tipis dari area editor),
 * lalu area editor mengisi sisa ruang, dan shortcutBar/slot tambahan di bawah.
 */
@Composable
fun PremiumLayout(
    tabManager: TabManager,
    shortcutBar: @Composable () -> Unit = {},
    onCloseTab: (Int) -> Unit = { tabManager.closeTab(it) },
    editorContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        Box(modifier = Modifier.background(DeepGray)) {
            TabBar(tabManager = tabManager, onCloseTab = onCloseTab)
        }
        HorizontalDivider(thickness = 1.dp, color = PremiumBorder)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            editorContent()
        }

        HorizontalDivider(thickness = 1.dp, color = PremiumBorder)
        shortcutBar()
    }
}
