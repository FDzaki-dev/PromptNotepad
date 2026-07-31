package com.promptnotepad.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.promptnotepad.app.ui.theme.PremiumEditorStyle
import com.promptnotepad.app.ui.theme.PureBlack

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
    BasicTextField(
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue)
            if (newValue.text != value.text) {
                onContentChange(newValue.text)
            }
        },
        textStyle = PremiumEditorStyle,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(PremiumEditorStyle.color),
        visualTransformation = if (enableTodoHighlight) TodoVisualTransformation() else VisualTransformation.None,
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(16.dp)
    )
}

private class TodoVisualTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val highlighted = highlightTodoSyntax(text.text)
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
