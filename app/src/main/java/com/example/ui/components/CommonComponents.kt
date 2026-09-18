package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GitLabDanger
import com.example.ui.theme.GitLabInfo
import com.example.ui.theme.GitLabLightOrange
import com.example.ui.theme.GitLabMerged
import com.example.ui.theme.GitLabOrange
import com.example.ui.theme.GitLabPurple
import com.example.ui.theme.GitLabRed
import com.example.ui.theme.GitLabSuccess
import com.example.ui.theme.GitLabWarning

/**
 * Geometric GitLab Tanuki Mascot Icon rendered via pure Canvas vector paths.
 */
@Composable
fun GitLabTanukiIcon(
  modifier: Modifier = Modifier,
  size: Dp = 28.dp
) {
  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height

    // Center bottom point
    val bottomX = w * 0.5f
    val bottomY = h * 0.95f

    // Cheeks
    val leftCheekX = w * 0.12f
    val rightCheekX = w * 0.88f
    val cheekY = h * 0.44f

    // Inner eye points
    val innerLeftX = w * 0.38f
    val innerRightX = w * 0.62f
    val innerY = h * 0.44f

    // Ear tips
    val leftEarTipX = w * 0.28f
    val rightEarTipX = w * 0.72f
    val earTipY = h * 0.10f

    // Center nose/chin facet (Deep Red/Orange)
    val centerPath = Path().apply {
      moveTo(bottomX, bottomY)
      lineTo(innerLeftX, innerY)
      lineTo(innerRightX, innerY)
      close()
    }
    drawPath(centerPath, color = GitLabRed)

    // Left flank (Vibrant Orange)
    val leftFlank = Path().apply {
      moveTo(bottomX, bottomY)
      lineTo(innerLeftX, innerY)
      lineTo(leftCheekX, cheekY)
      close()
    }
    drawPath(leftFlank, color = GitLabOrange)

    // Right flank (Vibrant Orange)
    val rightFlank = Path().apply {
      moveTo(bottomX, bottomY)
      lineTo(innerRightX, innerY)
      lineTo(rightCheekX, cheekY)
      close()
    }
    drawPath(rightFlank, color = GitLabOrange)

    // Left ear (Gold/Light Orange)
    val leftEar = Path().apply {
      moveTo(leftCheekX, cheekY)
      lineTo(leftEarTipX, earTipY)
      lineTo(innerLeftX, innerY)
      close()
    }
    drawPath(leftEar, color = GitLabLightOrange)

    // Right ear (Gold/Light Orange)
    val rightEar = Path().apply {
      moveTo(rightCheekX, cheekY)
      lineTo(rightEarTipX, earTipY)
      lineTo(innerRightX, innerY)
      close()
    }
    drawPath(rightEar, color = GitLabLightOrange)
  }
}

/**
 * CI/CD Pipeline status pill badge with official GitLab pipeline status colors and icons.
 */
@Composable
fun PipelineStatusBadge(
  status: String,
  modifier: Modifier = Modifier,
  showLabel: Boolean = true
) {
  val (bgColor, textColor, icon) = when (status.lowercase()) {
    "success", "passed" -> Triple(
      Color(0xFFE6F4EA),
      GitLabSuccess,
      Icons.Filled.CheckCircle
    )
    "running" -> Triple(
      Color(0xFFE8F0FE),
      GitLabInfo,
      Icons.Filled.Refresh
    )
    "failed" -> Triple(
      Color(0xFFFDE8E8),
      GitLabDanger,
      Icons.Filled.Error
    )
    "canceled" -> Triple(
      Color(0xFFF1F1F4),
      Color(0xFF6E6D7A),
      Icons.Filled.Block
    )
    "pending" -> Triple(
      Color(0xFFFEF3C7),
      GitLabWarning,
      Icons.Filled.HourglassEmpty
    )
    else -> Triple(
      Color(0xFFF1F1F4),
      Color(0xFF89888D),
      Icons.Filled.PlayCircle
    )
  }

  val infiniteTransition = rememberInfiniteTransition(label = "spin")
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotation"
  )

  Surface(
    modifier = modifier,
    color = bgColor,
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      val iconModifier = if (status.lowercase() == "running") {
        Modifier.size(13.dp).rotate(rotation)
      } else {
        Modifier.size(13.dp)
      }

      Icon(
        imageVector = icon,
        contentDescription = status,
        tint = textColor,
        modifier = iconModifier
      )

      if (showLabel) {
        Text(
          text = status.replaceFirstChar { it.uppercase() },
          color = textColor,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}

/**
 * Issue open/closed state indicator icon.
 */
@Composable
fun IssueStateIcon(state: String, modifier: Modifier = Modifier) {
  if (state.lowercase() == "opened") {
    Icon(
      imageVector = Icons.Outlined.Adjust,
      contentDescription = "Open issue",
      tint = GitLabSuccess,
      modifier = modifier.size(18.dp)
    )
  } else {
    Icon(
      imageVector = Icons.Outlined.CheckCircle,
      contentDescription = "Closed issue",
      tint = GitLabPurple,
      modifier = modifier.size(18.dp)
    )
  }
}

/**
 * Merge Request open/merged/closed state indicator icon.
 */
@Composable
fun MergeRequestStateIcon(state: String, modifier: Modifier = Modifier) {
  when (state.lowercase()) {
    "merged" -> Icon(
      imageVector = Icons.Outlined.DoneAll,
      contentDescription = "Merged MR",
      tint = GitLabMerged,
      modifier = modifier.size(18.dp)
    )
    "closed" -> Icon(
      imageVector = Icons.Filled.Block,
      contentDescription = "Closed MR",
      tint = GitLabDanger,
      modifier = modifier.size(18.dp)
    )
    else -> Icon(
      imageVector = Icons.Outlined.MergeType,
      contentDescription = "Open MR",
      tint = GitLabSuccess,
      modifier = modifier.size(18.dp)
    )
  }
}

/**
 * GitLab Label Badge (e.g. ~frontend, ~bug, ~CI/CD).
 */
@Composable
fun GitLabLabelChip(
  label: String,
  modifier: Modifier = Modifier
) {
  val (bgColor, textColor) = when {
    label.contains("bug", ignoreCase = true) || label.contains("critical", ignoreCase = true) ->
      Pair(Color(0xFFFDE8E8), Color(0xFFC81E1E))
    label.contains("feature", ignoreCase = true) || label.contains("enhancement", ignoreCase = true) ->
      Pair(Color(0xFFE1EFFE), Color(0xFF1E429F))
    label.contains("ci/cd", ignoreCase = true) || label.contains("devops", ignoreCase = true) ->
      Pair(Color(0xFFEDEBFE), Color(0xFF5521B5))
    label.contains("k8s", ignoreCase = true) || label.contains("kubernetes", ignoreCase = true) ->
      Pair(Color(0xFFE6F4EA), Color(0xFF0F766E))
    else ->
      Pair(Color(0xFFF3F4F6), Color(0xFF4B5563))
  }

  Surface(
    modifier = modifier,
    color = bgColor,
    shape = RoundedCornerShape(6.dp)
  ) {
    Text(
      text = label,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
  }
}

/**
 * Code Viewer with line numbers and monospace styling.
 */
@Composable
fun CodeViewer(
  code: String,
  modifier: Modifier = Modifier
) {
  val lines = code.lines()
  val horizontalScroll = rememberScrollState()

  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(8.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .horizontalScroll(horizontalScroll)
    ) {
      // Line numbers column
      Column(
        modifier = Modifier.padding(end = 12.dp),
        horizontalAlignment = Alignment.End
      ) {
        lines.indices.forEach { index ->
          Text(
            text = "${index + 1}",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            lineHeight = 18.sp
          )
        }
      }

      // Code content
      Column {
        lines.forEach { line ->
          Text(
            text = if (line.isEmpty()) " " else line,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 18.sp
          )
        }
      }
    }
  }
}

/**
 * Git Diff View highlighting additions (+) and deletions (-).
 */
@Composable
fun DiffViewer(
  diffText: String,
  modifier: Modifier = Modifier
) {
  val lines = diffText.lines()
  val horizontalScroll = rememberScrollState()

  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(8.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(horizontalScroll)
    ) {
      lines.forEach { line ->
        val (bgColor, textColor) = when {
          line.startsWith("+") && !line.startsWith("+++") ->
            Pair(Color(0xFF22C55E).copy(alpha = 0.15f), Color(0xFF15803D))
          line.startsWith("-") && !line.startsWith("---") ->
            Pair(Color(0xFFEF4444).copy(alpha = 0.15f), Color(0xFFB91C1C))
          line.startsWith("@@") ->
            Pair(GitLabPurple.copy(alpha = 0.10f), GitLabPurple)
          else ->
            Pair(Color.Transparent, MaterialTheme.colorScheme.onSurface)
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
          Text(
            text = if (line.isEmpty()) " " else line,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.5.sp,
            color = textColor,
            lineHeight = 16.sp
          )
        }
      }
    }
  }
}
