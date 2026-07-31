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

fun highlightTodoSyntax(text: String): AnnotatedString {
    return buildAnnotatedString {
        append(text)

        priorityRegex.findAll(text).forEach { match ->
            addStyle(
                style = SpanStyle(color = PremiumAccent, fontWeight = FontWeight.Bold),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        contextRegex.findAll(text).forEach { match ->
            addStyle(
                style = SpanStyle(color = CodeGreen),
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
