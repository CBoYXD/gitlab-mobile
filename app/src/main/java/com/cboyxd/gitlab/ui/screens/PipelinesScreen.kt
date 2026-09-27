package com.cboyxd.gitlab.ui.screens

import androidx.compose.foundation.layout.padding
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.ListsDropdownFilter
import com.cboyxd.gitlab.ui.components.ListsFilterBar
import com.cboyxd.gitlab.ui.components.PipelineRow
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
import com.cboyxd.gitlab.data.model.GitLabPipeline
import com.cboyxd.gitlab.ui.components.EmptyState
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.GlFilterRow
import com.cboyxd.gitlab.ui.components.GlIconAction
import com.cboyxd.gitlab.ui.components.GlTopBar
import com.cboyxd.gitlab.ui.components.InlineError
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.components.PipelineCard
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.Loadable

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
      onBack = onBack,
      actions = {
        GlIconAction(icon = Icons.Outlined.Refresh, contentDescription = "Refresh", onClick = onRefresh, enabled = !refreshing)
      },
    )
    ListsFilterBar {
      ListsDropdownFilter(
        name = "Status",
        options = PipelineFilter.entries.toList(),
        selected = filter,
        label = { it.label },
        onSelect = { filter = it },
        countOf = { option -> all.count { option.matches(it) } },
        active = filter != PipelineFilter.ALL,
      )
    }
    GlDivider()
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
          contentPadding = PaddingValues(bottom = GlSpace.group),
        ) {
          if (pipelines.error != null) {
            item(key = "error") {
              InlineError(message = pipelines.error, onRetry = onRefresh, modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = GlSpace.tight))
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
              PipelineRow(pipeline = pipeline, onRetry = onRetryPipeline, onClick = { onOpenPipeline(pipeline) })
              GlDivider()
            }
          }
        }
      }
    }
  }
}
