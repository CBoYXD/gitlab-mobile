package com.cboyxd.gitlab.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace

// ---------------------------------------------------------------------------------------------
// Code
// ---------------------------------------------------------------------------------------------

/** Longest line drawn unwrapped; past this a line is cut with an ellipsis. */
private const val MAX_CODE_COLUMNS = 800

/** Longest line drawn when wrapping; wrapped text costs layout per visual line. */
private const val MAX_WRAPPED_CHARS = 5_000

/**
 * Lazily rendered source view in the GitHub Mobile manner: a line-number gutter separated from
 * the code by a hairline, lines soft-wrapped by default (a wrapped line continues under the code
 * column, and only its first visual line carries a number). Only visible rows are composed, so a
 * 10k-line file stays cheap.
 *
 * [selection] holds 1-based line numbers and paints them with the line highlight; [onLineClick]
 * receives the 1-based number of a tapped line.
 */
@Composable
fun LazyCodeView(
  code: String,
  modifier: Modifier = Modifier,
  path: String = "",
  wrap: Boolean = true,
  selection: IntRange? = null,
  onLineClick: ((Int) -> Unit)? = null,
  listState: LazyListState = rememberLazyListState(),
) {
  val language = remember(path) { languageForPath(path) }
  val lines = rememberHighlightedLines(code, language)
  val c = Gl.colors
  val style = codeTextStyle()
  val gutterDigits = lines.size.toString().length.coerceAtLeast(2)
  val measurer = rememberTextMeasurer()
  val charWidthPx = remember(style) { measurer.measure("0", style).size.width.toFloat() }
  val density = LocalDensity.current
  // 8dp before the numbers, 10dp between them and the hairline.
  val gutterWidth = with(density) { (charWidthPx * gutterDigits).toDp() } + 18.dp
  val gutterLinePx = with(density) { gutterWidth.toPx() }
  val divider = c.border

  val rows = @Composable { index: Int, fill: Modifier ->
    val number = index + 1
    val selected = selection != null && number in selection
    Row(
      modifier = fill
        .background(if (selected) c.lineHighlight else Color.Transparent)
        .then(
          if (onLineClick != null) {
            Modifier
              .semantics { this.selected = selected }
              .clickable(role = Role.Button, onClickLabel = "Select line $number") { onLineClick(number) }
          } else Modifier,
        ),
    ) {
      Text(
        text = number.toString(),
        style = style,
        color = if (selected) c.textPrimary else c.textTertiary,
        textAlign = TextAlign.End,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.width(gutterWidth).padding(start = 8.dp, end = 10.dp),
      )
      Spacer(Modifier.width(10.dp))
      val line = lines[index]
      val limit = if (wrap) MAX_WRAPPED_CHARS else MAX_CODE_COLUMNS
      val text = when {
        line.length > limit -> line.subSequence(0, limit) + AnnotatedString("…")
        line.isEmpty() -> AnnotatedString(" ")
        else -> line
      }
      if (wrap) {
        Text(text = text, style = style, color = c.textPrimary, softWrap = true, modifier = Modifier.weight(1f).padding(end = GlSpace.margin))
      } else {
        Text(text = text, style = style, color = c.textPrimary, maxLines = 1, softWrap = false, modifier = Modifier.padding(end = GlSpace.margin))
      }
    }
  }

  // The hairline is drawn over the rows so a highlighted row doesn't hide it.
  val hairline = Modifier.drawWithContent {
    drawContent()
    drawLine(divider, Offset(gutterLinePx, 0f), Offset(gutterLinePx, size.height), strokeWidth = 1.dp.toPx())
  }

  if (wrap) {
    LazyColumn(
      state = listState,
      modifier = modifier.fillMaxSize().background(c.codeBg).then(hairline),
      contentPadding = PaddingValues(vertical = 8.dp),
    ) {
      items(lines.size) { index -> rows(index, Modifier.fillMaxWidth()) }
    }
  } else {
    val longest = remember(code) { (code.lineSequence().maxOfOrNull { it.length } ?: 0).coerceAtMost(MAX_CODE_COLUMNS) }
    BoxWithConstraints(modifier.fillMaxSize().background(c.codeBg)) {
      val contentWidth = maxOf(
        maxWidth,
        with(density) { (charWidthPx * (longest + 2)).toDp() } + gutterWidth + 10.dp + GlSpace.margin,
      )
      Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
        LazyColumn(
          state = listState,
          modifier = Modifier.width(contentWidth).fillMaxHeight().then(hairline),
          contentPadding = PaddingValues(vertical = 8.dp),
        ) {
          items(lines.size) { index -> rows(index, Modifier.fillMaxWidth()) }
        }
      }
    }
  }
}

/** Monospace at a reading size: GitHub Mobile shows code larger than list metadata. */
@Composable
fun codeTextStyle(): TextStyle = Gl.type.mono.copy(
  fontSize = 14.sp,
  lineHeight = 21.sp,
  // No trimming: a one-line row and each visual line of a wrapped row get the same height.
  lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

// ---------------------------------------------------------------------------------------------
// Text helpers
// ---------------------------------------------------------------------------------------------

/**
 * One line of text that, when too long, drops characters from the start ("…dle_parsing.rs"), so
 * the end of a file name stays readable. Compose here only ellipsizes at the end, so the fitting
 * suffix is measured by binary search.
 */
@Composable
fun StartEllipsisText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
  BoxWithConstraints(modifier) {
    val measurer = rememberTextMeasurer()
    val maxPx = constraints.maxWidth
    val shown = remember(text, style, maxPx) {
      fun fits(candidate: String) = measurer.measure(candidate, style, softWrap = false, maxLines = 1).size.width <= maxPx
      if (constraints.hasBoundedWidth.not() || fits(text)) {
        text
      } else {
        // Largest number of trailing characters that fit after the ellipsis.
        var lo = 0
        var hi = text.length
        while (lo < hi) {
          val mid = (lo + hi + 1) / 2
          if (fits("…" + text.takeLast(mid))) lo = mid else hi = mid - 1
        }
        "…" + text.takeLast(lo)
      }
    }
    Text(shown, style = style, color = color, maxLines = 1, softWrap = false)
  }
}

/** Opens the system share sheet for [text] (a link, usually). */
fun shareText(context: Context, text: String, title: String? = null) {
  val send = Intent(Intent.ACTION_SEND)
    .setType("text/plain")
    .putExtra(Intent.EXTRA_TEXT, text)
    .apply { if (title != null) putExtra(Intent.EXTRA_SUBJECT, title) }
  try {
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  } catch (_: ActivityNotFoundException) {
    // Nothing on the device can share; there is no better fallback.
  }
}
