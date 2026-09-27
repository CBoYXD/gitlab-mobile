package com.cboyxd.gitlab.ui.components

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
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.cboyxd.gitlab.ui.theme.GlTile
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
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.theme.TanukiLightOrange
import com.cboyxd.gitlab.ui.theme.TanukiOrange
import com.cboyxd.gitlab.ui.theme.TanukiRed

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

/** GitLab's issue states: open is green, closed is blue. */
fun issueStateStyle(state: String): StatusStyle =
  if (state == "opened") StatusStyle("Open", Tone.Success, Icons.Outlined.Adjust)
  else StatusStyle("Closed", Tone.Info, Icons.Outlined.TaskAlt)

/** GitLab's merge request states: open green, merged purple, closed red, draft gray. */
fun mergeRequestStateStyle(state: String, draft: Boolean = false): StatusStyle = when (state) {
  "merged" -> StatusStyle("Merged", Tone.Merged, Icons.Outlined.MergeType)
  "closed" -> StatusStyle("Closed", Tone.Danger, Icons.Outlined.Block)
  "locked" -> StatusStyle("Locked", Tone.Neutral, Icons.Outlined.Lock)
  else -> if (draft) StatusStyle("Draft", Tone.Neutral, Icons.Outlined.MergeType) else StatusStyle("Open", Tone.Success, Icons.Outlined.MergeType)
}

@Composable
fun StateIcon(style: StatusStyle, modifier: Modifier = Modifier, size: Dp = 22.dp) {
  val (fg, _) = toneColors(style.tone)
  Icon(style.icon, contentDescription = style.label, tint = fg, modifier = modifier.size(size))
}

/** Solid state pill with a white icon and label, as GitHub shows "Open" under a title. */
@Composable
fun StateBadge(style: StatusStyle, modifier: Modifier = Modifier) {
  val c = Gl.colors
  val fill = when (style.tone) {
    Tone.Success -> c.successSolid
    Tone.Merged -> c.mergedSolid
    Tone.Danger -> c.dangerSolid
    Tone.Info -> c.primarySolid
    Tone.Warning -> c.warning
    Tone.Neutral -> GlTile.gray
  }
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(50))
      .background(fill)
      .padding(start = 10.dp, end = 12.dp, top = 5.dp, bottom = 5.dp)
      .semantics(mergeDescendants = true) { contentDescription = style.label },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(style.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    androidx.compose.foundation.layout.Spacer(Modifier.width(5.dp))
    Text(style.label, style = Gl.type.label, color = Color.White, maxLines = 1)
  }
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

