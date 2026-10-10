package com.cboyxd.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.cboyxd.gitlab.ui.components.ColorTile
import com.cboyxd.gitlab.ui.components.GlIconAction
import com.cboyxd.gitlab.ui.theme.GlTile
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.CallMerge
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Domain
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import com.cboyxd.gitlab.ui.components.LoadingPlaceholder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.data.model.GitLabProject
import com.cboyxd.gitlab.ui.components.Avatar
import com.cboyxd.gitlab.ui.components.CountText
import com.cboyxd.gitlab.ui.components.EmptyState
import com.cboyxd.gitlab.ui.components.EventRow
import com.cboyxd.gitlab.ui.components.destination
import com.cboyxd.gitlab.ui.components.GitLabTanukiIcon
import com.cboyxd.gitlab.ui.components.GlBadge
import com.cboyxd.gitlab.ui.components.GlButton
import com.cboyxd.gitlab.ui.components.GlButtonKind
import com.cboyxd.gitlab.ui.components.GlCard
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlLargeHeader
import com.cboyxd.gitlab.ui.components.GlRow
import com.cboyxd.gitlab.ui.components.GlSectionHeader
import com.cboyxd.gitlab.ui.components.IconTile
import com.cboyxd.gitlab.ui.components.InlineError
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.components.SwapIcon
import com.cboyxd.gitlab.ui.components.Tone
import com.cboyxd.gitlab.ui.components.pressable
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.BottomTab
import com.cboyxd.gitlab.ui.viewmodel.GitLabUiState
import com.cboyxd.gitlab.ui.viewmodel.Loadable
import com.cboyxd.gitlab.ui.viewmodel.ScreenDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  state: GitLabUiState,
  onRefresh: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenSearch: () -> Unit,
  onAddServer: () -> Unit,
  onSignInAgain: () -> Unit,
  onNavigate: (ScreenDestination) -> Unit,
  onSwitchTab: (BottomTab) -> Unit,
  onToggleStar: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val instance = state.activeInstance
  PullToRefreshBox(
    isRefreshing = state.refreshing,
    onRefresh = onRefresh,
    modifier = modifier.fillMaxSize().background(Gl.colors.bg),
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = GlSpace.group),
    ) {
      item(key = "header") {
        GlLargeHeader(title = "Home") {
          if (instance != null) {
            GlIconAction(Icons.Outlined.Search, "Search", onClick = onOpenSearch)
            GlIconAction(Icons.Outlined.Sync, "Refresh", onClick = onRefresh, enabled = !state.refreshing)
            IconButton(onClick = onOpenProfile) {
              val user = state.currentUser.data
              Avatar(
                url = user?.avatarUrl,
                name = user?.name?.ifBlank { user.username } ?: instance.username.ifBlank { instance.name },
                size = 30.dp,
                modifier = Modifier.semantics { contentDescription = if (state.isSignedIn) "Your profile" else "Account" },
              )
            }
          }
        }
      }

      if (instance == null) {
        item(key = "onboarding") {
          EmptyState(
            icon = Icons.Outlined.CloudOff,
            title = "Connect a GitLab server",
            body = "Add GitLab.com or your company's server to see your issues, merge requests and pipelines.",
            actionLabel = "Add server",
            onAction = onAddServer,
          )
        }
        return@LazyColumn
      }

      if (state.sessionExpired) {
        item(key = "expired") {
          SessionBanner(
            text = "Your session expired. Sign in again to keep loading data.",
            actionLabel = "Sign in",
            onAction = onSignInAgain,
          )
        }
      } else if (!state.isSignedIn) {
        item(key = "public") {
          SessionBanner(
            text = "You're browsing as a guest. Sign in to see your issues, merge requests and to-dos.",
            actionLabel = "Sign in",
            onAction = onSignInAgain,
            informational = true,
          )
        }
      }

      if (state.isSignedIn) {
        item(key = "work-header") { GlSectionHeader("My work") }
        item(key = "work") {
          val failedPipelines = state.pipelines.data.count { it.status == "failed" }
          WorkRow(
            icon = Icons.Outlined.Adjust,
            tile = GlTile.green,
            title = "Issues",
            feed = state.issues,
            count = null,
            onClick = { onNavigate(ScreenDestination.IssuesList) },
          )
          WorkRow(
            icon = Icons.Outlined.MergeType,
            tile = GlTile.blue,
            title = "Merge requests",
            feed = state.mergeRequests,
            count = null,
            onClick = { onNavigate(ScreenDestination.MergeRequestsList) },
          )
          WorkRow(
            icon = Icons.Outlined.PlayCircleOutline,
            tile = GlTile.yellow,
            title = "Pipelines",
            feed = state.pipelines,
            count = null,
            badge = if (failedPipelines > 0) "$failedPipelines failed" else null,
            onClick = { onNavigate(ScreenDestination.PipelinesList) },
          )
          WorkRow(
            icon = Icons.Outlined.Book,
            tile = GlTile.slate,
            title = "Your projects",
            feed = state.projects,
            count = null,
            onClick = { onSwitchTab(BottomTab.PROJECTS) },
          )
          WorkRow(
            icon = Icons.Outlined.Domain,
            tile = GlTile.orange,
            title = "Groups",
            feed = state.groups,
            count = null,
            onClick = { onNavigate(ScreenDestination.GroupsList) },
          )
        }
        item(key = "work-divider") { GlDivider(Modifier.padding(top = GlSpace.tight)) }
      }

      // Without an account there is nothing starred; the feed holds popular public projects.
      val browsing = !state.isSignedIn
      item(key = "starred-header") { GlSectionHeader(if (browsing) "Popular projects" else "Favorites") }
      val starred = if (browsing) state.projects.data.take(8) else state.projects.data.filter { it.isStarred }
      when {
        state.projects.initialLoading -> item(key = "starred-loading") { LoadingState(label = "Loading projects") }
        state.projects.error != null && starred.isEmpty() -> item(key = "starred-error") {
          InlineError(
            message = state.projects.error,
            onRetry = onRefresh,
            modifier = Modifier.padding(horizontal = GlSpace.margin),
          )
        }
        starred.isEmpty() -> item(key = "starred-empty") {
          Column(Modifier.padding(horizontal = GlSpace.margin)) {
            Text(
              text = "Star the projects you work on to keep them one tap away.",
              style = Gl.type.bodySmall,
              color = Gl.colors.textSecondary,
            )
            Spacer(Modifier.height(GlSpace.tight))
            GlButton(
              text = "Browse projects",
              onClick = { onSwitchTab(BottomTab.PROJECTS) },
              kind = GlButtonKind.Secondary,
            )
          }
        }
        else -> items(starred, key = { "fav-${it.id}" }) { project ->
          FavoriteProjectRow(
            project = project,
            onClick = { onNavigate(ScreenDestination.ProjectDetail(project.id)) },
          )
        }
      }

      if (state.isSignedIn) {
        item(key = "activity-divider") { GlDivider(Modifier.padding(top = GlSpace.tight)) }
        item(key = "activity-header") { GlSectionHeader("Recent activity") }
        val activity = state.activity
        when {
          activity.initialLoading -> item(key = "activity-loading") { LoadingState(label = "Loading activity") }
          activity.error != null && activity.data.isEmpty() -> item(key = "activity-error") {
            InlineError(
              message = activity.error,
              onRetry = onRefresh,
              modifier = Modifier.padding(horizontal = GlSpace.margin),
            )
          }
          activity.data.isEmpty() -> item(key = "activity-empty") {
            Text(
              text = "Your recent activity on this server shows up here.",
              style = Gl.type.bodySmall,
              color = Gl.colors.textSecondary,
              modifier = Modifier.padding(horizontal = GlSpace.margin),
            )
          }
          else -> items(activity.data.take(8), key = { "event-${it.id}-${it.createdAt}" }) { event ->
            EventRow(
              event = event,
              onClick = event.destination()?.let { dest -> { onNavigate(dest) } },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SessionBanner(text: String, actionLabel: String, onAction: () -> Unit, informational: Boolean = false) {
  val c = Gl.colors
  Row(
    modifier = Modifier
      .padding(start = GlSpace.margin, end = GlSpace.margin, top = GlSpace.tight)
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(if (informational) c.infoBg else c.warningBg)
      .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      if (informational) Icons.Outlined.Info else Icons.Outlined.ErrorOutline,
      contentDescription = null,
      tint = if (informational) c.info else c.warning,
      modifier = Modifier.size(18.dp),
    )
    Spacer(Modifier.width(8.dp))
    Text(text, style = Gl.type.meta, color = c.textPrimary, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
    TextButton(onClick = onAction) { Text(actionLabel.uppercase(), style = Gl.type.label, color = Gl.colors.accent) }
  }
}

@Composable
private fun <T> WorkRow(
  icon: ImageVector,
  tile: Color,
  title: String,
  feed: Loadable<List<T>>,
  count: Int?,
  onClick: () -> Unit,
  badge: String? = null,
) {
  GlRow(
    title = title,
    leading = { ColorTile(icon = icon, color = tile) },
    onClick = onClick,
    trailing = when {
      feed.loading && !feed.loaded -> {
        {
          LoadingPlaceholder()
        }
      }
      feed.error != null && feed.data.isEmpty() -> {
        { Icon(Icons.Outlined.ErrorOutline, contentDescription = "Couldn't load", tint = Gl.colors.danger, modifier = Modifier.size(18.dp)) }
      }
      badge != null -> { { GlBadge(text = badge, tone = Tone.Danger) } }
      count != null && count > 0 -> { { CountText(count) } }
      else -> null
    },
  )
}

/** Favorite in the GitHub manner: owner avatar, the namespace above the project's name. */
@Composable
private fun FavoriteProjectRow(project: GitLabProject, onClick: () -> Unit) {
  val namespace = project.namespace?.name?.takeIf { it.isNotBlank() }
    ?: project.nameWithNamespace.substringBeforeLast("/", "").trim()
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open ${project.name}")
      .heightIn(min = 64.dp)
      .padding(horizontal = GlSpace.margin, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ProjectTile(project, size = 32.dp)
    Spacer(Modifier.width(16.dp))
    Column(Modifier.weight(1f)) {
      if (namespace.isNotBlank()) {
        Text(namespace, style = Gl.type.bodySmall, color = Gl.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Text(
        project.name,
        style = Gl.type.rowTitle.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
        color = Gl.colors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

/** Project avatar or its initial on a neutral tile. Shared by the project lists. */
@Composable
internal fun ProjectTile(project: GitLabProject, modifier: Modifier = Modifier, size: Dp = 36.dp) {
  val c = Gl.colors
  val shape = RoundedCornerShape(size / 4)
  // The initial sits underneath so a failed or private avatar still leaves a readable tile.
  Box(
    modifier = modifier.testTag(com.cboyxd.gitlab.ui.components.PERSONAL_IMAGE_TAG).size(size).clip(shape).background(c.surface2).border(1.dp, c.imageOutline, shape),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = project.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
      style = Gl.type.label,
      color = c.textSecondary,
    )
    if (!project.avatarUrl.isNullOrBlank()) {
      com.cboyxd.gitlab.ui.components.AvatarImage(
        url = project.avatarUrl,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}
