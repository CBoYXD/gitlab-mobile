package com.cboyxd.gitlab.ui.screens

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Domain
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.data.model.GitLabGroup
import com.cboyxd.gitlab.data.model.GitLabProject
import com.cboyxd.gitlab.ui.components.Avatar
import com.cboyxd.gitlab.ui.components.EmptyState
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.FullScreenState
import com.cboyxd.gitlab.ui.components.GlBadge
import com.cboyxd.gitlab.ui.components.GlCard
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlIconAction
import com.cboyxd.gitlab.ui.components.GlRow
import com.cboyxd.gitlab.ui.components.GlSectionHeader
import com.cboyxd.gitlab.ui.components.GlTopBar
import com.cboyxd.gitlab.ui.components.IconTile
import com.cboyxd.gitlab.ui.components.InlineError
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.GroupDetailState

@Composable
fun GroupDetailScreen(
  state: GroupDetailState,
  onBack: () -> Unit,
  onRetry: () -> Unit,
  onOpenGroup: (GitLabGroup) -> Unit,
  onOpenProject: (Long) -> Unit,
  onOpenInBrowser: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val group = state.details.data ?: state.group
  Column(modifier = modifier.fillMaxSize().background(Gl.colors.bg)) {
    val listState = rememberLazyListState()
    val headerGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    GlTopBar(
      title = group?.name ?: "Group",
      overline = group?.fullPath?.substringBeforeLast('/', "")?.takeIf { it.isNotBlank() },
      showTitle = group == null || headerGone,
      onBack = onBack,
    ) {
      val url = group?.webUrl?.takeIf { it.isNotBlank() }
      if (url != null) GlIconAction(Icons.AutoMirrored.Outlined.OpenInNew, "Open in browser", onClick = { onOpenInBrowser(url) })
    }
    if (group == null) {
      FullScreenState { if (state.details.error != null) ErrorState(state.details.error, onRetry) else LoadingState(label = "Loading group") }
    } else LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = GlSpace.group),
    ) {
      item(key = "header") { GroupHeader(group) }
      if (state.details.error != null) {
        item(key = "details-error") {
          InlineError(state.details.error, Modifier.padding(horizontal = GlSpace.margin), onRetry)
        }
      }

      val subgroups = state.subgroups
      if (subgroups.loading || subgroups.data.isNotEmpty() || subgroups.error != null) {
        item(key = "subgroups-header") {
          GlDivider()
          GlSectionHeader("Subgroups")
        }
        when {
          subgroups.initialLoading -> item(key = "subgroups-loading") { LoadingState(label = "Loading subgroups") }
          subgroups.error != null && subgroups.data.isEmpty() -> item(key = "subgroups-error") {
            InlineError(subgroups.error, Modifier.padding(horizontal = GlSpace.margin), onRetry)
          }
          else -> items(subgroups.data, key = { "sub-${it.id}" }) { sub ->
            GlRow(
              title = sub.name,
              subtitle = sub.description?.takeIf { it.isNotBlank() } ?: sub.fullPath,
              leading = { GroupAvatar(sub) },
              onClick = { onOpenGroup(sub) },
              onClickLabel = "Open group",
            )
          }
        }
      }

      item(key = "projects-header") {
        GlDivider()
        GlSectionHeader("Projects")
      }
      val projects = state.projects
      when {
        projects.initialLoading -> item(key = "projects-loading") { LoadingState(label = "Loading projects") }
        projects.error != null && projects.data.isEmpty() -> item(key = "projects-error") {
          InlineError(projects.error, Modifier.padding(horizontal = GlSpace.margin), onRetry)
        }
        projects.data.isEmpty() -> item(key = "projects-empty") {
          EmptyState(
            icon = Icons.Outlined.Folder,
            title = "No projects here",
            body = "This group has no projects you can see.",
          )
        }
        else -> items(projects.data, key = { "project-${it.id}" }) { project ->
          ListsProjectRow(project, onClick = { onOpenProject(project.id) })
          GlDivider(inset = 64.dp)
        }
      }
    }
  }
}

/** Big header like a GitHub organization: avatar, name, path and visibility, description. */
@Composable
private fun GroupHeader(group: GitLabGroup) {
  val c = Gl.colors
  Column(modifier = Modifier.fillMaxWidth().padding(start = GlSpace.margin, end = GlSpace.margin, top = 8.dp, bottom = GlSpace.margin)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      GroupAvatar(group, size = 64.dp)
      Spacer(Modifier.width(GlSpace.margin))
      Column(modifier = Modifier.weight(1f)) {
        Text(group.name, style = Gl.type.headline, color = c.textPrimary, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        if (group.fullPath.isNotBlank()) Text(group.fullPath, style = Gl.type.bodySmall, color = c.textSecondary)
      }
    }
    if (!group.description.isNullOrBlank()) {
      Spacer(Modifier.height(12.dp))
      Text(group.description, style = Gl.type.body, color = c.textPrimary)
    }
    if (group.visibility.isNotBlank()) {
      Spacer(Modifier.height(12.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          if (group.visibility.equals("public", true)) Icons.Outlined.Public else Icons.Outlined.Lock,
          contentDescription = null,
          tint = c.textSecondary,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(group.visibility.replaceFirstChar { it.uppercase() }, style = Gl.type.body, color = c.textSecondary)
      }
    }
  }
}
