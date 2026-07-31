package com.promptnotepad.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.promptnotepad.app.ui.theme.DeepGray
import com.promptnotepad.app.ui.theme.PremiumAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ShortcutAction(val label: String, val staticInsert: String?, val dynamic: Boolean = false)

private val shortcuts = listOf(
    ShortcutAction("#", "# "),
    ShortcutAction("- [ ]", "- [ ] "),
    ShortcutAction("()", "()"),
    ShortcutAction("Tab", "    "),
    ShortcutAction("Time", null, dynamic = true)
)

/**
 * Baris tombol cepat di atas keyboard. Tombol "Time" menyisipkan timestamp
 * saat ini (bukan nilai statis) agar selalu akurat ketika ditekan.
 */
@Composable
fun ShortcutBar(onInsertText: (String) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepGray)
            .padding(vertical = 6.dp)
    ) {
        items(shortcuts) { action ->
            TextButton(
                onClick = {
                    val text = if (action.dynamic) {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    } else {
                        action.staticInsert.orEmpty()
                    }
                    onInsertText(text)
                },
                colors = ButtonDefaults.textButtonColors(contentColor = PremiumAccent)
            ) {
                Text(action.label)
            }
        }
    }
}
