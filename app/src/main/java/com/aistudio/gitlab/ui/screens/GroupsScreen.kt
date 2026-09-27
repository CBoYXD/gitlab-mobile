package com.aistudio.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Domain
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistudio.gitlab.data.model.GitLabGroup
import com.aistudio.gitlab.ui.components.EmptyState
import com.aistudio.gitlab.ui.components.ErrorState
import com.aistudio.gitlab.ui.components.GlDivider
import com.aistudio.gitlab.ui.components.GlRow
import com.aistudio.gitlab.ui.components.GlTopBar
import com.aistudio.gitlab.ui.components.IconTile
import com.aistudio.gitlab.ui.components.InlineError
import com.aistudio.gitlab.ui.components.LoadingState
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlSpace
import com.aistudio.gitlab.ui.viewmodel.Loadable

@Composable
fun GroupsScreen(
  groups: Loadable<List<GitLabGroup>>,
  onSelect: (GitLabGroup) -> Unit,
  onRetry: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize().background(Gl.colors.bg)) {
    GlTopBar(title = "Groups", onBack = onBack, subtitle = if (groups.loaded && groups.data.isNotEmpty()) "${groups.data.size} groups" else null)
    when {
      groups.initialLoading -> LoadingState(label = "Loading groups")
      groups.error != null && groups.data.isEmpty() -> ErrorState(message = groups.error, onRetry = onRetry)
      groups.data.isEmpty() -> EmptyState(
        icon = Icons.Outlined.Domain,
        title = "No groups yet",
        body = "Groups you belong to on this server appear here.",
      )
      else -> LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = GlSpace.group),
      ) {
        if (groups.error != null) {
          item(key = "error") {
            InlineError(
              message = groups.error,
              onRetry = onRetry,
              modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = 4.dp),
            )
          }
        }
        items(groups.data, key = { it.id }) { group ->
          GlRow(
            title = group.name,
            subtitle = group.fullPath.takeIf { it != group.name },
            leading = { IconTile(icon = Icons.Outlined.Domain) },
            trailing = {
              Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = Gl.colors.textTertiary,
                modifier = Modifier.size(20.dp),
              )
            },
            onClick = { onSelect(group) },
            onClickLabel = "Open group",
          )
          GlDivider(inset = 64.dp)
        }
      }
    }
  }
}
