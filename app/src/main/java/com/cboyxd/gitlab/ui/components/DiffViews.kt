package com.cboyxd.gitlab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.data.model.GitLabFileDiff
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------------------------
// Model
// ---------------------------------------------------------------------------------------------

enum class DiffKind { Context, Add, Remove, Hunk }

/**
 * One rendered diff line. [changed] marks the part of an edited line that differs from its
 * partner on the other side, so the edit stands out inside the line.
 */
@Immutable
data class DiffLine(
  val kind: DiffKind,
  val oldNo: Int?,
  val newNo: Int?,
  val text: String,
  val changed: IntRange? = null,
)

@Immutable
data class DiffStats(val added: Int, val removed: Int)

private val HUNK = Regex("""^@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@.*""")

/** Parses a unified diff body (as GitLab returns it, without the ---/+++ file lines). */
fun parseUnifiedDiff(diff: String): List<DiffLine> {
  val out = ArrayList<DiffLine>()
  var oldNo = 0
  var newNo = 0
  val raw = diff.lines().let { if (it.lastOrNull()?.isEmpty() == true) it.dropLast(1) else it }
  for (line in raw) {
    when {
      line.startsWith("@@") -> {
        HUNK.matchEntire(line)?.let { m ->
          oldNo = m.groupValues[1].toInt()
          newNo = m.groupValues[2].toInt()
        }
        out += DiffLine(DiffKind.Hunk, null, null, line)
      }
      line.startsWith("+++") || line.startsWith("---") || line.startsWith("\\") -> Unit
      line.startsWith("+") -> out += DiffLine(DiffKind.Add, null, newNo++, line.drop(1))
      line.startsWith("-") -> out += DiffLine(DiffKind.Remove, oldNo++, null, line.drop(1))
      else -> out += DiffLine(DiffKind.Context, oldNo++, newNo++, line.removePrefix(" "))
    }
  }
  return markWordChanges(out)
}

/** Pairs each run of removed lines with the run of added lines after it and marks what changed. */
private fun markWordChanges(lines: List<DiffLine>): List<DiffLine> {
  val result = lines.toMutableList()
  var i = 0
  while (i < result.size) {
    if (result[i].kind != DiffKind.Remove) { i++; continue }
    val removeStart = i
    while (i < result.size && result[i].kind == DiffKind.Remove) i++
    val addStart = i
    while (i < result.size && result[i].kind == DiffKind.Add) i++
    val removed = addStart - removeStart
    val added = i - addStart
    if (removed == added && removed in 1..20) {
      for (k in 0 until removed) {
        val a = result[removeStart + k]
        val b = result[addStart + k]
        val (ra, rb) = changedRanges(a.text, b.text) ?: continue
        result[removeStart + k] = a.copy(changed = ra)
        result[addStart + k] = b.copy(changed = rb)
      }
    }
  }
  return result
}

private fun changedRanges(a: String, b: String): Pair<IntRange, IntRange>? {
  if (a == b) return null
  var prefix = 0
  val max = minOf(a.length, b.length)
  while (prefix < max && a[prefix] == b[prefix]) prefix++
  var suffix = 0
  while (suffix < max - prefix && a[a.length - 1 - suffix] == b[b.length - 1 - suffix]) suffix++
  val aEnd = a.length - suffix
  val bEnd = b.length - suffix
  // A line rewritten almost entirely reads better without an inner highlight.
  if (prefix == 0 && suffix == 0) return null
  return (prefix until aEnd) to (prefix until bEnd)
}

fun diffStats(diffs: List<GitLabFileDiff>): DiffStats {
  var added = 0
  var removed = 0
  diffs.forEach { d ->
    d.diff.lineSequence().forEach { line ->
      when {
        line.startsWith("+++") || line.startsWith("---") -> Unit
        line.startsWith("+") -> added++
        line.startsWith("-") -> removed++
      }
    }
  }
  return DiffStats(added, removed)
}

/** Lines ready to draw: syntax colors plus the in-line change background. Blocking. */
fun prepareDiffLines(lines: List<DiffLine>, path: String, colors: GlColors): List<AnnotatedString> {
  val language = languageForPath(path)
  val highlightable = lines.sumOf { it.text.length } < 60_000
  return lines.map { line ->
    if (line.kind == DiffKind.Hunk) return@map AnnotatedString(line.text)
    val base = if (highlightable) highlight(line.text, language, colors) else AnnotatedString(line.text)
    val range = line.changed
    if (range == null || range.isEmpty()) base
    else buildAnnotatedString {
      append(base)
      val word = if (line.kind == DiffKind.Add) colors.diffAddWordBg else colors.diffRemoveWordBg
      addStyle(SpanStyle(background = word), range.first.coerceIn(0, base.length), (range.last + 1).coerceIn(0, base.length))
    }
  }
}

// ---------------------------------------------------------------------------------------------
// Rendering
// ---------------------------------------------------------------------------------------------

/** Width of the line-number gutter for numbers up to [maxLine]. */
fun diffGutterWidth(maxLine: Int): Dp = (maxLine.toString().length.coerceAtLeast(2) * 8 + 16).dp

/**
 * A diff line in the GitHub Mobile manner: a number gutter (new line number, or the old one for
 * removals), a hairline, then the soft-wrapped code on a red or green wash.
 */
@Composable
fun DiffLineRow(line: DiffLine, text: AnnotatedString?, gutter: Dp, modifier: Modifier = Modifier) {
  val c = Gl.colors
  if (line.kind == DiffKind.Hunk) {
    Text(
      text = line.text,
      style = Gl.type.monoSmall,
      color = c.accent,
      modifier = modifier
        .fillMaxWidth()
        .background(c.diffHunkBg)
        .padding(start = gutter + 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
    )
    return
  }
  val (bg, marker) = when (line.kind) {
    DiffKind.Add -> c.diffAddBg to "+"
    DiffKind.Remove -> c.diffRemoveBg to "−"
    else -> Color.Transparent to " "
  }
  val number = if (line.kind == DiffKind.Remove) line.oldNo else line.newNo
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(IntrinsicSize.Min)
      .background(bg)
      .semantics(mergeDescendants = true) {
        contentDescription = when (line.kind) {
          DiffKind.Add -> "Added line ${line.newNo}: ${line.text}"
          DiffKind.Remove -> "Removed line ${line.oldNo}: ${line.text}"
          else -> "Line ${line.newNo}: ${line.text}"
        }
      },
  ) {
    Text(
      text = if (line.kind == DiffKind.Remove) "" else number?.toString().orEmpty(),
      style = Gl.type.monoSmall,
      color = if (line.kind == DiffKind.Add) c.success else c.textTertiary,
      maxLines = 1,
      modifier = Modifier.width(gutter).padding(start = 4.dp, end = 8.dp, top = 3.dp),
      textAlign = androidx.compose.ui.text.style.TextAlign.End,
    )
    Box(Modifier.width(1.dp).fillMaxHeight().background(c.border))
    Text(
      text = marker,
      style = Gl.type.monoSmall,
      color = when (line.kind) {
        DiffKind.Add -> c.success
        DiffKind.Remove -> c.danger
        else -> c.textTertiary
      },
      modifier = Modifier.padding(start = 6.dp, top = 3.dp),
    )
    Text(
      text = text ?: AnnotatedString(line.text),
      style = Gl.type.mono.copy(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)),
      color = c.textPrimary,
      modifier = Modifier.weight(1f).padding(start = 4.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
    )
  }
}

/** File bar above a diff: collapse caret, type icon, path, a "viewed" box and the change count. */
@Composable
fun DiffFileHeader(
  diff: GitLabFileDiff,
  collapsed: Boolean,
  viewed: Boolean,
  onToggleCollapsed: () -> Unit,
  onToggleViewed: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  val c = Gl.colors
  val path = diff.newPath.ifEmpty { diff.oldPath }
  val name = path.substringAfterLast('/')
  val shown = if (diff.renamedFile && diff.oldPath != diff.newPath) "${diff.oldPath} → ${diff.newPath}" else path
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(c.surface)
      .clickable(role = Role.Button, onClickLabel = if (collapsed) "Show changes" else "Hide changes", onClick = onToggleCollapsed)
      .semantics { stateDescription = if (collapsed) "Collapsed" else "Expanded" }
      .defaultMinSize(minHeight = 52.dp)
      .padding(start = 8.dp, end = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      if (collapsed) Icons.AutoMirrored.Outlined.KeyboardArrowRight else Icons.Outlined.KeyboardArrowDown,
      contentDescription = null,
      tint = c.textSecondary,
      modifier = Modifier.size(20.dp),
    )
    Spacer(Modifier.width(6.dp))
    FileTypeIcon(name = name, folder = false, size = 18.dp)
    Spacer(Modifier.width(8.dp))
    Text(
      text = ellipsizeStart(shown, 44),
      style = Gl.type.mono,
      color = c.textPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f).semantics { contentDescription = shown },
    )
    when {
      diff.newFile -> GlBadge("New", tone = Tone.Success)
      diff.deletedFile -> GlBadge("Deleted", tone = Tone.Danger)
      diff.renamedFile -> GlBadge("Renamed", tone = Tone.Info)
    }
    if (onToggleViewed != null) {
      IconButton(onClick = onToggleViewed) {
        Icon(
          if (viewed) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
          contentDescription = if (viewed) "Viewed, mark as not viewed" else "Mark as viewed",
          tint = c.accent,
        )
      }
    } else {
      Spacer(Modifier.width(8.dp))
    }
  }
}

/** Keeps the end of a long path, which says more than its start: "…nel/AGENTS.md". */
fun ellipsizeStart(text: String, max: Int): String =
  if (text.length <= max) text else "…" + text.takeLast(max - 1)

/**
 * A list of changed files for a LazyColumn: a sticky header per file, then its lines as
 * separate items so a huge change only composes what is on screen.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun LazyListScope.diffFileItems(
  diffs: List<GitLabFileDiff>,
  parsed: Map<Int, List<DiffLine>>,
  prepared: Map<Int, List<AnnotatedString>>,
  collapsed: Set<Int>,
  viewed: Set<Int>,
  onToggleCollapsed: (Int) -> Unit,
  onToggleViewed: ((Int) -> Unit)?,
) {
  diffs.forEachIndexed { index, diff ->
    stickyHeader(key = "file-$index") {
      Column {
        DiffFileHeader(
          diff = diff,
          collapsed = index in collapsed,
          viewed = index in viewed,
          onToggleCollapsed = { onToggleCollapsed(index) },
          onToggleViewed = onToggleViewed?.let { { it(index) } },
        )
        GlDivider()
      }
    }
    if (index in collapsed) return@forEachIndexed
    val lines = parsed[index]
    when {
      diff.diff.isBlank() -> item(key = "file-$index-empty") {
        Text(
          "No text changes (binary or too large to show)",
          style = Gl.type.meta,
          color = Gl.colors.textSecondary,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
      }
      lines == null -> item(key = "file-$index-loading") { LoadingState(label = "Loading changes") }
      else -> {
        val gutter = diffGutterWidth(lines.maxOf { maxOf(it.oldNo ?: 0, it.newNo ?: 0) })
        val texts = prepared[index]
        items(lines.size, key = { "file-$index-line-$it" }) { n ->
          DiffLineRow(lines[n], texts?.getOrNull(n), gutter)
        }
      }
    }
    item(key = "file-$index-gap") { Spacer(Modifier.height(12.dp).fillMaxWidth().background(Gl.colors.sunken)) }
  }
}

/** Parsed lines and their highlighted text, filled in file by file off the main thread. */
class PreparedDiffs {
  val parsed = mutableStateMapOf<Int, List<DiffLine>>()
  val texts = mutableStateMapOf<Int, List<AnnotatedString>>()
}

/**
 * Parses every file first (plain text shows at once), then highlights them in order, so a change
 * touching hundreds of files never blocks the screen.
 */
@Composable
fun rememberPreparedDiffs(diffs: List<GitLabFileDiff>): PreparedDiffs {
  val colors = Gl.colors
  val holder = remember(diffs, colors) { PreparedDiffs() }
  LaunchedEffect(holder) {
    diffs.forEachIndexed { index, diff ->
      holder.parsed[index] = withContext(Dispatchers.Default) { parseUnifiedDiff(diff.diff) }
    }
    diffs.forEachIndexed { index, diff ->
      val lines = holder.parsed[index] ?: return@forEachIndexed
      val path = diff.newPath.ifEmpty { diff.oldPath }
      holder.texts[index] = withContext(Dispatchers.Default) { prepareDiffLines(lines, path, colors) }
    }
  }
  return holder
}

private const val DIFF_PREVIEW_LINES = 120

/** Unified diff for a single file, inline in a page. Long files start collapsed to a preview. */
@Composable
fun DiffViewer(diffText: String, modifier: Modifier = Modifier, path: String = "") {
  val c = Gl.colors
  val lines = remember(diffText) { parseUnifiedDiff(diffText) }
  var expanded by rememberSaveable(diffText.hashCode()) { mutableStateOf(false) }
  val visible = if (expanded || lines.size <= DIFF_PREVIEW_LINES) lines else lines.take(DIFF_PREVIEW_LINES)
  var texts by remember(diffText, c, expanded) { mutableStateOf<List<AnnotatedString>?>(null) }
  LaunchedEffect(diffText, c, expanded) {
    texts = withContext(Dispatchers.Default) { prepareDiffLines(visible, path, c) }
  }
  val gutter = diffGutterWidth(lines.maxOfOrNull { maxOf(it.oldNo ?: 0, it.newNo ?: 0) } ?: 0)
  Column(modifier = modifier.fillMaxWidth()) {
    visible.forEachIndexed { index, line -> DiffLineRow(line, texts?.getOrNull(index), gutter) }
    if (lines.size > DIFF_PREVIEW_LINES) {
      GlButton(
        text = if (expanded) "Show fewer lines" else "Show all ${lines.size} lines",
        onClick = { expanded = !expanded },
        kind = GlButtonKind.Ghost,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}
