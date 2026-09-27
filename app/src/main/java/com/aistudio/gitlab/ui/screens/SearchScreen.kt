package com.aistudio.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.aistudio.gitlab.data.model.GitLabIssue
import com.aistudio.gitlab.data.model.GitLabMergeRequest
import com.aistudio.gitlab.data.model.GitLabUser
import com.aistudio.gitlab.ui.components.EmptyState
import com.aistudio.gitlab.ui.components.ErrorState
import com.aistudio.gitlab.ui.components.GlDivider
import com.aistudio.gitlab.ui.components.GlFilterRow
import com.aistudio.gitlab.ui.components.GlRow
import com.aistudio.gitlab.ui.components.InlineError
import com.aistudio.gitlab.ui.components.IssueRow
import com.aistudio.gitlab.ui.components.LoadingState
import com.aistudio.gitlab.ui.components.MergeRequestRow
import com.aistudio.gitlab.ui.components.UserRow
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlSpace
import com.aistudio.gitlab.ui.viewmodel.Loadable
import com.aistudio.gitlab.ui.viewmodel.SearchScope
import com.aistudio.gitlab.ui.viewmodel.SearchState

@Composable
fun SearchScreen(
  state: SearchState,
  isSignedIn: Boolean,
  onBack: () -> Unit,
  onQueryChange: (String) -> Unit,
  onScopeChange: (SearchScope) -> Unit,
  onOpenProject: (Long) -> Unit,
  onOpenIssue: (GitLabIssue) -> Unit,
  onOpenMergeRequest: (GitLabMergeRequest) -> Unit,
  onOpenUser: (GitLabUser) -> Unit,
  modifier: Modifier = Modifier,
) {
  val c = Gl.colors
  val focus = remember { FocusRequester() }
  val keyboard = LocalSoftwareKeyboardController.current
  LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

  Column(modifier = modifier.fillMaxSize().background(c.bg)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = GlSpace.margin, top = 8.dp, bottom = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
      }
      OutlinedTextField(
        value = state.query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search GitLab") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = c.textSecondary) },
        trailingIcon = {
          if (state.query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
              Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = c.textSecondary)
            }
          }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = c.textPrimary,
          unfocusedBorderColor = c.borderStrong,
          cursorColor = c.textPrimary,
          focusedContainerColor = c.surface,
          unfocusedContainerColor = c.surface,
        ),
        modifier = Modifier
          .weight(1f)
          .focusRequester(focus)
          .semantics { contentDescription = "Search" },
      )
    }
    GlFilterRow(
      options = SearchScope.entries,
      selected = state.scope,
      label = { it.title },
      onSelect = onScopeChange,
    )
    GlDivider(modifier = Modifier.padding(top = 4.dp))

    val query = state.query.trim()
    val retry = { onScopeChange(state.scope) }
    when {
      query.length < 2 -> EmptyState(
        icon = Icons.Outlined.Search,
        title = "Search GitLab",
        body = "Find projects, issues, merge requests and people.",
      )
      !isSignedIn && (state.scope == SearchScope.ISSUES || state.scope == SearchScope.MERGE_REQUESTS) -> EmptyState(
        icon = Icons.Outlined.Lock,
        title = "Sign in to search issues",
        body = "Searching issues and merge requests needs an account on this server.",
      )
      else -> when (state.scope) {
        SearchScope.PROJECTS -> Results(state.projects, query, retry, key = { it.id }) { project ->
          GlRow(
            title = project.name,
            subtitle = listOfNotNull(
              project.pathWithNamespace.takeIf { it.isNotBlank() },
              project.description?.takeIf { it.isNotBlank() }?.lineSequence()?.firstOrNull(),
            ).joinToString(" · "),
            leading = { ProjectTile(project) },
            onClick = { onOpenProject(project.id) },
            onClickLabel = "Open project",
          )
          GlDivider(inset = 64.dp)
        }
        SearchScope.ISSUES -> Results(state.issues, query, retry, key = { "${it.projectId}-${it.iid}" }) { issue ->
          IssueRow(issue = issue, onClick = { onOpenIssue(issue) })
          GlDivider(inset = 48.dp)
        }
        SearchScope.MERGE_REQUESTS -> Results(state.mergeRequests, query, retry, key = { "${it.projectId}-${it.iid}" }) { mr ->
          MergeRequestRow(mr = mr, onClick = { onOpenMergeRequest(mr) })
          GlDivider(inset = 48.dp)
        }
        SearchScope.USERS -> Results(state.users, query, retry, key = { it.id }) { user ->
          UserRow(user = user, onClick = { onOpenUser(user) })
          GlDivider(inset = 64.dp)
        }
      }
    }
  }
}

@Composable
private fun <T> Results(
  feed: Loadable<List<T>>,
  query: String,
  onRetry: () -> Unit,
  key: (T) -> Any,
  row: @Composable (T) -> Unit,
) {
  when {
    feed.data.isEmpty() && (feed.loading || !feed.loaded) && feed.error == null -> LoadingState(label = "Searching")
    feed.error != null && feed.data.isEmpty() -> ErrorState(feed.error, onRetry)
    feed.data.isEmpty() -> EmptyState(
      icon = Icons.Outlined.SearchOff,
      title = "No results for “$query”",
      body = "Check the spelling or try a shorter search.",
    )
    else -> LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = GlSpace.group),
    ) {
      if (feed.error != null) {
        item(key = "error") {
          InlineError(feed.error, Modifier.padding(horizontal = GlSpace.margin, vertical = 4.dp), onRetry)
        }
      }
      items(feed.data, key = { key(it) }) { row(it) }
    }
  }
}
