package com.promptnotepad.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.promptnotepad.app.ui.theme.LocalAppColors
import com.promptnotepad.app.ui.theme.LocalEditorFontSize
import com.promptnotepad.app.ui.theme.editorTextStyle

/**
 * Editor teks utama. Auto-save dipanggil di setiap perubahan karakter (onValueChange)
 * melalui callback [onContentChange], sehingga penyimpanan bersifat instan per keystroke.
 */
@Composable
fun TextEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enableTodoHighlight: Boolean = true
) {
    // Debounce highlighting: hasil highlight hanya dihitung ulang 250ms setelah
    // pengguna berhenti mengetik, agar regex todo.txt tidak membebani UI thread
    // pada setiap keystroke di file besar.
    var debouncedText by remember { mutableStateOf(value.text) }
    LaunchedEffect(value.text) {
        delay(250)
        debouncedText = value.text
    }

    // Batch 2: warna & ukuran font kini mengikuti tema/pengaturan aktif alih-alih
    // konstanta tetap — dibaca dari CompositionLocal yang disediakan PromptNotepadTheme.
    val colors = LocalAppColors.current
    val fontSize = LocalEditorFontSize.current
    val style = editorTextStyle(fontSize = fontSize, color = colors.textPrimary)

    BasicTextField(
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue)
            if (newValue.text != value.text) {
                onContentChange(newValue.text)
            }
        },
        textStyle = style,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(style.color),
        visualTransformation = if (enableTodoHighlight) {
            TodoVisualTransformation(
                stableText = debouncedText,
                priorityColor = colors.accent,
                contextColor = colors.codeGreen
            )
        } else {
            VisualTransformation.None
        },
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    )
}

/**
 * Menerapkan highlight hanya jika teks saat ini sudah cocok dengan [stableText]
 * (nilai yang sudah didebounce). Selagi menunggu jeda 250ms, teks tetap tampil
 * apa adanya tanpa highlight agar tidak ada delay pada pengetikan itu sendiri.
 * Warna prioritas/konteks diteruskan dari tema aktif (Batch 2) agar tetap
 * kontras baik di tema gelap maupun terang.
 */
private class TodoVisualTransformation(
    private val stableText: String,
    private val priorityColor: Color,
    private val contextColor: Color
) : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        if (text.text != stableText) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val highlighted = highlightTodoSyntax(text.text, priorityColor, contextColor)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

/**
 * Menyisipkan teks pada posisi kursor (atau menggantikan seleksi aktif),
 * digunakan oleh ShortcutBar dan tombol timestamp.
 */
fun insertAtCursor(currentValue: TextFieldValue, textToInsert: String): TextFieldValue {
    val selection = currentValue.selection
    val originalText = currentValue.text

    val newText = StringBuilder(originalText)
        .replace(selection.start, selection.end, textToInsert)
        .toString()

    val newCursorPosition = selection.start + textToInsert.length

    return TextFieldValue(
        text = newText,
        selection = androidx.compose.ui.text.TextRange(newCursorPosition)
    )
}
