package com.cboyxd.gitlab.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.sp
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
  var inHunk = false
  val raw = diff.replace("\t", "    ").lines().let { if (it.lastOrNull()?.isEmpty() == true) it.dropLast(1) else it }
  for (line in raw) {
    when {
      line.startsWith("@@") -> {
        inHunk = true
        HUNK.matchEntire(line)?.let { m ->
          oldNo = m.groupValues[1].toInt()
          newNo = m.groupValues[2].toInt()
        }
        out += DiffLine(DiffKind.Hunk, null, null, line)
      }
      line.startsWith("\\") -> out += DiffLine(DiffKind.Hunk, null, null, line.removePrefix("\\ "))
      !inHunk -> Unit
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
    var inHunk = false
    d.diff.lineSequence().forEach { line ->
      when {
        line.startsWith("@@") -> inHunk = true
        inHunk && line.startsWith("+") -> added++
        inHunk && line.startsWith("-") -> removed++
      }
    }
  }
  return DiffStats(added, removed)
}

/** Highlight the old and new side of each hunk together, retaining multiline syntax state. */
fun prepareDiffLines(lines: List<DiffLine>, path: String, colors: GlColors): List<AnnotatedString> {
  val language = languageForPath(path)
  val result = lines.map { AnnotatedString(it.text) }.toMutableList()
  if (lines.sumOf { it.text.length } < 60_000) {
    var start = 0
    while (start < lines.size) {
      if (lines[start].kind == DiffKind.Hunk) { start++; continue }
      var end = start
      while (end < lines.size && lines[end].kind != DiffKind.Hunk) end++
      for (oldSide in listOf(true, false)) {
        val indices = (start until end).filter {
          lines[it].kind != if (oldSide) DiffKind.Add else DiffKind.Remove
        }
        val highlighted = highlight(indices.joinToString("\n") { lines[it].text }, language, colors).splitLines()
        indices.forEachIndexed { n, index -> result[index] = highlighted.getOrElse(n) { AnnotatedString(lines[index].text) } }
      }
      start = end
    }
  }
  return lines.mapIndexed { index, line ->
    val base = result[index]
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
 * Unified diff with old/new line numbers, syntax colors and an optional shared horizontal scroll.
 */
@Composable
fun DiffLineRow(
  line: DiffLine, text: AnnotatedString?, gutter: Dp, modifier: Modifier = Modifier,
  scrollState: ScrollState? = null, codeWidth: Dp = 0.dp,
) {
  val c = Gl.colors
  if (line.kind == DiffKind.Hunk) {
    Text(
      text = line.text,
      style = Gl.type.monoSmall,
      color = c.accent,
      modifier = modifier
        .fillMaxWidth()
        .background(c.diffHunkBg)
        .padding(start = gutter * 2 + 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
    )
    return
  }
  val (bg, marker) = when (line.kind) {
    DiffKind.Add -> c.diffAddBg to "+"
    DiffKind.Remove -> c.diffRemoveBg to "−"
    else -> Color.Transparent to " "
  }
  Row(
    modifier = modifier
      .fillMaxWidth()
      .then(if (scrollState != null) Modifier.horizontalScroll(scrollState).width(codeWidth) else Modifier)
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
    for ((number, tone) in listOf(line.oldNo to c.danger, line.newNo to c.success)) {
      Text(
        text = number?.toString().orEmpty(),
        style = Gl.type.monoSmall.copy(fontSize = 11.sp),
        color = if (line.kind == DiffKind.Context) c.textTertiary else tone,
        maxLines = 1,
        modifier = Modifier.width(gutter).padding(start = 2.dp, end = 6.dp, top = 3.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.End,
      )
    }
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
      style = Gl.type.mono.copy(fontSize = 12.sp, lineHeight = 18.sp, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)),
      color = c.textPrimary,
      softWrap = scrollState == null,
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
      text = shown,
      style = Gl.type.mono,
      color = c.textPrimary,
      maxLines = 3,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f).semantics { contentDescription = shown },
    )
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 6.dp)) {
      when {
        diff.newFile -> GlBadge("New", tone = Tone.Success)
        diff.deletedFile -> GlBadge("Deleted", tone = Tone.Danger)
        diff.renamedFile -> GlBadge("Renamed", tone = Tone.Info)
        diff.generatedFile -> GlBadge("Generated")
      }
      val stats = remember(diff) { diffStats(listOf(diff)) }
      Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
        Text("+${stats.added}", style = Gl.type.monoSmall, color = c.success)
        Text("−${stats.removed}", style = Gl.type.monoSmall, color = c.danger)
      }
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
  wrapLines: Boolean = true,
  selectedFile: Int? = null,
) {
  diffs.forEachIndexed { index, diff ->
    if (selectedFile != null && selectedFile != index) return@forEachIndexed
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
          when {
            diff.tooLarge -> "GitLab excluded this diff because it is too large. View the file in GitLab."
            diff.collapsed -> "GitLab omitted this diff. Open it in GitLab to load the changes."
            diff.renamedFile -> "File renamed without text changes."
            else -> "No text diff available (binary file or metadata-only change)."
          },
          style = Gl.type.meta,
          color = Gl.colors.textSecondary,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
      }
      lines == null -> item(key = "file-$index-loading") { LoadingState(label = "Loading changes") }
      else -> {
        val gutter = diffGutterWidth(lines.maxOfOrNull { maxOf(it.oldNo ?: 0, it.newNo ?: 0) } ?: 0)
        val texts = prepared[index]
        val maxColumns = (lines.maxOfOrNull { it.text.length } ?: 0).coerceAtMost(10_000)
        items(lines.size, key = { "file-$index-line-$it" }) { n ->
          val scroll = if (wrapLines) null else LocalDiffScrollStates.current.getOrPut(index) { ScrollState(0) }
          val screenWidth = LocalConfiguration.current.screenWidthDp.dp
          val width = remember(lines, screenWidth, gutter) {
            maxColumns.times(8).dp.plus(gutter * 2 + 36.dp).coerceAtLeast(screenWidth)
          }
          DiffLineRow(lines[n], texts?.getOrNull(n), gutter, scrollState = scroll, codeWidth = width)
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

val LocalDiffScrollStates = androidx.compose.runtime.compositionLocalOf { mutableMapOf<Int, ScrollState>() }

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
