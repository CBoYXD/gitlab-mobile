package com.cboyxd.gitlab.ui.screens

import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import com.cboyxd.gitlab.ui.components.Avatar
import com.cboyxd.gitlab.ui.theme.GlTile
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
import com.cboyxd.gitlab.data.model.GitLabGroup
import com.cboyxd.gitlab.ui.components.EmptyState
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlRow
import com.cboyxd.gitlab.ui.components.GlTopBar
import com.cboyxd.gitlab.ui.components.IconTile
import com.cboyxd.gitlab.ui.components.InlineError
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.Loadable

@Composable
fun GroupsScreen(
  groups: Loadable<List<GitLabGroup>>,
  onSelect: (GitLabGroup) -> Unit,
  onRetry: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize().background(Gl.colors.bg)) {
    GlTopBar(title = "Groups", onBack = onBack)
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
            leading = { GroupAvatar(group) },
            onClick = { onSelect(group) },
            onClickLabel = "Open group",
          )
          GlDivider(inset = 64.dp)
        }
      }
    }
  }
}

/** Group avatar, or an orange tile with the group's initial when it has none. */
@Composable
internal fun GroupAvatar(group: GitLabGroup, size: androidx.compose.ui.unit.Dp = 32.dp) {
  if (!group.avatarUrl.isNullOrBlank()) {
    Avatar(group.avatarUrl, group.name, size)
  } else {
    androidx.compose.foundation.layout.Box(
      modifier = Modifier
        .size(size)
        .clip(androidx.compose.foundation.shape.RoundedCornerShape(size / 4))
        .background(GlTile.orange),
      contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
      Text(
        group.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
        style = if (size >= 48.dp) Gl.type.title else Gl.type.label,
        color = androidx.compose.ui.graphics.Color.White,
      )
    }
  }
}
