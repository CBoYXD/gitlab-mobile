package com.aistudio.gitlab.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.CallMerge
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlSpace
import com.aistudio.gitlab.ui.theme.TanukiLightOrange
import com.aistudio.gitlab.ui.theme.TanukiOrange
import com.aistudio.gitlab.ui.theme.TanukiRed

/** GitLab Tanuki mark. The only place brand orange appears. */
@Composable
fun GitLabTanukiIcon(modifier: Modifier = Modifier, size: Dp = 28.dp) {
  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val bottomX = w * 0.5f
    val bottomY = h * 0.95f
    val leftCheekX = w * 0.12f
    val rightCheekX = w * 0.88f
    val cheekY = h * 0.44f
    val innerLeftX = w * 0.38f
    val innerRightX = w * 0.62f
    val innerY = h * 0.44f
    val leftEarTipX = w * 0.28f
    val rightEarTipX = w * 0.72f
    val earTipY = h * 0.10f

    fun tri(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float) =
      Path().apply { moveTo(ax, ay); lineTo(bx, by); lineTo(cx, cy); close() }

    drawPath(tri(bottomX, bottomY, innerLeftX, innerY, innerRightX, innerY), TanukiRed)
    drawPath(tri(bottomX, bottomY, innerLeftX, innerY, leftCheekX, cheekY), TanukiOrange)
    drawPath(tri(bottomX, bottomY, innerRightX, innerY, rightCheekX, cheekY), TanukiOrange)
    drawPath(tri(leftCheekX, cheekY, leftEarTipX, earTipY, innerLeftX, innerY), TanukiLightOrange)
    drawPath(tri(rightCheekX, cheekY, rightEarTipX, earTipY, innerRightX, innerY), TanukiLightOrange)
  }
}

// ---------------------------------------------------------------------------------------------
// Status
// ---------------------------------------------------------------------------------------------

data class StatusStyle(val label: String, val tone: Tone, val icon: ImageVector, val spins: Boolean = false)

fun pipelineStatusStyle(status: String): StatusStyle = when (status.lowercase()) {
  "success", "passed" -> StatusStyle("Passed", Tone.Success, Icons.Outlined.CheckCircle)
  "failed" -> StatusStyle("Failed", Tone.Danger, Icons.Outlined.ErrorOutline)
  "running" -> StatusStyle("Running", Tone.Info, Icons.Outlined.Autorenew, spins = true)
  "pending", "created", "preparing", "scheduled", "waiting_for_resource", "waiting_for_callback" ->
    StatusStyle("Pending", Tone.Warning, Icons.Outlined.Schedule)
  "manual" -> StatusStyle("Manual", Tone.Neutral, Icons.Outlined.PanTool)
  "canceled", "canceling" -> StatusStyle("Canceled", Tone.Neutral, Icons.Outlined.Block)
  "skipped" -> StatusStyle("Skipped", Tone.Neutral, Icons.Outlined.SkipNext)
  "" -> StatusStyle("Unknown", Tone.Neutral, Icons.Outlined.RadioButtonUnchecked)
  else -> StatusStyle(status.replace('_', ' ').replaceFirstChar { it.uppercase() }, Tone.Neutral, Icons.Outlined.RadioButtonUnchecked)
}

/** Status is carried by icon and label as well as color. */
@Composable
fun PipelineStatusBadge(status: String, modifier: Modifier = Modifier, showLabel: Boolean = true) {
  val style = pipelineStatusStyle(status)
  val (fg, bg) = toneColors(style.tone)
  if (!showLabel) {
    StatusIcon(style, fg, modifier.semantics { contentDescription = style.label })
    return
  }
  Row(
    modifier = modifier
      .background(bg, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
      .padding(horizontal = 6.dp, vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    StatusIcon(style, fg, Modifier)
    androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
    Text(style.label, style = Gl.type.badge, color = fg, maxLines = 1)
  }
}

@Composable
private fun StatusIcon(style: StatusStyle, tint: Color, modifier: Modifier) {
  val spin = style.spins && !Gl.reducedMotion
  val rotation = if (spin) {
    val transition = rememberInfiniteTransition(label = "spin")
    transition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
      label = "rotation",
    ).value
  } else 0f
  Icon(style.icon, contentDescription = null, tint = tint, modifier = modifier.size(14.dp).rotate(rotation))
}

fun issueStateStyle(state: String): StatusStyle =
  if (state == "opened") StatusStyle("Open", Tone.Success, Icons.Outlined.Adjust)
  else StatusStyle("Closed", Tone.Merged, Icons.Outlined.TaskAlt)

fun mergeRequestStateStyle(state: String, draft: Boolean = false): StatusStyle = when (state) {
  "merged" -> StatusStyle("Merged", Tone.Merged, Icons.Outlined.DoneAll)
  "closed" -> StatusStyle("Closed", Tone.Danger, Icons.Outlined.Block)
  "locked" -> StatusStyle("Locked", Tone.Neutral, Icons.Outlined.Block)
  else -> if (draft) StatusStyle("Draft", Tone.Neutral, Icons.Outlined.CallMerge) else StatusStyle("Open", Tone.Success, Icons.Outlined.CallMerge)
}

@Composable
fun StateIcon(style: StatusStyle, modifier: Modifier = Modifier) {
  val (fg, _) = toneColors(style.tone)
  Icon(style.icon, contentDescription = style.label, tint = fg, modifier = modifier.size(20.dp))
}

@Composable
fun StateBadge(style: StatusStyle, modifier: Modifier = Modifier) {
  GlBadge(text = style.label, tone = style.tone, icon = style.icon, modifier = modifier)
}

@Composable
fun IssueStateIcon(state: String, modifier: Modifier = Modifier) = StateIcon(issueStateStyle(state), modifier)

@Composable
fun MergeRequestStateIcon(state: String, modifier: Modifier = Modifier, draft: Boolean = false) =
  StateIcon(mergeRequestStateStyle(state, draft), modifier)

/** GitLab label. The REST list endpoints return names only, so labels stay neutral. */
@Composable
fun GitLabLabelChip(label: String, modifier: Modifier = Modifier) {
  GlBadge(text = label, modifier = modifier)
}

// ---------------------------------------------------------------------------------------------
// Code and diffs
// ---------------------------------------------------------------------------------------------

private const val MAX_CODE_COLUMNS = 800

/**
 * Lazily rendered source view. Lines never wrap; the whole list scrolls horizontally together,
 * so a 10k-line file costs only the visible rows.
 */
@Composable
fun LazyCodeView(code: String, modifier: Modifier = Modifier, path: String = "") {
  val language = remember(path) { languageForPath(path) }
  val lines = rememberHighlightedLines(code, language)
  val longest = remember(code) { (code.lineSequence().maxOfOrNull { it.length } ?: 0).coerceAtMost(MAX_CODE_COLUMNS) }
  val gutterDigits = lines.size.toString().length
  val measurer = rememberTextMeasurer()
  val style = Gl.type.mono
  val charWidthPx = remember(style) { measurer.measure("0", style).size.width.toFloat() }
  val density = LocalDensity.current
  val gutterWidth = with(density) { (charWidthPx * gutterDigits).toDp() } + 16.dp
  val contentWidth = with(density) { (charWidthPx * (longest + 2)).toDp() } + gutterWidth + GlSpace.margin * 2
  val horizontal = rememberScrollState()
  val c = Gl.colors

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(c.surface)
      .horizontalScroll(horizontal),
  ) {
    LazyColumn(
      modifier = Modifier
        .width(contentWidth)
        .fillMaxHeight(),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
      items(lines.size) { index ->
        Row(modifier = Modifier.padding(horizontal = GlSpace.margin)) {
          Text(
            text = (index + 1).toString().padStart(gutterDigits),
            style = style,
            color = c.textTertiary,
            modifier = Modifier.width(gutterWidth),
            maxLines = 1,
            softWrap = false,
          )
          val line = lines[index]
          Text(
            text = when {
              line.length > MAX_CODE_COLUMNS -> line.subSequence(0, MAX_CODE_COLUMNS) + AnnotatedString("…")
              line.isEmpty() -> AnnotatedString(" ")
              else -> line
            },
            style = style,
            color = c.textPrimary,
            maxLines = 1,
            softWrap = false,
          )
        }
      }
    }
  }
}

private const val DIFF_PREVIEW_LINES = 120

/** Unified diff. Long files start collapsed to a preview with a control to reveal the rest. */
@Composable
fun DiffViewer(diffText: String, modifier: Modifier = Modifier, path: String = "") {
  val lines = remember(diffText) { diffText.lines() }
  var expanded by rememberSaveable(diffText.hashCode()) { mutableStateOf(false) }
  val visible = if (expanded || lines.size <= DIFF_PREVIEW_LINES) lines else lines.take(DIFF_PREVIEW_LINES)
  val c = Gl.colors
  val language = remember(path) { languageForPath(path) }
  // Highlight the code after each +/-/space marker; hunk headers stay plain.
  var highlighted by remember(diffText, language, c, expanded) { mutableStateOf<List<AnnotatedString>?>(null) }
  LaunchedEffect(diffText, language, c, expanded) {
    highlighted = withContext(Dispatchers.Default) {
      visible.map { line ->
        if (line.startsWith("@@") || line.isEmpty()) AnnotatedString(line)
        else AnnotatedString(line.take(1)) + highlight(line.drop(1), language, c)
      }
    }
  }
  val horizontal = rememberScrollState()

  Column(modifier = modifier.fillMaxWidth()) {
    Box(modifier = Modifier.fillMaxWidth().horizontalScroll(horizontal)) {
      Column(modifier = Modifier.width(IntrinsicSize.Max)) {
      visible.forEachIndexed { index, line ->
        val (bg, fg) = when {
          line.startsWith("+") && !line.startsWith("+++") -> c.successBg to c.success
          line.startsWith("-") && !line.startsWith("---") -> c.dangerBg to c.danger
          line.startsWith("@@") -> c.surface2 to c.textSecondary
          else -> Color.Transparent to c.textPrimary
        }
        val rendered = highlighted?.getOrNull(index)
        Text(
          text = if (line.isEmpty()) AnnotatedString(" ") else rendered ?: AnnotatedString(line),
          style = Gl.type.monoSmall,
          color = fg,
          softWrap = false,
          maxLines = 1,
          modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 1.dp),
        )
      }
      }
    }
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
