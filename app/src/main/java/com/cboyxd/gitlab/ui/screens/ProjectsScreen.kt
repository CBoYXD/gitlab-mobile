package com.cboyxd.gitlab.ui.screens

import androidx.compose.material.icons.filled.Star
import com.cboyxd.gitlab.ui.components.ListsDropdownFilter
import com.cboyxd.gitlab.ui.components.ListsFilterBar
import com.cboyxd.gitlab.ui.components.ListsSearchField
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.data.model.GitLabProject
import com.cboyxd.gitlab.ui.components.EmptyState
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.GlBadge
import com.cboyxd.gitlab.ui.components.GlButton
import com.cboyxd.gitlab.ui.components.GlButtonKind
import com.cboyxd.gitlab.ui.components.GlChip
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlFilterRow
import com.cboyxd.gitlab.ui.components.GlIconAction
import com.cboyxd.gitlab.ui.components.GlLargeHeader
import com.cboyxd.gitlab.ui.components.GlTopBar
import com.cboyxd.gitlab.ui.components.InlineError
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.components.SwapIcon
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlRadius
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.Loadable

private enum class ProjectFilter(val label: String) {
  ALL("All"),
  STARRED("Starred"),
  PRIVATE("Private"),
  INTERNAL("Internal"),
  PUBLIC("Public");

  fun matches(project: GitLabProject): Boolean = when (this) {
    ALL -> true
    STARRED -> project.isStarred
    PRIVATE -> project.visibility.equals("private", true)
    INTERNAL -> project.visibility.equals("internal", true)
    PUBLIC -> project.visibility.equals("public", true)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
  title: String,
  projects: Loadable<List<GitLabProject>>,
  searchQuery: String?,
  searchResults: Loadable<List<GitLabProject>>?,
  pendingStars: Set<Long>,
  canStar: Boolean,
  canCreate: Boolean,
  hasServer: Boolean,
  onSearchChange: (String) -> Unit,
  onRefresh: () -> Unit,
  refreshing: Boolean,
  onSelectProject: (Long) -> Unit,
  onToggleStar: (Long) -> Unit,
  onCreateProject: (name: String, description: String, visibility: String, onResult: (String?) -> Unit) -> Unit,
  onAddServer: () -> Unit,
  onBack: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  var filter by rememberSaveable { mutableStateOf(ProjectFilter.ALL) }
  var showCreate by rememberSaveable { mutableStateOf(false) }
  val searching = !searchQuery.isNullOrBlank() && searchResults != null
  val feed = if (searching) searchResults!! else projects
  val visible = feed.data.filter { filter.matches(it) }

  Column(modifier = modifier.fillMaxSize().background(Gl.colors.bg)) {
    if (onBack == null) {
      GlLargeHeader(title = title) {
        if (canCreate) {
          GlIconAction(icon = Icons.Outlined.Add, contentDescription = "New project", onClick = { showCreate = true })
        }
      }
    } else {
      GlTopBar(title = title, onBack = onBack)
    }

    if (!hasServer) {
      EmptyState(
        icon = Icons.Outlined.CloudOff,
        title = "Connect a GitLab server",
        body = "Your projects appear here once you add a server.",
        actionLabel = "Add server",
        onAction = onAddServer,
      )
      return@Column
    }

    if (searchQuery != null) {
      ListsSearchField(
        query = searchQuery,
        onChange = onSearchChange,
        placeholder = if (canStar) "Search projects" else "Search public projects",
        modifier = Modifier.fillMaxWidth().padding(horizontal = GlSpace.margin, vertical = 4.dp),
      )
    }
    ListsFilterBar {
      ListsDropdownFilter(
        name = "Show",
        options = ProjectFilter.entries.filter { canStar || it != ProjectFilter.STARRED },
        selected = filter,
        label = { if (it == ProjectFilter.ALL) "All projects" else it.label },
        onSelect = { filter = it },
        countOf = { option -> feed.data.count { option.matches(it) } },
        active = filter != ProjectFilter.ALL,
      )
      if (feed.loaded && feed.data.isNotEmpty()) {
        Text(
          if (visible.size == 1) "1 project" else "${visible.size} projects",
          style = Gl.type.bodySmall,
          color = Gl.colors.textSecondary,
        )
      }
    }
    GlDivider()

    val body: @Composable () -> Unit = {
      when {
        feed.initialLoading || (searching && feed.loading && feed.data.isEmpty()) ->
          LoadingState(label = if (searching) "Searching projects" else "Loading projects")
        feed.error != null && feed.data.isEmpty() -> ErrorState(message = feed.error, onRetry = if (searching) ({ onSearchChange(searchQuery.orEmpty()) }) else onRefresh)
        visible.isEmpty() -> LazyColumn(modifier = Modifier.fillMaxSize()) {
          item {
            when {
              searching -> EmptyState(
                icon = Icons.Outlined.Search,
                title = "No projects match “${searchQuery!!.trim()}”",
                body = "Check the spelling or search by the project's path.",
                actionLabel = "Clear search",
                onAction = { onSearchChange("") },
              )
              filter != ProjectFilter.ALL -> EmptyState(
                icon = Icons.Outlined.Folder,
                title = "No ${filter.label.lowercase()} projects",
                body = "Nothing here matches this filter.",
                actionLabel = "Show all projects",
                onAction = { filter = ProjectFilter.ALL },
              )
              else -> EmptyState(
                icon = Icons.Outlined.Folder,
                title = "No projects yet",
                body = when {
                  onBack != null -> "This group has no projects you can see."
                  !canStar -> "Search public projects by name, or paste a path like gitlab-org/gitlab."
                  else -> "Projects you're a member of or have starred appear here."
                },
                actionLabel = if (canCreate) "Create project" else null,
                onAction = if (canCreate) ({ showCreate = true }) else null,
              )
            }
          }
        }
        else -> LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = GlSpace.group),
        ) {
          if (feed.error != null) {
            item(key = "error") {
              InlineError(
                message = feed.error,
                onRetry = onRefresh,
                modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = 4.dp),
              )
            }
          }
          items(visible, key = { it.id }) { project ->
            ProjectRow(
              project = project,
              pending = project.id in pendingStars,
              onClick = { onSelectProject(project.id) },
              onToggleStar = if (canStar) ({ onToggleStar(project.id) }) else null,
            )
            GlDivider()
          }
        }
      }
    }

    if (onBack == null && !searching) {
      PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) { body() }
    } else {
      Box(modifier = Modifier.fillMaxSize()) { body() }
    }
  }

  if (showCreate) {
    NewProjectDialog(
      onDismiss = { showCreate = false },
      onCreate = onCreateProject,
    )
  }
}

@Composable
private fun ProjectRow(
  project: GitLabProject,
  pending: Boolean,
  onClick: () -> Unit,
  onToggleStar: (() -> Unit)?,
) {
  ListsProjectRow(
    project = project,
    onClick = onClick,
    trailing = if (onToggleStar == null) null else {
      {
        IconButton(onClick = onToggleStar, enabled = !pending) {
          SwapIcon(
            target = project.isStarred,
            on = Icons.Filled.Star,
            off = Icons.Outlined.StarOutline,
            contentDescription = if (project.isStarred) "Unstar ${project.name}" else "Star ${project.name}",
            tintOn = StarYellow,
            tintOff = Gl.colors.textTertiary,
          )
        }
      }
    },
  )
}

/** GitHub's starred yellow; only the filled star uses it. */
private val StarYellow = androidx.compose.ui.graphics.Color(0xFFE3B341)

@Composable
private fun NewProjectDialog(
  onDismiss: () -> Unit,
  onCreate: (name: String, description: String, visibility: String, onResult: (String?) -> Unit) -> Unit,
) {
  var name by rememberSaveable { mutableStateOf("") }
  var description by rememberSaveable { mutableStateOf("") }
  var visibility by rememberSaveable { mutableStateOf("private") }
  var busy by rememberSaveable { mutableStateOf(false) }
  var error by rememberSaveable { mutableStateOf<String?>(null) }
  val c = Gl.colors

  AlertDialog(
    onDismissRequest = { if (!busy) onDismiss() },
    containerColor = c.surface,
    title = { Text("New project", style = Gl.type.title, color = c.textPrimary) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it; error = null },
          label = { Text("Name") },
          placeholder = { Text("my-project") },
          singleLine = true,
          enabled = !busy,
          isError = error != null && name.isBlank(),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description (optional)") },
          minLines = 2,
          maxLines = 4,
          enabled = !busy,
          modifier = Modifier.fillMaxWidth(),
        )
        Text("Visibility", style = Gl.type.label, color = c.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(GlSpace.tight)) {
          listOf("private" to "Private", "internal" to "Internal", "public" to "Public").forEach { (value, label) ->
            GlChip(text = label, selected = visibility == value, onClick = { if (!busy) visibility = value })
          }
        }
        error?.let { Text(it, style = Gl.type.meta, color = c.danger) }
      }
    },
    confirmButton = {
      GlButton(
        text = "Create project",
        loading = busy,
        onClick = {
          if (name.isBlank()) {
            error = "Enter a name for the project."
            return@GlButton
          }
          busy = true
          error = null
          onCreate(name, description, visibility) { result ->
            busy = false
            if (result == null) onDismiss() else error = result
          }
        },
      )
    },
    dismissButton = {
      GlButton(text = "Cancel", onClick = onDismiss, kind = GlButtonKind.Ghost, enabled = !busy)
    },
  )
}
