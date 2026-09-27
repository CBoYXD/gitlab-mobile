package com.cboyxd.gitlab.ui.screens

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
import com.cboyxd.gitlab.data.model.GitLabIssue
import com.cboyxd.gitlab.data.model.GitLabMergeRequest
import com.cboyxd.gitlab.data.model.GitLabUser
import com.cboyxd.gitlab.ui.components.EmptyState
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlFilterRow
import com.cboyxd.gitlab.ui.components.GlRow
import com.cboyxd.gitlab.ui.components.InlineError
import com.cboyxd.gitlab.ui.components.IssueRow
import com.cboyxd.gitlab.ui.components.ListsSearchField
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.components.MergeRequestRow
import com.cboyxd.gitlab.ui.components.UserRow
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.Loadable
import com.cboyxd.gitlab.ui.viewmodel.SearchScope
import com.cboyxd.gitlab.ui.viewmodel.SearchState

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
      ListsSearchField(
        query = state.query,
        onChange = onQueryChange,
        placeholder = "Search GitLab",
        modifier = Modifier
          .weight(1f)
          .focusRequester(focus),
      )
    }
    GlFilterRow(
      options = SearchScope.entries,
      selected = state.scope,
      label = { it.title },
      onSelect = onScopeChange,
    )
    GlDivider(modifier = Modifier.padding(top = 2.dp))

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
          ListsProjectRow(project, onClick = { onOpenProject(project.id) })
          GlDivider()
        }
        SearchScope.ISSUES -> Results(state.issues, query, retry, key = { "${it.projectId}-${it.iid}" }) { issue ->
          IssueRow(issue = issue, onClick = { onOpenIssue(issue) })
          GlDivider()
        }
        SearchScope.MERGE_REQUESTS -> Results(state.mergeRequests, query, retry, key = { "${it.projectId}-${it.iid}" }) { mr ->
          MergeRequestRow(mr = mr, onClick = { onOpenMergeRequest(mr) })
          GlDivider()
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
