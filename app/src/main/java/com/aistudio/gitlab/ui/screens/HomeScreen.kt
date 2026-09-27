package com.aistudio.gitlab.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
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
import com.aistudio.gitlab.data.model.GitLabProject
import com.aistudio.gitlab.ui.components.Avatar
import com.aistudio.gitlab.ui.components.CountText
import com.aistudio.gitlab.ui.components.EmptyState
import com.aistudio.gitlab.ui.components.EventRow
import com.aistudio.gitlab.ui.components.destination
import com.aistudio.gitlab.ui.components.GitLabTanukiIcon
import com.aistudio.gitlab.ui.components.GlBadge
import com.aistudio.gitlab.ui.components.GlButton
import com.aistudio.gitlab.ui.components.GlButtonKind
import com.aistudio.gitlab.ui.components.GlCard
import com.aistudio.gitlab.ui.components.GlDivider
import com.aistudio.gitlab.ui.components.GlLargeHeader
import com.aistudio.gitlab.ui.components.GlRow
import com.aistudio.gitlab.ui.components.GlSectionHeader
import com.aistudio.gitlab.ui.components.IconTile
import com.aistudio.gitlab.ui.components.InlineError
import com.aistudio.gitlab.ui.components.LoadingState
import com.aistudio.gitlab.ui.components.SwapIcon
import com.aistudio.gitlab.ui.components.Tone
import com.aistudio.gitlab.ui.components.pressable
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlSpace
import com.aistudio.gitlab.ui.viewmodel.BottomTab
import com.aistudio.gitlab.ui.viewmodel.GitLabUiState
import com.aistudio.gitlab.ui.viewmodel.Loadable
import com.aistudio.gitlab.ui.viewmodel.ScreenDestination

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
            IconButton(onClick = onOpenSearch) {
              Icon(Icons.Outlined.Search, contentDescription = "Search", tint = Gl.colors.textPrimary)
            }
            IconButton(onClick = onOpenProfile) {
              val user = state.currentUser.data
              Avatar(
                url = user?.avatarUrl,
                name = user?.name?.ifBlank { user.username } ?: instance.username.ifBlank { instance.name },
                size = 32.dp,
                modifier = Modifier.semantics { contentDescription = "Your profile" },
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
            text = "You're browsing public projects. Sign in to load your issues, merge requests and to-dos.",
            actionLabel = "Sign in",
            onAction = onSignInAgain,
            informational = true,
          )
        }
      }

      if (state.isSignedIn) item(key = "work-header") { GlSectionHeader("My work") }
      if (state.isSignedIn) item(key = "work") {
        val failedPipelines = state.pipelines.data.count { it.status == "failed" }
        GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin)) {
          WorkRow(
            icon = Icons.Outlined.Adjust,
            title = "Issues",
            feed = state.issues,
            count = state.issues.data.count { it.state == "opened" },
            onClick = { onNavigate(ScreenDestination.IssuesList) },
          )
          GlDivider(inset = 64.dp)
          WorkRow(
            icon = Icons.Outlined.CallMerge,
            title = "Merge requests",
            feed = state.mergeRequests,
            count = state.mergeRequests.data.count { it.state == "opened" },
            onClick = { onNavigate(ScreenDestination.MergeRequestsList) },
          )
          GlDivider(inset = 64.dp)
          WorkRow(
            icon = Icons.Outlined.PlayCircleOutline,
            title = "Pipelines",
            feed = state.pipelines,
            count = null,
            badge = if (failedPipelines > 0) "$failedPipelines failed" else null,
            onClick = { onNavigate(ScreenDestination.PipelinesList) },
          )
          GlDivider(inset = 64.dp)
          WorkRow(
            icon = Icons.Outlined.Inbox,
            title = "To-dos",
            feed = state.notifications,
            count = state.unreadCount,
            onClick = { onSwitchTab(BottomTab.INBOX) },
          )
          GlDivider(inset = 64.dp)
          WorkRow(
            icon = Icons.Outlined.Domain,
            title = "Groups",
            feed = state.groups,
            count = state.groups.data.size,
            onClick = { onNavigate(ScreenDestination.GroupsList) },
          )
        }
      }

      if (state.isSignedIn) {
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
          else -> item(key = "activity") {
            GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin)) {
              activity.data.take(8).forEachIndexed { index, event ->
                if (index > 0) GlDivider(inset = 60.dp)
                EventRow(
                  event = event,
                  onClick = event.destination()?.let { dest -> { onNavigate(dest) } },
                )
              }
            }
          }
        }
      }

      // Without an account there is nothing starred; the feed holds popular public projects.
      val browsing = !state.isSignedIn
      item(key = "starred-header") { GlSectionHeader(if (browsing) "Popular projects" else "Starred projects") }
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
          GlCard(
            modifier = Modifier.padding(horizontal = GlSpace.margin),
            contentPadding = PaddingValues(GlSpace.margin),
          ) {
            Text(
              text = "Star projects you work on to keep them one tap away.",
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
        else -> item(key = "starred") {
          GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin)) {
            starred.forEachIndexed { index, project ->
              if (index > 0) GlDivider(inset = 64.dp)
              StarredProjectRow(
                project = project,
                pending = project.id in state.pendingStars,
                onClick = { onNavigate(ScreenDestination.ProjectDetail(project.id)) },
                onToggleStar = if (browsing) null else ({ onToggleStar(project.id) }),
              )
            }
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
      .clip(RoundedCornerShape(12.dp))
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
    GlButton(text = actionLabel, onClick = onAction, kind = GlButtonKind.Ghost)
  }
}

@Composable
private fun <T> WorkRow(
  icon: ImageVector,
  title: String,
  feed: Loadable<List<T>>,
  count: Int?,
  onClick: () -> Unit,
  badge: String? = null,
) {
  GlRow(
    title = title,
    leading = { IconTile(icon = icon) },
    onClick = onClick,
    trailing = {
      when {
        feed.loading && !feed.loaded -> CircularProgressIndicator(
          modifier = Modifier.size(16.dp).semantics { contentDescription = "Loading" },
          strokeWidth = 2.dp,
          color = Gl.colors.textTertiary,
        )
        feed.error != null && feed.data.isEmpty() -> Icon(
          Icons.Outlined.ErrorOutline,
          contentDescription = "Couldn't load",
          tint = Gl.colors.danger,
          modifier = Modifier.size(18.dp),
        )
        else -> {
          if (badge != null) GlBadge(text = badge, tone = Tone.Danger)
          else if (count != null && count > 0) CountText(count)
        }
      }
      Spacer(Modifier.width(4.dp))
      Icon(
        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = Gl.colors.textTertiary,
        modifier = Modifier.size(20.dp),
      )
    },
  )
}

@Composable
private fun StarredProjectRow(
  project: GitLabProject,
  pending: Boolean,
  onClick: () -> Unit,
  onToggleStar: (() -> Unit)?,
) {
  val namespace = project.nameWithNamespace.substringBeforeLast("/", "").trim()
  GlRow(
    title = project.name,
    subtitle = namespace.ifBlank { null },
    leading = { ProjectTile(project) },
    onClick = onClick,
    trailing = if (onToggleStar == null) null else {
      {
      IconButton(onClick = onToggleStar, enabled = !pending) {
        SwapIcon(
          target = project.isStarred,
          on = Icons.Outlined.Star,
          off = Icons.Outlined.StarOutline,
          contentDescription = if (project.isStarred) "Unstar ${project.name}" else "Star ${project.name}",
          tintOn = Gl.colors.textPrimary,
          tintOff = Gl.colors.textSecondary,
        )
      }
    }
    },
  )
}

/** Project avatar or its initial on a neutral tile. Shared by the project lists. */
@Composable
internal fun ProjectTile(project: GitLabProject, modifier: Modifier = Modifier) {
  val c = Gl.colors
  val shape = RoundedCornerShape(10.dp)
  // The initial sits underneath so a failed or private avatar still leaves a readable tile.
  Box(
    modifier = modifier.testTag(com.aistudio.gitlab.ui.components.PERSONAL_IMAGE_TAG).size(36.dp).clip(shape).background(c.surface2).border(1.dp, c.imageOutline, shape),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = project.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
      style = Gl.type.label,
      color = c.textSecondary,
    )
    if (!project.avatarUrl.isNullOrBlank()) {
      coil.compose.AsyncImage(
        model = com.aistudio.gitlab.ui.components.rememberImageModel(project.avatarUrl),
        contentDescription = null,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}
