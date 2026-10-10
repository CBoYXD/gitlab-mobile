package com.cboyxd.gitlab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.data.model.GitLabDiffPosition
import com.cboyxd.gitlab.data.model.GitLabFileDiff
import com.cboyxd.gitlab.ui.theme.Gl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A small window around the comment's original line, with syntax and diff colors. */
@Composable
fun DiscussionDiffPreview(position: GitLabDiffPosition, file: GitLabFileDiff?, earlierVersion: Boolean, onOpen: () -> Unit) {
  val colors = Gl.colors
  val path = position.newPath.ifBlank { position.oldPath }
  val preview by produceState<Pair<List<DiffLine>, List<androidx.compose.ui.text.AnnotatedString>>?>(null, file, position, colors) {
    value = withContext(Dispatchers.Default) {
      val lines = parseUnifiedDiff(file?.diff.orEmpty())
      val target = lines.indexOfFirst { line ->
        if (position.newLine != null) line.newNo == position.newLine else position.oldLine != null && line.oldNo == position.oldLine
      }
      if (target < 0) null else {
        val hunkStart = (target downTo 0).firstOrNull { lines[it].kind == DiffKind.Hunk } ?: 0
        val hunkEnd = ((target + 1) until lines.size).firstOrNull { lines[it].kind == DiffKind.Hunk } ?: lines.size
        val start = maxOf(hunkStart + 1, target - 3)
        val end = minOf(hunkEnd, target + 4)
        val text = prepareDiffLines(lines, path, colors)
        lines.subList(start, end) to text.subList(start, end)
      }
    }
  }
  Column(Modifier.fillMaxWidth().background(colors.surface).border(1.dp, colors.border)) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(12.dp)) {
      Text(path.ifBlank { "File discussion" }, style = Gl.type.monoSmall, color = colors.accent)
      val line = position.newLine?.let { "Line $it" } ?: position.oldLine?.let { "Removed line $it" } ?: "File comment"
      Text(listOfNotNull(line, if (earlierVersion) "Earlier version" else null, position.headSha.take(8).takeIf { it.isNotBlank() }).joinToString(" · "), style = Gl.type.bodySmall, color = colors.textSecondary)
    }
    val content = preview
    if (content != null) {
      SelectionContainer {
        Column(Modifier.fillMaxWidth()) {
          content.first.forEachIndexed { index, line ->
            val focused = if (position.newLine != null) line.newNo == position.newLine else line.oldNo == position.oldLine
            DiffLineRow(line, content.second[index], gutter = 38.dp, modifier = if (focused) Modifier.border(1.dp, colors.accent.copy(alpha = .55f)) else Modifier)
          }
        }
      }
    } else {
      Text(if (position.positionType == "text") "Diff context unavailable · Open thread in GitLab" else "Open file discussion in GitLab", style = Gl.type.bodySmall, color = colors.textSecondary, modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(12.dp))
    }
  }
}
