package com.cboyxd.gitlab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlRadius

/**
 * Markdown field with a formatting toolbar and a rendered preview. Toolbar actions wrap the
 * selection (bold, code, link) or prefix the current lines (quote, lists, heading), so they work
 * the same with or without a selection.
 */
@Composable
fun MarkdownEditor(
  value: TextFieldValue,
  onValueChange: (TextFieldValue) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  placeholder: String? = null,
  minLines: Int = 4,
  maxLines: Int = 14,
  enabled: Boolean = true,
  isError: Boolean = false,
  supportingText: String? = null,
  previewContext: MarkdownContext = MarkdownContext(),
) {
  val c = Gl.colors
  var preview by rememberSaveable { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      GlChip(text = "Write", selected = !preview, onClick = { preview = false })
      GlChip(text = "Preview", selected = preview, onClick = { preview = true })
    }
    if (preview) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 120.dp)
          .border(1.dp, c.borderStrong, RoundedCornerShape(GlRadius.control))
          .padding(12.dp),
      ) {
        if (value.text.isBlank()) {
          Text("Nothing to preview", style = Gl.type.bodySmall, color = c.textTertiary)
        } else {
          MarkdownText(value.text, context = previewContext)
        }
      }
    } else {
      OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        minLines = minLines,
        maxLines = maxLines,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = c.textPrimary,
          unfocusedBorderColor = c.borderStrong,
          focusedLabelColor = c.textPrimary,
          unfocusedLabelColor = c.textSecondary,
          cursorColor = c.textPrimary,
          errorBorderColor = c.danger,
          errorLabelColor = c.danger,
          errorSupportingTextColor = c.danger,
        ),
        modifier = Modifier.fillMaxWidth(),
      )
      MarkdownToolbar(enabled = enabled) { action -> onValueChange(action(value)) }
    }
  }
}

@Composable
private fun MarkdownToolbar(enabled: Boolean, apply: ((TextFieldValue) -> TextFieldValue) -> Unit) {
  val scroll = rememberScrollState()
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Gl.colors.surface2, RoundedCornerShape(GlRadius.control))
      .horizontalScroll(scroll),
  ) {
    ToolbarButton(Icons.Outlined.Title, "Heading", enabled) { apply { it.prefixLines("### ") } }
    ToolbarButton(Icons.Outlined.FormatBold, "Bold", enabled) { apply { it.wrap("**", "**", "bold text") } }
    ToolbarButton(Icons.Outlined.FormatItalic, "Italic", enabled) { apply { it.wrap("_", "_", "italic text") } }
    ToolbarButton(Icons.Outlined.FormatStrikethrough, "Strikethrough", enabled) { apply { it.wrap("~~", "~~", "text") } }
    ToolbarButton(Icons.Outlined.Code, "Code", enabled) { apply { it.code() } }
    ToolbarButton(Icons.Outlined.Link, "Link", enabled) { apply { it.link() } }
    ToolbarButton(Icons.Outlined.FormatQuote, "Quote", enabled) { apply { it.prefixLines("> ") } }
    ToolbarButton(Icons.AutoMirrored.Outlined.FormatListBulleted, "Bulleted list", enabled) { apply { it.prefixLines("- ") } }
    ToolbarButton(Icons.Outlined.FormatListNumbered, "Numbered list", enabled) { apply { it.prefixLines("1. ") } }
    ToolbarButton(Icons.Outlined.CheckBox, "Task list", enabled) { apply { it.prefixLines("- [ ] ") } }
    ToolbarButton(Icons.Outlined.AlternateEmail, "Mention someone", enabled) { apply { it.insert("@") } }
  }
}

@Composable
private fun ToolbarButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
  IconButton(onClick = onClick, enabled = enabled) {
    Icon(icon, contentDescription = description, tint = if (enabled) Gl.colors.textSecondary else Gl.colors.textTertiary)
  }
}

// Text transformations. Pure functions over TextFieldValue so they are easy to reason about.

internal fun TextFieldValue.wrap(before: String, after: String, placeholder: String): TextFieldValue {
  val start = selection.min
  val end = selection.max
  val selected = text.substring(start, end)
  val inner = selected.ifEmpty { placeholder }
  val newText = text.substring(0, start) + before + inner + after + text.substring(end)
  // Select the inner text so typing replaces the placeholder.
  return copy(text = newText, selection = TextRange(start + before.length, start + before.length + inner.length))
}

internal fun TextFieldValue.code(): TextFieldValue {
  val selected = text.substring(selection.min, selection.max)
  return if ('\n' in selected) wrap("```\n", "\n```", "code") else wrap("`", "`", "code")
}

internal fun TextFieldValue.link(): TextFieldValue {
  val start = selection.min
  val end = selection.max
  val label = text.substring(start, end).ifEmpty { "link text" }
  val url = "https://"
  val inserted = "[$label]($url)"
  val newText = text.substring(0, start) + inserted + text.substring(end)
  val urlStart = start + label.length + 3
  return copy(text = newText, selection = TextRange(urlStart + url.length))
}

internal fun TextFieldValue.insert(value: String): TextFieldValue {
  val start = selection.min
  val newText = text.substring(0, start) + value + text.substring(selection.max)
  return copy(text = newText, selection = TextRange(start + value.length))
}

/** Prefixes every line touched by the selection; toggles the prefix off when all lines have it. */
internal fun TextFieldValue.prefixLines(prefix: String): TextFieldValue {
  val lineStart = text.lastIndexOf('\n', (selection.min - 1).coerceAtLeast(0)).let { if (selection.min == 0 || it < 0) 0 else it + 1 }
  val lineEnd = text.indexOf('\n', selection.max).let { if (it < 0) text.length else it }
  val block = text.substring(lineStart, lineEnd)
  val lines = block.split('\n')
  val allPrefixed = lines.all { it.startsWith(prefix) }
  val numbered = prefix == "1. "
  val changed = lines.mapIndexed { index, line ->
    when {
      allPrefixed -> line.removePrefix(prefix)
      numbered -> "${index + 1}. $line"
      else -> prefix + line
    }
  }.joinToString("\n")
  val newText = text.substring(0, lineStart) + changed + text.substring(lineEnd)
  return copy(text = newText, selection = TextRange(lineStart + changed.length))
}
