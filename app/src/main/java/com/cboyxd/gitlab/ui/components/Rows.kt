package com.cboyxd.gitlab.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.data.model.GitLabCommit
import com.cboyxd.gitlab.data.model.GitLabIssue
import com.cboyxd.gitlab.data.model.GitLabLabel
import com.cboyxd.gitlab.data.model.GitLabMergeRequest
import com.cboyxd.gitlab.data.model.GitLabPipeline
import com.cboyxd.gitlab.data.model.GitLabStage
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace

/**
 * Issue in a list, GitHub Mobile style: state icon, "namespace / project #12" with the age on the
 * right, the title, colored labels and small counters.
 */
@Composable
fun IssueRow(issue: GitLabIssue, onClick: () -> Unit, modifier: Modifier = Modifier, labels: Map<String, GitLabLabel> = emptyMap()) {
  val style = issueStateStyle(issue.state)
  ListItemRow(
    stateIcon = { StateIcon(style) },
    reference = listOfNotNull(projectLabel(issue.references?.full, issue.webUrl), "#${issue.iid}").joinToString(" "),
    age = issue.updatedAt,
    title = issue.title,
    onClick = onClick,
    onClickLabel = "Open issue",
    modifier = modifier,
  ) {
    if (issue.labels.isNotEmpty()) {
      Spacer(Modifier.height(8.dp))
      LabelRow(issue.labels, labels + issue.labelDetails.associateBy { it.name }, max = 4)
    }
    val tasks = issue.taskStatus?.takeIf { it.count > 0 }
    if (issue.userNotesCount > 0 || tasks != null || issue.confidential) {
      Spacer(Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (issue.confidential) CountPill(Icons.Outlined.VisibilityOff, "Confidential", "Confidential")
        CommentPill(issue.userNotesCount)
        tasks?.let { CountPill(Icons.Outlined.CheckCircleOutline, "${it.completedCount}/${it.count}", "${it.completedCount} of ${it.count} tasks done") }
      }
    }
  }
}

@Composable
fun MergeRequestRow(mr: GitLabMergeRequest, onClick: () -> Unit, modifier: Modifier = Modifier, labels: Map<String, GitLabLabel> = emptyMap()) {
  val style = mergeRequestStateStyle(mr.state, mr.isDraft)
  ListItemRow(
    stateIcon = { StateIcon(style) },
    reference = listOfNotNull(projectLabel(mr.references?.full, mr.webUrl), "!${mr.iid}").joinToString(" "),
    age = mr.updatedAt,
    title = mr.title,
    onClick = onClick,
    onClickLabel = "Open merge request",
    modifier = modifier,
  ) {
    if (mr.labels.isNotEmpty()) {
      Spacer(Modifier.height(8.dp))
      LabelRow(mr.labels, labels + mr.labelDetails.associateBy { it.name }, max = 4)
    }
    val pipeline = mr.headPipeline
    if (mr.isDraft || mr.userNotesCount > 0 || pipeline != null || mr.hasConflicts == true) {
      Spacer(Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (mr.isDraft) CountPill(Icons.Outlined.EditNote, "Draft", "Draft")
        CommentPill(mr.userNotesCount)
        pipeline?.let {
          val status = pipelineStatusStyle(it.status)
          val (fg, _) = toneColors(status.tone)
          CountPill(status.icon, status.label, "Pipeline ${status.label.lowercase()}", tint = fg)
        }
        if (mr.hasConflicts == true) CountPill(Icons.Outlined.ErrorOutline, "Conflicts", "Has merge conflicts", tint = Gl.colors.danger)
      }
    }
  }
}

/** The shared shape of issue and merge request rows. */
@Composable
private fun ListItemRow(
  stateIcon: @Composable () -> Unit,
  reference: String,
  age: String,
  title: String,
  onClick: () -> Unit,
  onClickLabel: String,
  modifier: Modifier = Modifier,
  extra: @Composable ColumnScope.() -> Unit,
) {
  val c = Gl.colors
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = onClickLabel)
      .defaultMinSize(minHeight = 72.dp)
      .padding(horizontal = GlSpace.margin, vertical = 14.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Box(Modifier.padding(top = 1.dp)) { stateIcon() }
    Spacer(Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(reference, style = Gl.type.bodySmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        val shortAge = compactAge(age)
        if (shortAge.isNotBlank()) {
          Spacer(Modifier.width(12.dp))
          Text(shortAge, style = Gl.type.bodySmall, color = c.textSecondary, maxLines = 1)
        }
      }
      Spacer(Modifier.height(2.dp))
      Text(title, style = Gl.type.rowTitle.copy(fontWeight = FontWeight.Medium), color = c.textPrimary, maxLines = 4, overflow = TextOverflow.Ellipsis)
      extra()
    }
  }
}

@Composable
private fun CommentPill(count: Int) {
  if (count <= 0) return
  CountPill(Icons.Outlined.ChatBubbleOutline, count.toString(), if (count == 1) "1 comment" else "$count comments")
}

/** Pipeline summary with its stages. The retry action is only offered when it can do something. */
@Composable
fun PipelineCard(
  pipeline: GitLabPipeline,
  onRetry: ((GitLabPipeline) -> Unit)?,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
) {
  val retryable = onRetry != null && pipeline.status in setOf("failed", "canceled", "canceling")
  GlCard(modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open pipeline") else Modifier)) {
    // The ghost Retry button carries its own 48dp touch height, so the card pads less below it.
    Column(modifier = Modifier.padding(start = GlSpace.margin, end = GlSpace.margin, top = 14.dp, bottom = if (retryable) 4.dp else 14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        PipelineStatusBadge(status = pipeline.status)
        Spacer(Modifier.width(8.dp))
        Text("#${pipeline.id}", style = Gl.type.numeric, color = Gl.colors.textPrimary)
        Spacer(Modifier.weight(1f))
        pipeline.duration?.toLong()?.takeIf { it > 0 }?.let { seconds ->
          Text(formatDuration(seconds), style = Gl.type.numeric, color = Gl.colors.textSecondary)
        }
      }
      Spacer(Modifier.height(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        MonoPill(pipeline.ref.ifBlank { "unknown ref" }, Modifier.weight(1f, fill = false))
        if (pipeline.sha.isNotBlank()) {
          Spacer(Modifier.width(8.dp))
          Text(pipeline.sha.take(8), style = Gl.type.monoSmall, color = Gl.colors.textSecondary)
        }
      }
      if (pipeline.stages.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        StageStrip(pipeline.stages)
      }
      Spacer(Modifier.height(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = listOfNotNull(
            pipeline.user?.username?.takeIf { it.isNotBlank() }?.let { "@$it" },
            pipeline.createdAt.takeIf { it.isNotBlank() },
            pipeline.coverage?.takeIf { it.isNotBlank() }?.let { "coverage $it%" },
          ).joinToString(" · "),
          style = Gl.type.meta,
          color = Gl.colors.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        if (retryable && onRetry != null) {
          GlButton(text = "Retry", onClick = { onRetry(pipeline) }, kind = GlButtonKind.Ghost, icon = Icons.Outlined.Refresh)
        }
      }
    }
  }
}

@Composable
private fun StageStrip(stages: List<GitLabStage>) {
  LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    items(stages.size) { index ->
      val stage = stages[index]
      Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
          modifier = Modifier
            .semantics(mergeDescendants = true) { contentDescription = "Stage ${stage.name}, ${pipelineStatusStyle(stage.status).label}" },
          verticalAlignment = Alignment.CenterVertically,
        ) {
          PipelineStatusBadge(status = stage.status, showLabel = false)
          Spacer(Modifier.width(4.dp))
          Text(stage.name, style = Gl.type.badge, color = Gl.colors.textSecondary, maxLines = 1)
        }
        if (index < stages.lastIndex) {
          Spacer(Modifier.width(6.dp))
          Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Gl.colors.textTertiary, modifier = Modifier.size(12.dp))
        }
      }
    }
  }
}

/** Commit as GitHub Mobile lists them: the title with its age, then the author who wrote it. */
@Composable
fun CommitRow(commit: GitLabCommit, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
  val c = Gl.colors
  Column(
    modifier = modifier
      .fillMaxWidth()
      .then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open commit") else Modifier.semantics(mergeDescendants = true) {})
      .defaultMinSize(minHeight = 64.dp)
      .padding(horizontal = GlSpace.margin, vertical = 12.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(commit.title, style = Gl.type.rowTitle.copy(fontWeight = FontWeight.Medium), color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
      val age = compactAge(commit.createdAt)
      if (age.isNotBlank()) {
        Spacer(Modifier.width(12.dp))
        Text(age, style = Gl.type.bodySmall, color = c.textSecondary, maxLines = 1)
      }
    }
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      val author = commit.authorName.ifBlank { commit.committerName.orEmpty() }
      Avatar(url = null, name = author, size = 20.dp)
      Spacer(Modifier.width(8.dp))
      Text(
        buildAnnotatedString {
          withStyle(SpanStyle(color = c.textPrimary, fontWeight = FontWeight.SemiBold)) { append(author.ifBlank { "Unknown" }) }
          append(" authored")
        },
        style = Gl.type.bodySmall,
        color = c.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f, fill = false),
      )
      Spacer(Modifier.width(8.dp))
      Text(commit.shortId, style = Gl.type.monoSmall, color = c.textTertiary, maxLines = 1)
    }
  }
}

/**
 * Pipeline as a flat list row: status icon, "#id" with the ref and age, stages, who ran it. The
 * retry action is only offered when it can do something.
 */
@Composable
fun PipelineRow(
  pipeline: GitLabPipeline,
  onRetry: ((GitLabPipeline) -> Unit)?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val c = Gl.colors
  val status = pipelineStatusStyle(pipeline.status)
  val (fg, _) = toneColors(status.tone)
  val retryable = onRetry != null && pipeline.status in setOf("failed", "canceled", "canceling")
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open pipeline")
      .defaultMinSize(minHeight = 72.dp)
      .padding(start = GlSpace.margin, end = if (retryable) 4.dp else GlSpace.margin, top = 14.dp, bottom = 14.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Icon(status.icon, contentDescription = status.label, tint = fg, modifier = Modifier.padding(top = 1.dp).size(22.dp))
    Spacer(Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          listOfNotNull(pipeline.name?.takeIf { it.isNotBlank() }, "#${pipeline.id}").joinToString(" "),
          style = Gl.type.bodySmall,
          color = c.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        val age = compactAge(pipeline.createdAt)
        if (age.isNotBlank()) {
          Spacer(Modifier.width(12.dp))
          Text(age, style = Gl.type.bodySmall, color = c.textSecondary, maxLines = 1)
        }
      }
      Spacer(Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(status.label, style = Gl.type.rowTitle.copy(fontWeight = FontWeight.Medium), color = c.textPrimary)
        Spacer(Modifier.width(8.dp))
        RefPill(pipeline.ref.ifBlank { "unknown ref" }, Modifier.weight(1f, fill = false))
      }
      if (pipeline.stages.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        StageStrip(pipeline.stages)
      }
      val meta = listOfNotNull(
        pipeline.user?.let { it.name.ifBlank { it.username } }?.takeIf { it.isNotBlank() },
        pipeline.sha.takeIf { it.isNotBlank() }?.take(8),
        pipeline.duration?.toLong()?.takeIf { it > 0 }?.let(::formatDuration),
        pipeline.coverage?.takeIf { it.isNotBlank() }?.let { "coverage $it%" },
      ).joinToString(" · ")
      if (meta.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        Text(meta, style = Gl.type.meta, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
    if (retryable) {
      IconButton(onClick = { onRetry?.invoke(pipeline) }) {
        Icon(Icons.Outlined.Refresh, contentDescription = "Retry pipeline #${pipeline.id}", tint = c.accent)
      }
    }
  }
}

/** Branch or tag name as a monospace pill in the accent color, like GitHub's branch chips. */
@Composable
fun RefPill(name: String, modifier: Modifier = Modifier) {
  val c = Gl.colors
  Text(
    text = name,
    style = Gl.type.mono,
    color = c.accent,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(c.accentBg)
      .padding(horizontal = 8.dp, vertical = 2.dp),
  )
}

fun formatDuration(seconds: Long): String {
  val h = seconds / 3600
  val m = (seconds % 3600) / 60
  val s = seconds % 60
  return when {
    h > 0 -> "${h}h ${m}m"
    m > 0 -> "${m}m ${s}s"
    else -> "${s}s"
  }
}
