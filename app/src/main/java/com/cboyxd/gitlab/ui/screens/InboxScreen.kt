package com.cboyxd.gitlab.ui.screens

import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.PlayCircleOutline
import com.cboyxd.gitlab.ui.components.ListsDropdownFilter
import com.cboyxd.gitlab.ui.components.ListsFilterBar
import com.cboyxd.gitlab.ui.components.compactAge
import com.cboyxd.gitlab.ui.viewmodel.NotificationType
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.ui.components.Avatar
import com.cboyxd.gitlab.ui.components.EmptyState
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.GlBadge
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlFilterRow
import com.cboyxd.gitlab.ui.components.GlIconAction
import com.cboyxd.gitlab.ui.components.GlLargeHeader
import com.cboyxd.gitlab.ui.components.InlineError
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.components.Tone
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.GitLabNotification
import com.cboyxd.gitlab.ui.viewmodel.GitLabUiState
import com.cboyxd.gitlab.ui.viewmodel.InboxFilter

private val AssignedActions = setOf("assigned", "review_requested", "approval_required")
private val MentionActions = setOf("mentioned", "directly_addressed")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
  state: GitLabUiState,
  onRefresh: () -> Unit,
  onFilterChange: (InboxFilter) -> Unit,
  onOpen: (GitLabNotification) -> Unit,
  onMarkDone: (String) -> Unit,
  onMarkAllDone: () -> Unit,
  onAddServer: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val feed = state.notifications
  val unread = state.unreadCount
  val filter = state.inboxFilter
  val visible = feed.data.filter { n ->
    when (filter) {
      InboxFilter.ALL -> true
      InboxFilter.UNREAD -> n.isUnread
      InboxFilter.ASSIGNED -> n.actionName in AssignedActions
      InboxFilter.MENTIONED -> n.actionName in MentionActions
    }
  }

  Column(modifier = modifier.fillMaxSize().background(Gl.colors.bg)) {
    GlLargeHeader(title = "Inbox") {
      if (state.isSignedIn) {
        GlIconAction(
          icon = Icons.Outlined.DoneAll,
          contentDescription = "Mark all as done",
          onClick = onMarkAllDone,
          enabled = unread > 0,
        )
      }
    }

    if (state.activeInstance == null || !state.isSignedIn) {
      EmptyState(
        icon = if (state.activeInstance == null) Icons.Outlined.CloudOff else Icons.Outlined.Inbox,
        title = "Sign in to see your inbox",
        body = "Your to-dos, mentions and review requests appear here once you sign in.",
        actionLabel = "Sign in",
        onAction = onAddServer,
      )
      return@Column
    }

    ListsFilterBar {
      ListsDropdownFilter(
        name = "Show",
        options = InboxFilter.entries,
        selected = filter,
        label = { it.label },
        onSelect = onFilterChange,
        active = filter != InboxFilter.ALL,
      )
      if (unread > 0) {
        Text(
          if (unread == 1) "1 to-do" else "$unread to-dos",
          style = Gl.type.bodySmall,
          color = Gl.colors.textSecondary,
        )
      }
    }
    GlDivider()

    PullToRefreshBox(
      isRefreshing = state.refreshing,
      onRefresh = onRefresh,
      modifier = Modifier.fillMaxSize(),
    ) {
      when {
        feed.initialLoading -> LoadingState(label = "Loading to-dos")
        feed.error != null && feed.data.isEmpty() -> ErrorState(message = feed.error, onRetry = onRefresh)
        visible.isEmpty() -> LazyColumn(modifier = Modifier.fillMaxSize()) {
          item {
            val (title, body) = when (filter) {
              InboxFilter.ALL -> "No to-dos" to "Mentions, assignments and review requests show up here."
              InboxFilter.UNREAD -> "No to-dos" to "Nothing needs your attention right now."
              InboxFilter.ASSIGNED -> "Nothing assigned" to "Issues and merge requests assigned to you or waiting for your review show up here."
              InboxFilter.MENTIONED -> "No mentions" to "When someone mentions you in a comment, it shows up here."
            }
            EmptyState(icon = Icons.Outlined.Inbox, title = title, body = body)
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
          items(visible, key = { it.id }) { notification ->
            NotificationRow(
              notification = notification,
              onClick = { onOpen(notification) },
              onMarkDone = { onMarkDone(notification.id) },
            )
            GlDivider()
          }
        }
      }
    }
  }
}

/** To-do in the GitHub inbox manner: type icon with an unread dot, "project · age", title, reason. */
@Composable
private fun NotificationRow(
  notification: GitLabNotification,
  onClick: () -> Unit,
  onMarkDone: () -> Unit,
) {
  val c = Gl.colors
  val tone = when (notification.actionName) {
    "build_failed", "unmergeable" -> Tone.Danger
    "review_requested", "assigned", "approval_required" -> Tone.Info
    else -> Tone.Neutral
  }
  val (icon, iconTint) = when (notification.type) {
    NotificationType.ISSUE -> Icons.Outlined.Adjust to c.success
    NotificationType.MERGE_REQUEST -> Icons.Outlined.MergeType to c.success
    NotificationType.PIPELINE -> Icons.Outlined.PlayCircleOutline to if (tone == Tone.Danger) c.danger else c.warning
    NotificationType.MENTION -> Icons.Outlined.AlternateEmail to c.accent
    NotificationType.DISCUSSION -> Icons.Outlined.ChatBubbleOutline to c.textSecondary
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open")
      .defaultMinSize(minHeight = 72.dp)
      .padding(start = 8.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Box(Modifier.padding(top = 8.dp).size(8.dp)) {
      if (notification.isUnread) Box(Modifier.size(8.dp).clip(CircleShape).background(c.accent))
    }
    Spacer(Modifier.width(8.dp))
    Box(Modifier.padding(top = 1.dp)) {
      Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
    }
    Spacer(Modifier.width(16.dp))
    Column(
      modifier = Modifier
        .weight(1f)
        .semantics(mergeDescendants = true) {
          contentDescription = buildString {
            if (notification.isUnread) append("To do. ")
            notification.statusBadge?.let { append(it).append(". ") }
            append(notification.title)
            if (notification.projectPath.isNotBlank()) append(", in ").append(notification.projectPath)
            if (notification.authorName.isNotBlank()) append(", from ").append(notification.authorName)
            if (notification.timestamp.isNotBlank()) append(", ").append(notification.timestamp)
          }
        },
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = notification.projectPath.replace("/", " / ").ifBlank { notification.authorName },
          style = Gl.type.bodySmall,
          color = c.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        val age = compactAge(notification.timestamp)
        if (age.isNotBlank()) {
          Spacer(Modifier.width(12.dp))
          Text(age, style = Gl.type.bodySmall, color = c.textSecondary, maxLines = 1)
        }
      }
      Spacer(Modifier.height(2.dp))
      Text(
        text = notification.title,
        style = Gl.type.rowTitle.copy(fontWeight = if (notification.isUnread) FontWeight.SemiBold else FontWeight.Normal),
        color = c.textPrimary,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )
      if (notification.subtitle.isNotBlank()) {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Avatar(url = notification.authorAvatarUrl, name = notification.authorName, size = 18.dp)
          Spacer(Modifier.width(6.dp))
          Text(
            text = notification.subtitle,
            style = Gl.type.bodySmall,
            color = c.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      notification.statusBadge?.let { badge ->
        Spacer(Modifier.height(8.dp))
        GlBadge(text = badge, tone = tone)
      }
    }
    if (notification.isUnread) {
      IconButton(onClick = onMarkDone) {
        Icon(Icons.Outlined.Check, contentDescription = "Mark as done", tint = c.accent)
      }
    } else {
      Spacer(Modifier.width(12.dp))
    }
  }
}
