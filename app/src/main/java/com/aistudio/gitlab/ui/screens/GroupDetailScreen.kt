package com.aistudio.gitlab.ui.screens

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
import com.aistudio.gitlab.data.model.GitLabGroup
import com.aistudio.gitlab.data.model.GitLabProject
import com.aistudio.gitlab.ui.components.Avatar
import com.aistudio.gitlab.ui.components.EmptyState
import com.aistudio.gitlab.ui.components.ErrorState
import com.aistudio.gitlab.ui.components.FullScreenState
import com.aistudio.gitlab.ui.components.GlBadge
import com.aistudio.gitlab.ui.components.GlCard
import com.aistudio.gitlab.ui.components.GlDivider
import com.aistudio.gitlab.ui.components.GlIconAction
import com.aistudio.gitlab.ui.components.GlRow
import com.aistudio.gitlab.ui.components.GlSectionHeader
import com.aistudio.gitlab.ui.components.GlTopBar
import com.aistudio.gitlab.ui.components.IconTile
import com.aistudio.gitlab.ui.components.InlineError
import com.aistudio.gitlab.ui.components.LoadingState
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlSpace
import com.aistudio.gitlab.ui.viewmodel.GroupDetailState

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
    GlTopBar(
      title = group?.name ?: "Group",
      subtitle = group?.fullPath?.takeIf { it.isNotBlank() },
      onBack = onBack,
    ) {
      val url = group?.webUrl?.takeIf { it.isNotBlank() }
      if (url != null) GlIconAction(Icons.AutoMirrored.Outlined.OpenInNew, "Open in browser", onClick = { onOpenInBrowser(url) })
    }
    GlDivider()
    if (group == null) {
      FullScreenState { if (state.details.error != null) ErrorState(state.details.error, onRetry) else LoadingState(label = "Loading group") }
    } else LazyColumn(
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
        item(key = "subgroups-header") { GlSectionHeader("Subgroups") }
        when {
          subgroups.initialLoading -> item(key = "subgroups-loading") { LoadingState(label = "Loading subgroups") }
          subgroups.error != null && subgroups.data.isEmpty() -> item(key = "subgroups-error") {
            InlineError(subgroups.error, Modifier.padding(horizontal = GlSpace.margin), onRetry)
          }
          else -> item(key = "subgroups") {
            GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin)) {
              subgroups.data.forEachIndexed { index, sub ->
                if (index > 0) GlDivider(inset = 64.dp)
                GlRow(
                  title = sub.name,
                  subtitle = sub.description?.takeIf { it.isNotBlank() } ?: sub.fullPath,
                  leading = { IconTile(Icons.Outlined.Domain) },
                  trailing = { Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Gl.colors.textTertiary) },
                  onClick = { onOpenGroup(sub) },
                  onClickLabel = "Open group",
                )
              }
            }
          }
        }
      }

      item(key = "projects-header") { GlSectionHeader("Projects") }
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
        else -> item(key = "projects") {
          GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin)) {
            projects.data.forEachIndexed { index, project ->
              if (index > 0) GlDivider(inset = 64.dp)
              GroupProjectRow(project) { onOpenProject(project.id) }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun GroupHeader(group: GitLabGroup) {
  val c = Gl.colors
  Column(modifier = Modifier.fillMaxWidth().padding(GlSpace.margin)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (!group.avatarUrl.isNullOrBlank()) {
        Avatar(group.avatarUrl, group.name, 56.dp)
      } else {
        IconTile(Icons.Outlined.Domain)
      }
      Spacer(Modifier.width(GlSpace.margin))
      Column(modifier = Modifier.weight(1f)) {
        Text(group.name, style = Gl.type.title, color = c.textPrimary)
        if (group.fullPath.isNotBlank()) Text(group.fullPath, style = Gl.type.meta, color = c.textSecondary)
      }
      if (group.visibility.isNotBlank()) {
        Spacer(Modifier.width(GlSpace.tight))
        GlBadge(group.visibility.replaceFirstChar { it.uppercase() })
      }
    }
    if (!group.description.isNullOrBlank()) {
      Spacer(Modifier.height(12.dp))
      Text(group.description, style = Gl.type.bodySmall, color = c.textSecondary)
    }
  }
}

@Composable
private fun GroupProjectRow(project: GitLabProject, onClick: () -> Unit) {
  GlRow(
    title = project.name,
    subtitle = listOfNotNull(
      project.description?.takeIf { it.isNotBlank() },
      if (project.starCount == 1) "1 star" else "${project.starCount} stars",
    ).joinToString(" · "),
    leading = { ProjectTile(project) },
    onClick = onClick,
    onClickLabel = "Open project",
  )
}
