package com.cboyxd.gitlab.ui.screens

import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import com.cboyxd.gitlab.ui.components.ListsDropdownFilter
import com.cboyxd.gitlab.ui.components.ListsFilterBar
import com.cboyxd.gitlab.ui.components.ListsSearchField
import com.cboyxd.gitlab.ui.components.projectLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CallMerge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.data.model.GitLabMergeRequest
import com.cboyxd.gitlab.data.model.GitLabProject
import com.cboyxd.gitlab.ui.components.EmptyState
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.GlButton
import com.cboyxd.gitlab.ui.components.GlButtonKind
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlFilterRow
import com.cboyxd.gitlab.ui.components.GlIconAction
import com.cboyxd.gitlab.ui.components.GlTopBar
import com.cboyxd.gitlab.ui.components.InlineError
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.components.MergeRequestRow
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.Loadable

private enum class MergeRequestFilter(val label: String, val emptyTitle: String) {
  OPEN("Open", "No open merge requests"),
  MERGED("Merged", "No merged merge requests"),
  CLOSED("Closed", "No closed merge requests"),
  ALL("All", "No merge requests yet");

  fun matches(mr: GitLabMergeRequest): Boolean = when (this) {
    OPEN -> mr.state == "opened"
    MERGED -> mr.state == "merged"
    CLOSED -> mr.state == "closed"
    ALL -> true
  }
}

@Composable
fun MergeRequestsScreen(
  mergeRequests: Loadable<List<GitLabMergeRequest>>,
  projects: List<GitLabProject>,
  onSelectMergeRequest: (GitLabMergeRequest) -> Unit,
  onLoadBranches: suspend (Long, String) -> Result<List<com.cboyxd.gitlab.data.model.GitLabBranch>>,
  onCreateMergeRequest: (
    projectId: Long,
    title: String,
    source: String,
    target: String,
    description: String?,
    onResult: (String?) -> Unit,
  ) -> Unit,
  onRetry: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var filter by rememberSaveable { mutableStateOf(MergeRequestFilter.OPEN) }
  var project by rememberSaveable { mutableStateOf(ALL_PROJECTS) }
  var searching by rememberSaveable { mutableStateOf(false) }
  var query by rememberSaveable { mutableStateOf("") }
  var showCreate by rememberSaveable { mutableStateOf(false) }
  val all = mergeRequests.data
  val projectOptions = listOf(ALL_PROJECTS) + all.mapNotNull { projectLabel(it.references?.full, it.webUrl) }.distinct().sorted()
  val inProject = all.filter { project == ALL_PROJECTS || projectLabel(it.references?.full, it.webUrl) == project }
  val visible = inProject.filter { filter.matches(it) && it.matchesQuery(query) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Gl.colors.bg),
  ) {
    GlTopBar(
      title = "Merge requests",
      onBack = onBack,
      actions = {
        GlIconAction(
          icon = if (searching) Icons.Outlined.SearchOff else Icons.Outlined.Search,
          contentDescription = if (searching) "Close search" else "Search merge requests",
          onClick = {
            searching = !searching
            if (!searching) query = ""
          },
        )
        GlIconAction(Icons.Outlined.Add, "New merge request", onClick = { showCreate = true })
      },
    )
    if (searching) {
      ListsSearchField(
        query = query,
        onChange = { query = it },
        placeholder = "Filter by title, number or branch",
        modifier = Modifier.fillMaxWidth().padding(horizontal = GlSpace.margin, vertical = 4.dp),
      )
    }
    ListsFilterBar {
      ListsDropdownFilter(
        name = "State",
        options = MergeRequestFilter.entries.toList(),
        selected = filter,
        label = { it.label },
        onSelect = { filter = it },
        countOf = { option -> inProject.count { option.matches(it) } },
      )
      if (projectOptions.size > 2) {
        ListsDropdownFilter(
          name = "Project",
          options = projectOptions,
          selected = project,
          label = { it },
          onSelect = { project = it },
          active = project != ALL_PROJECTS,
        )
      }
    }
    GlDivider()
    Box(modifier = Modifier.fillMaxSize()) {
      when {
        mergeRequests.initialLoading -> LoadingState(label = "Loading merge requests")
        mergeRequests.error != null && all.isEmpty() -> ErrorState(message = mergeRequests.error, onRetry = onRetry)
        else -> LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = GlSpace.group),
        ) {
          if (mergeRequests.error != null) {
            item(key = "error") {
              InlineError(
                message = mergeRequests.error,
                onRetry = onRetry,
                modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = GlSpace.tight),
              )
            }
          }
          if (visible.isEmpty()) {
            item(key = "empty") {
              EmptyState(
                icon = Icons.Outlined.CallMerge,
                title = if (query.isNotBlank()) "No merge requests match “${query.trim()}”" else filter.emptyTitle,
                body = "Merge requests assigned to you, created by you or awaiting your review show up here.",
                actionLabel = "New merge request",
                onAction = { showCreate = true },
              )
            }
          } else {
            items(visible, key = { "${it.projectId}-${it.iid}" }) { mr ->
              Column {
                MergeRequestRow(mr = mr, onClick = { onSelectMergeRequest(mr) })
                GlDivider()
              }
            }
          }
        }
      }
    }
  }

  if (showCreate) {
    CreateMergeRequestSheet(
      projects = projects,
      onCreate = onCreateMergeRequest,
      onLoadBranches = onLoadBranches,
      onDismiss = { showCreate = false },
    )
  }
}

private const val ALL_PROJECTS = "All projects"

private fun GitLabMergeRequest.matchesQuery(query: String): Boolean {
  val q = query.trim().removePrefix("!")
  return q.isEmpty() || title.contains(q, ignoreCase = true) || iid.toString() == q ||
    sourceBranch.contains(q, ignoreCase = true)
}
