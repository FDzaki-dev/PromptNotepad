package com.promptnotepad.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.promptnotepad.app.ui.theme.LocalAppColors

private sealed class MdBlock {
    data class Heading(val text: String, val level: Int) : MdBlock()
    data class Checkbox(val text: String, val checked: Boolean) : MdBlock()
    data class Bullet(val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
}

private fun parseMarkdown(content: String): List<MdBlock> {
    return content.lineSequence().filter { it.isNotBlank() }.map { line ->
        val trimmed = line.trimStart()
        when {
            trimmed.startsWith("### ") -> MdBlock.Heading(trimmed.removePrefix("### "), 3)
            trimmed.startsWith("## ") -> MdBlock.Heading(trimmed.removePrefix("## "), 2)
            trimmed.startsWith("# ") -> MdBlock.Heading(trimmed.removePrefix("# "), 1)
            trimmed.startsWith("- [x] ") -> MdBlock.Checkbox(trimmed.removePrefix("- [x] "), true)
            trimmed.startsWith("- [ ] ") -> MdBlock.Checkbox(trimmed.removePrefix("- [ ] "), false)
            trimmed.startsWith("- ") -> MdBlock.Bullet(trimmed.removePrefix("- "))
            else -> MdBlock.Paragraph(trimmed)
        }
    }.toList()
}

/**
 * Render visual Markdown ringan, sepenuhnya lokal/offline (tanpa WebView atau library eksternal).
 * Mendukung heading (#, ##, ###), bullet list, dan checkbox todo (- [ ] / - [x]).
 */
@Composable
fun MarkdownViewer(content: String, modifier: Modifier = Modifier) {
    val blocks = parseMarkdown(content)
    val colors = LocalAppColors.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        items(blocks) { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    text = block.text,
                    color = colors.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = when (block.level) {
                        1 -> 22.sp
                        2 -> 19.sp
                        else -> 16.sp
                    },
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                is MdBlock.Checkbox -> Text(
                    text = (if (block.checked) "☑ " else "☐ ") + block.text,
                    color = if (block.checked) colors.textSecondary else colors.textPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                is MdBlock.Bullet -> Text(
                    text = "•  " + block.text,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                is MdBlock.Paragraph -> Text(
                    text = block.text,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
