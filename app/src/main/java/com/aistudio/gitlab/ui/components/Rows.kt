package com.aistudio.gitlab.ui.components

import androidx.compose.foundation.clickable
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
import com.aistudio.gitlab.data.model.GitLabCommit
import com.aistudio.gitlab.data.model.GitLabIssue
import com.aistudio.gitlab.data.model.GitLabLabel
import com.aistudio.gitlab.data.model.GitLabMergeRequest
import com.aistudio.gitlab.data.model.GitLabPipeline
import com.aistudio.gitlab.data.model.GitLabStage
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlSpace

/** Issue in a list: state icon, title, reference and meta, labels. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IssueRow(issue: GitLabIssue, onClick: () -> Unit, modifier: Modifier = Modifier, labels: Map<String, GitLabLabel> = emptyMap()) {
  val style = issueStateStyle(issue.state)
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open issue")
      .defaultMinSize(minHeight = 64.dp)
      .padding(horizontal = GlSpace.margin, vertical = 12.dp),
    verticalAlignment = Alignment.Top,
  ) {
    StateIcon(style, Modifier.padding(top = 1.dp))
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(issue.title, style = Gl.type.rowTitle, color = Gl.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
      Spacer(Modifier.height(2.dp))
      Text(
        text = listOfNotNull(
          "#${issue.iid}",
          issue.updatedAt.takeIf { it.isNotBlank() }?.let { "updated $it" },
          issue.author.name.takeIf { it.isNotBlank() }?.let { "by $it" },
        ).joinToString(" · "),
        style = Gl.type.meta,
        color = Gl.colors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (issue.labels.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        LabelRow(issue.labels, labels + issue.labelDetails.associateBy { it.name }, max = 4)
      }
    }
    CommentCount(issue.userNotesCount)
  }
}

@Composable
fun MergeRequestRow(mr: GitLabMergeRequest, onClick: () -> Unit, modifier: Modifier = Modifier, labels: Map<String, GitLabLabel> = emptyMap()) {
  val style = mergeRequestStateStyle(mr.state, mr.isDraft)
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open merge request")
      .defaultMinSize(minHeight = 64.dp)
      .padding(horizontal = GlSpace.margin, vertical = 12.dp),
    verticalAlignment = Alignment.Top,
  ) {
    StateIcon(style, Modifier.padding(top = 1.dp))
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(mr.title, style = Gl.type.rowTitle, color = Gl.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
      Spacer(Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (mr.isDraft) {
          GlBadge("Draft")
          Spacer(Modifier.width(6.dp))
        }
        MonoPill(
          text = "${mr.sourceBranch} → ${mr.targetBranch}",
          modifier = Modifier.weight(1f, fill = false).semantics { contentDescription = "from ${mr.sourceBranch} into ${mr.targetBranch}" },
        )
      }
      Spacer(Modifier.height(4.dp))
      Text(
        text = listOfNotNull(
          "!${mr.iid}",
          mr.updatedAt.takeIf { it.isNotBlank() }?.let { "updated $it" },
          mr.author.name.takeIf { it.isNotBlank() }?.let { "by $it" },
        ).joinToString(" · "),
        style = Gl.type.meta,
        color = Gl.colors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (mr.labels.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        LabelRow(mr.labels, labels + mr.labelDetails.associateBy { it.name }, max = 4)
      }
    }
    mr.headPipeline?.let { pipeline ->
      Spacer(Modifier.width(8.dp))
      PipelineStatusBadge(pipeline.status, showLabel = false, modifier = Modifier.padding(top = 3.dp))
    }
    CommentCount(mr.userNotesCount)
  }
}

@Composable
private fun CommentCount(count: Int) {
  if (count <= 0) return
  Spacer(Modifier.width(8.dp))
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .padding(top = 2.dp)
      .semantics(mergeDescendants = true) { contentDescription = if (count == 1) "1 comment" else "$count comments" },
  ) {
    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = Gl.colors.textTertiary, modifier = Modifier.size(16.dp))
    Spacer(Modifier.width(4.dp))
    Text(count.toString(), style = Gl.type.numeric, color = Gl.colors.textSecondary)
  }
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

@Composable
fun CommitRow(commit: GitLabCommit, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open commit") else Modifier.semantics(mergeDescendants = true) {})
      .defaultMinSize(minHeight = 56.dp)
      .padding(horizontal = GlSpace.margin, vertical = 10.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(commit.title, style = Gl.type.rowTitle, color = Gl.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
      Spacer(Modifier.height(2.dp))
      Text(
        text = listOf(commit.authorName, commit.createdAt).filter { it.isNotBlank() }.joinToString(" · "),
        style = Gl.type.meta,
        color = Gl.colors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(Modifier.width(8.dp))
    MonoPill(commit.shortId)
  }
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
