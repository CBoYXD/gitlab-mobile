package com.cboyxd.gitlab.ui.screens

import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import com.cboyxd.gitlab.ui.components.ListsDropdownFilter
import com.cboyxd.gitlab.ui.components.ListsFilterBar
import com.cboyxd.gitlab.ui.components.ListsSearchField
import com.cboyxd.gitlab.ui.components.ListsOverflowMenu
import com.cboyxd.gitlab.ui.components.projectLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CallMerge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
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
import com.cboyxd.gitlab.ui.components.MarkdownEditor
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
        ListsOverflowMenu(listOf("New merge request" to { showCreate = true }))
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
    CreateMergeRequestDialog(
      projects = projects,
      onCreate = onCreateMergeRequest,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateMergeRequestDialog(
  projects: List<GitLabProject>,
  onCreate: (
    projectId: Long,
    title: String,
    source: String,
    target: String,
    description: String?,
    onResult: (String?) -> Unit,
  ) -> Unit,
  onDismiss: () -> Unit,
) {
  val initial = projects.firstOrNull()
  var projectId by rememberSaveable { mutableStateOf(initial?.id ?: 0L) }
  var title by rememberSaveable { mutableStateOf("") }
  var source by rememberSaveable { mutableStateOf("") }
  var target by rememberSaveable { mutableStateOf(initial?.defaultBranch ?: "main") }
  var description by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
  var titleMissing by remember { mutableStateOf(false) }
  var sourceMissing by remember { mutableStateOf(false) }
  var targetMissing by remember { mutableStateOf(false) }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  val busyState = rememberUpdatedState(busy)
  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true,
    confirmValueChange = { !busyState.value },
  )

  ModalBottomSheet(
    onDismissRequest = { if (!busy) onDismiss() },
    sheetState = sheetState,
    containerColor = Gl.colors.surface,
  ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .imePadding()
          .navigationBarsPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = GlSpace.margin)
          .padding(bottom = GlSpace.group),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text("New merge request", style = Gl.type.title, color = Gl.colors.textPrimary)
        ProjectPicker(
          projects = projects,
          selectedId = projectId,
          enabled = !busy,
          onSelect = { project ->
            projectId = project.id
            target = project.defaultBranch ?: "main"
            targetMissing = false
          },
        )
        OutlinedTextField(
          value = title,
          onValueChange = { title = it; titleMissing = false },
          label = { Text("Title") },
          singleLine = true,
          enabled = !busy,
          isError = titleMissing,
          supportingText = if (titleMissing) ({ Text("Enter a title") }) else null,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = source,
          onValueChange = { source = it; sourceMissing = false },
          label = { Text("Source branch") },
          placeholder = { Text("feature/login") },
          singleLine = true,
          enabled = !busy,
          isError = sourceMissing,
          supportingText = if (sourceMissing) ({ Text("Enter the branch with your changes") }) else null,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Next),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = target,
          onValueChange = { target = it; targetMissing = false },
          label = { Text("Target branch") },
          singleLine = true,
          enabled = !busy,
          isError = targetMissing,
          supportingText = if (targetMissing) ({ Text("Enter the branch to merge into") }) else null,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Next),
          modifier = Modifier.fillMaxWidth(),
        )
        MarkdownEditor(
          value = description,
          onValueChange = { description = it },
          label = "Description",
          minLines = 4,
          enabled = !busy,
        )
        error?.let { InlineError(message = it) }
      GlButton(
        text = "Create merge request",
        loading = busy,
        enabled = projects.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        onClick = {
          titleMissing = title.isBlank()
          sourceMissing = source.isBlank()
          targetMissing = target.isBlank()
          if (titleMissing || sourceMissing || targetMissing) return@GlButton
          busy = true
          error = null
          onCreate(projectId, title.trim(), source.trim(), target.trim(), description.text.ifBlank { null }) { result ->
            busy = false
            if (result == null) onDismiss() else error = result
          }
        },
      )
      GlButton(
        text = "Cancel",
        kind = GlButtonKind.Ghost,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
        onClick = onDismiss,
      )
      }
  }
}
