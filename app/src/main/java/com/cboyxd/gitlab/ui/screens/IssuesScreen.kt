package com.cboyxd.gitlab.ui.screens

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
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import com.cboyxd.gitlab.ui.components.ListsDropdownFilter
import com.cboyxd.gitlab.ui.components.ListsFilterBar
import com.cboyxd.gitlab.ui.components.ListsSearchField
import com.cboyxd.gitlab.ui.components.ListsOverflowMenu
import com.cboyxd.gitlab.ui.components.projectLabel
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.data.model.GitLabIssue
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
import com.cboyxd.gitlab.ui.components.IssueRow
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.components.MarkdownEditor
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.Loadable

private enum class IssueFilter(val label: String, val emptyTitle: String) {
  OPEN("Open", "No open issues"),
  CLOSED("Closed", "No closed issues"),
  ALL("All", "No issues yet");

  fun matches(issue: GitLabIssue): Boolean = when (this) {
    OPEN -> issue.state == "opened"
    CLOSED -> issue.state == "closed"
    ALL -> true
  }
}

@Composable
fun IssuesScreen(
  issues: Loadable<List<GitLabIssue>>,
  projects: List<GitLabProject>,
  onSelectIssue: (GitLabIssue) -> Unit,
  onCreateIssue: (projectId: Long, title: String, description: String?, labels: String?, onResult: (String?) -> Unit) -> Unit,
  onRetry: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var filter by rememberSaveable { mutableStateOf(IssueFilter.OPEN) }
  var project by rememberSaveable { mutableStateOf(ALL_PROJECTS) }
  var searching by rememberSaveable { mutableStateOf(false) }
  var query by rememberSaveable { mutableStateOf("") }
  var showCreate by rememberSaveable { mutableStateOf(false) }
  val all = issues.data
  val projectOptions = listOf(ALL_PROJECTS) + all.mapNotNull { projectLabel(it.references?.full, it.webUrl) }.distinct().sorted()
  val inProject = all.filter { project == ALL_PROJECTS || projectLabel(it.references?.full, it.webUrl) == project }
  val visible = inProject.filter { filter.matches(it) && it.matchesQuery(query) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Gl.colors.bg),
  ) {
    GlTopBar(
      title = "Issues",
      onBack = onBack,
      actions = {
        GlIconAction(
          icon = if (searching) Icons.Outlined.SearchOff else Icons.Outlined.Search,
          contentDescription = if (searching) "Close search" else "Search issues",
          onClick = {
            searching = !searching
            if (!searching) query = ""
          },
        )
        ListsOverflowMenu(listOf("New issue" to { showCreate = true }))
      },
    )
    if (searching) {
      ListsSearchField(
        query = query,
        onChange = { query = it },
        placeholder = "Filter by title or number",
        modifier = Modifier.fillMaxWidth().padding(horizontal = GlSpace.margin, vertical = 4.dp),
      )
    }
    ListsFilterBar {
      ListsDropdownFilter(
        name = "State",
        options = IssueFilter.entries.toList(),
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
        issues.initialLoading -> LoadingState(label = "Loading issues")
        issues.error != null && all.isEmpty() -> ErrorState(message = issues.error, onRetry = onRetry)
        else -> LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = GlSpace.group),
        ) {
          if (issues.error != null) {
            item(key = "error") {
              InlineError(
                message = issues.error,
                onRetry = onRetry,
                modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = GlSpace.tight),
              )
            }
          }
          if (visible.isEmpty()) {
            item(key = "empty") {
              EmptyState(
                icon = Icons.Outlined.Adjust,
                title = if (query.isNotBlank()) "No issues match “${query.trim()}”" else filter.emptyTitle,
                body = "Issues assigned to you or created by you show up here.",
                actionLabel = "New issue",
                onAction = { showCreate = true },
              )
            }
          } else {
            items(visible, key = { "${it.projectId}-${it.iid}" }) { issue ->
              Column {
                IssueRow(issue = issue, onClick = { onSelectIssue(issue) })
                GlDivider()
              }
            }
          }
        }
      }
    }
  }

  if (showCreate) {
    CreateIssueDialog(
      projects = projects,
      onCreate = onCreateIssue,
      onDismiss = { showCreate = false },
    )
  }
}

private const val ALL_PROJECTS = "All projects"

private fun GitLabIssue.matchesQuery(query: String): Boolean {
  val q = query.trim().removePrefix("#")
  return q.isEmpty() || title.contains(q, ignoreCase = true) || iid.toString() == q
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateIssueDialog(
  projects: List<GitLabProject>,
  onCreate: (projectId: Long, title: String, description: String?, labels: String?, onResult: (String?) -> Unit) -> Unit,
  onDismiss: () -> Unit,
) {
  var projectId by rememberSaveable { mutableStateOf(projects.firstOrNull()?.id ?: 0L) }
  var title by rememberSaveable { mutableStateOf("") }
  var description by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
  var labels by rememberSaveable { mutableStateOf("") }
  var titleMissing by remember { mutableStateOf(false) }
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
      Text("New issue", style = Gl.type.title, color = Gl.colors.textPrimary)
      ProjectPicker(
        projects = projects,
        selectedId = projectId,
        enabled = !busy,
        onSelect = { projectId = it.id },
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
      MarkdownEditor(
        value = description,
        onValueChange = { description = it },
        label = "Description",
        minLines = 4,
        enabled = !busy,
      )
      OutlinedTextField(
        value = labels,
        onValueChange = { labels = it },
        label = { Text("Labels") },
        placeholder = { Text("bug, backend") },
        supportingText = { Text("Separate with commas") },
        singleLine = true,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
      )
      error?.let { InlineError(message = it) }
      GlButton(
        text = "Create issue",
        loading = busy,
        enabled = projects.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        onClick = {
          if (title.isBlank()) {
            titleMissing = true
            return@GlButton
          }
          busy = true
          error = null
          onCreate(projectId, title.trim(), description.text.ifBlank { null }, labels.ifBlank { null }) { result ->
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

/** Read-only dropdown of projects, shared by the create dialogs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectPicker(
  projects: List<GitLabProject>,
  selectedId: Long,
  enabled: Boolean,
  onSelect: (GitLabProject) -> Unit,
) {
  if (projects.isEmpty()) {
    Text("Join or create a project first.", style = Gl.type.bodySmall, color = Gl.colors.textSecondary)
    return
  }
  var expanded by remember { mutableStateOf(false) }
  val selected = projects.firstOrNull { it.id == selectedId }
  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { if (enabled) expanded = it },
  ) {
    OutlinedTextField(
      value = selected?.nameWithNamespace.orEmpty(),
      onValueChange = {},
      readOnly = true,
      singleLine = true,
      enabled = enabled,
      label = { Text("Project") },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier = Modifier
        .fillMaxWidth()
        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      containerColor = Gl.colors.surface,
    ) {
      projects.forEach { project ->
        DropdownMenuItem(
          text = {
            Text(project.nameWithNamespace, style = Gl.type.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
          },
          onClick = {
            onSelect(project)
            expanded = false
          },
          contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
        )
      }
    }
  }
}
