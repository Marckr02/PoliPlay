package com.example.campuspocket.feature.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campuspocket.feature.notes.domain.MarkdownParser

/** Renderizador propio del subconjunto de Markdown de las notas (AnnotatedString, sin librerías). */
@Composable
fun MarkdownPreview(
    markdown: String,
    onToggleCheckbox: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val blocks = remember(markdown) { MarkdownParser.parse(markdown) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownParser.Heading -> Text(
                    text = block.text,
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    }
                )
                is MarkdownParser.Paragraph -> Text(renderInline(block.text), style = MaterialTheme.typography.bodyMedium)
                is MarkdownParser.UnorderedListItem -> Row(Modifier.fillMaxWidth()) {
                    Text("•", modifier = Modifier.padding(end = 6.dp))
                    Text(renderInline(block.text), style = MaterialTheme.typography.bodyMedium)
                }
                is MarkdownParser.OrderedListItem -> Row(Modifier.fillMaxWidth()) {
                    Text("${block.number}.", modifier = Modifier.padding(end = 6.dp))
                    Text(renderInline(block.text), style = MaterialTheme.typography.bodyMedium)
                }
                is MarkdownParser.CodeBlock -> Text(
                    text = block.text,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                )
                is MarkdownParser.Checkbox -> Row(
                    modifier = Modifier.fillMaxWidth().clickable { onToggleCheckbox(block.rawStart) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (block.checked) "☑" else "☐", modifier = Modifier.padding(end = 6.dp))
                    Text(renderInline(block.text), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Negrita **...**, cursiva *...* o _..._ y código en línea `...` sobre AnnotatedString. */
internal fun renderInline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i + 2) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold)); append(text.substring(i + 2, end)); pop()
                    i = end + 2
                } else { append(text[i]); i++ }
            }
            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end > i + 1) {
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x1A000000)))
                    append(text.substring(i + 1, end))
                    pop()
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            (text.startsWith("*", i) || text.startsWith("_", i)) -> {
                val marker = text[i]
                val end = text.indexOf(marker, i + 1)
                if (end > i + 1) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(text.substring(i + 1, end)); pop()
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
}
