package com.promptnotepad.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.promptnotepad.app.ui.theme.CodeGreen
import com.promptnotepad.app.ui.theme.PremiumAccent

private val priorityRegex = Regex("\\([A-Z]\\)")
private val contextRegex = Regex("@\\w+")
private val projectRegex = Regex("\\+\\w+")

/**
 * [priorityColor]/[contextColor] default ke warna tema gelap asli untuk
 * kompatibilitas mundur — TextEditor.kt (satu-satunya pemanggil saat ini)
 * meneruskan warna dari tema aktif (Batch 2) agar kontras tetap terjaga
 * di tema terang.
 */
fun highlightTodoSyntax(
    text: String,
    priorityColor: Color = PremiumAccent,
    contextColor: Color = CodeGreen
): AnnotatedString {
    return buildAnnotatedString {
        append(text)

        priorityRegex.findAll(text).forEach { match ->
            addStyle(
                style = SpanStyle(color = priorityColor, fontWeight = FontWeight.Bold),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        contextRegex.findAll(text).forEach { match ->
            addStyle(
                style = SpanStyle(color = contextColor),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        projectRegex.findAll(text).forEach { match ->
            addStyle(
                style = SpanStyle(color = Color(0xFF5CA8FF)),
                start = match.range.first,
                end = match.range.last + 1
            )
        }
    }
}
