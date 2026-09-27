package com.aistudio.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistudio.gitlab.data.model.GitLabPipeline
import com.aistudio.gitlab.ui.components.EmptyState
import com.aistudio.gitlab.ui.components.ErrorState
import com.aistudio.gitlab.ui.components.GlFilterRow
import com.aistudio.gitlab.ui.components.GlIconAction
import com.aistudio.gitlab.ui.components.GlTopBar
import com.aistudio.gitlab.ui.components.InlineError
import com.aistudio.gitlab.ui.components.LoadingState
import com.aistudio.gitlab.ui.components.PipelineCard
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlSpace
import com.aistudio.gitlab.ui.viewmodel.Loadable

private val RunningStatuses = setOf("running", "pending", "created", "preparing", "waiting_for_resource")

private enum class PipelineFilter(val label: String, val emptyTitle: String) {
  ALL("All", "No pipelines yet"),
  FAILED("Failed", "No failed pipelines"),
  RUNNING("Running", "Nothing running"),
  PASSED("Passed", "No passed pipelines");

  fun matches(pipeline: GitLabPipeline): Boolean = when (this) {
    ALL -> true
    FAILED -> pipeline.status == "failed"
    RUNNING -> pipeline.status in RunningStatuses
    PASSED -> pipeline.status == "success"
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipelinesScreen(
  pipelines: Loadable<List<GitLabPipeline>>,
  refreshing: Boolean,
  onRefresh: () -> Unit,
  onRetryPipeline: (GitLabPipeline) -> Unit,
  onOpenPipeline: (GitLabPipeline) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var filter by rememberSaveable { mutableStateOf(PipelineFilter.ALL) }
  val all = pipelines.data
  val visible = all.filter { filter.matches(it) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Gl.colors.bg),
  ) {
    GlTopBar(
      title = "Pipelines",
      subtitle = "Recent pipelines across your projects",
      onBack = onBack,
      actions = {
        GlIconAction(icon = Icons.Outlined.Refresh, contentDescription = "Refresh", onClick = onRefresh, enabled = !refreshing)
      },
    )
    GlFilterRow(
      options = PipelineFilter.entries.toList(),
      selected = filter,
      label = { option -> "${option.label} ${all.count { option.matches(it) }}" },
      onSelect = { filter = it },
    )
    PullToRefreshBox(
      isRefreshing = refreshing,
      onRefresh = onRefresh,
      modifier = Modifier.fillMaxSize(),
    ) {
      when {
        pipelines.initialLoading -> LoadingState(label = "Loading pipelines")
        pipelines.error != null && all.isEmpty() -> ErrorState(message = pipelines.error, onRetry = onRefresh)
        else -> LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = GlSpace.margin, end = GlSpace.margin, top = GlSpace.tight, bottom = GlSpace.group),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (pipelines.error != null) {
            item(key = "error") {
              InlineError(message = pipelines.error, onRetry = onRefresh)
            }
          }
          if (visible.isEmpty()) {
            item(key = "empty") {
              EmptyState(
                icon = Icons.Outlined.PlayCircleOutline,
                title = filter.emptyTitle,
                body = "Pipelines from your most active projects appear here.",
                actionLabel = "Refresh",
                onAction = onRefresh,
              )
            }
          } else {
            items(visible, key = { "${it.projectId}-${it.id}" }) { pipeline ->
              PipelineCard(pipeline = pipeline, onRetry = onRetryPipeline, onClick = { onOpenPipeline(pipeline) })
            }
          }
        }
      }
    }
  }
}
