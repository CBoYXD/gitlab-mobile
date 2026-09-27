package com.aistudio.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aistudio.gitlab.data.model.GitLabPipeline
import com.aistudio.gitlab.ui.components.EmptyState
import com.aistudio.gitlab.ui.components.ErrorState
import com.aistudio.gitlab.ui.components.FullScreenState
import com.aistudio.gitlab.ui.components.GlCard
import com.aistudio.gitlab.ui.components.GlDivider
import com.aistudio.gitlab.ui.components.GlIconAction
import com.aistudio.gitlab.ui.components.GlSectionHeader
import com.aistudio.gitlab.ui.components.GlTopBar
import com.aistudio.gitlab.ui.components.InlineError
import com.aistudio.gitlab.ui.components.LoadingState
import com.aistudio.gitlab.ui.components.MonoPill
import com.aistudio.gitlab.ui.components.PipelineStatusBadge
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlSpace
import com.aistudio.gitlab.ui.viewmodel.CommitDetailState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommitDetailScreen(
  state: CommitDetailState,
  onRetry: () -> Unit,
  onOpenPipeline: (GitLabPipeline) -> Unit,
  onOpenInBrowser: (String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val commit = state.commit.data
  val c = Gl.colors
  Column(modifier = modifier.fillMaxSize().background(c.bg)) {
    GlTopBar(
      title = commit?.shortId ?: state.sha.take(8),
      subtitle = commit?.title,
      onBack = onBack,
    ) {
      val web = commit?.webUrl
      if (!web.isNullOrBlank()) {
        GlIconAction(Icons.AutoMirrored.Outlined.OpenInNew, "Open in browser", onClick = { onOpenInBrowser(web) })
      }
    }
    GlDivider()
    when {
      commit == null && state.commit.initialLoading -> FullScreenState { LoadingState(label = "Loading commit") }
      commit == null && state.commit.error != null -> FullScreenState { ErrorState(state.commit.error, onRetry) }
      commit == null -> FullScreenState { LoadingState(label = "Loading commit") }
      else -> LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = GlSpace.group),
      ) {
        item(key = "header") {
          GlCard(modifier = Modifier.padding(start = GlSpace.margin, end = GlSpace.margin, top = GlSpace.margin)) {
            Column(modifier = Modifier.padding(GlSpace.margin), verticalArrangement = Arrangement.spacedBy(GlSpace.tight)) {
              Text(commit.title, style = Gl.type.sectionTitle, color = c.textPrimary)
              val body = commit.message.substringAfter('\n', "").trim()
              if (body.isNotEmpty()) {
                Text(body, style = Gl.type.bodySmall, color = c.textSecondary)
              }
              Text(
                text = listOf(commit.authorName, commit.committedDate ?: commit.createdAt).filter { !it.isNullOrBlank() }.joinToString(" · "),
                style = Gl.type.meta,
                color = c.textSecondary,
              )
              if (commit.parentIds.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text("Parents", style = Gl.type.meta, color = c.textTertiary, modifier = Modifier.align(Alignment.CenterVertically))
                  commit.parentIds.forEach { MonoPill(it.take(8)) }
                }
              }
              val stats = commit.stats
              val files = state.diffs.data.size.takeIf { state.diffs.loaded }
              if (stats != null || files != null) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = listOfNotNull(
                      stats?.let { "${it.additions} additions, ${it.deletions} deletions" },
                      files?.let { if (it == 1) "1 file" else "$it files" },
                    ).joinToString(", ")
                  },
                ) {
                  if (stats != null) {
                    Text("+${stats.additions}", style = Gl.type.numeric, color = c.success)
                    Spacer(Modifier.width(8.dp))
                    Text("−${stats.deletions}", style = Gl.type.numeric, color = c.danger)
                  }
                  if (files != null) {
                    if (stats != null) Text(" · ", style = Gl.type.meta, color = c.textTertiary)
                    Text(if (files == 1) "1 file" else "$files files", style = Gl.type.meta, color = c.textSecondary)
                  }
                }
              }
            }
            commit.lastPipeline?.let { pipeline ->
              GlDivider()
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable(role = Role.Button, onClickLabel = "Open pipeline") { onOpenPipeline(pipeline) }
                  .defaultMinSize(minHeight = GlSpace.touch)
                  .padding(horizontal = GlSpace.margin, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("Pipeline", style = Gl.type.label, color = c.textPrimary, modifier = Modifier.weight(1f))
                PipelineStatusBadge(pipeline.status)
                Spacer(Modifier.width(8.dp))
                Text("#${pipeline.id}", style = Gl.type.numeric, color = c.textSecondary)
              }
            }
          }
        }
        item(key = "changes-header") {
          val count = state.diffs.data.size
          GlSectionHeader(if (state.diffs.loaded && state.diffs.error == null) "Changes ($count ${if (count == 1) "file" else "files"})" else "Changes")
        }
        val diffs = state.diffs
        when {
          diffs.initialLoading -> item(key = "diff-loading") { LoadingState(label = "Loading changes") }
          diffs.error != null && diffs.data.isEmpty() -> item(key = "diff-error") {
            InlineError(diffs.error, modifier = Modifier.padding(horizontal = GlSpace.margin), onRetry = onRetry)
          }
          diffs.data.isEmpty() -> item(key = "diff-empty") {
            EmptyState(Icons.Outlined.Difference, "No changes", "This commit doesn't change any files.")
          }
          else -> itemsIndexed(diffs.data, key = { index, d -> "$index-${d.newPath}" }) { _, diff ->
            DetailDiffCard(diff, Modifier.padding(horizontal = GlSpace.margin, vertical = 6.dp))
          }
        }
      }
    }
  }
}
