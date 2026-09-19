package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GitLabOrange
import com.example.ui.theme.GitLabPurple
import com.example.ui.theme.GitLabSuccess
import com.example.ui.viewmodel.BottomTab
import com.example.ui.viewmodel.GitLabNotification
import com.example.ui.viewmodel.InboxFilter
import com.example.ui.viewmodel.NotificationType
import com.example.ui.viewmodel.ScreenDestination

@Composable
fun InboxScreen(
  notifications: List<GitLabNotification>,
  currentFilter: InboxFilter,
  onFilterChange: (InboxFilter) -> Unit,
  onToggleRead: (String) -> Unit,
  onMarkAllRead: () -> Unit,
  onNavigateToWorkItems: () -> Unit,
  onNavigateToPipelines: () -> Unit,
  modifier: Modifier = Modifier
) {
  val unreadCount = notifications.count { it.isUnread }

  val filteredNotifications = when (currentFilter) {
    InboxFilter.ALL -> notifications
    InboxFilter.UNREAD -> notifications.filter { it.isUnread }
    InboxFilter.ASSIGNED -> notifications.filter {
      it.type == NotificationType.ISSUE || it.statusBadge?.contains("Assigned", ignoreCase = true) == true
    }
    InboxFilter.MENTIONED -> notifications.filter {
      it.type == NotificationType.MENTION || it.statusBadge?.contains("Mentioned", ignoreCase = true) == true
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Top Bar - GitHub style
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "Inbox",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        if (unreadCount > 0) {
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            color = GitLabOrange.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(top = 2.dp)
          ) {
            Text(
              text = "$unreadCount unread",
              color = GitLabOrange,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
          }
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onMarkAllRead,
          modifier = Modifier.testTag("inbox_mark_all_read_button")
        ) {
          Icon(
            imageVector = Icons.Outlined.DoneAll,
            contentDescription = "Mark all as read",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // Filter Chips
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(InboxFilter.values()) { filter ->
        val isSelected = (currentFilter == filter)
        val filterLabel = when (filter) {
          InboxFilter.ALL -> "All"
          InboxFilter.UNREAD -> if (unreadCount > 0) "Unread ($unreadCount)" else "Unread"
          InboxFilter.ASSIGNED -> "Assigned"
          InboxFilter.MENTIONED -> "Mentioned"
        }

        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onFilterChange(filter) },
          color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
          border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) GitLabOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
          ),
          shape = RoundedCornerShape(20.dp)
        ) {
          Text(
            text = filterLabel,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) GitLabOrange else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Notification List
    if (filteredNotifications.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(68.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.Inbox,
              contentDescription = null,
              tint = GitLabOrange,
              modifier = Modifier.size(36.dp)
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = if (currentFilter == InboxFilter.UNREAD) "All caught up!" else "No notifications",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = if (currentFilter == InboxFilter.UNREAD)
              "You have read all your notifications. Take a break!"
            else
              "No activity matches the selected filter.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
      ) {
        items(filteredNotifications, key = { it.id }) { notification ->
          NotificationItemRow(
            notification = notification,
            onItemClick = {
              onToggleRead(notification.id)
              when (notification.type) {
                NotificationType.PIPELINE -> onNavigateToPipelines()
                else -> onNavigateToWorkItems()
              }
            },
            onToggleRead = { onToggleRead(notification.id) }
          )
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 64.dp)
              .height(0.6.dp)
              .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
          )
        }
      }
    }
  }
}

@Composable
fun NotificationItemRow(
  notification: GitLabNotification,
  onItemClick: () -> Unit,
  onToggleRead: () -> Unit
) {
  val (icon, squircleColor) = when (notification.type) {
    NotificationType.MERGE_REQUEST -> Pair(Icons.Outlined.AltRoute, Color(0xFF1F6FEB))
    NotificationType.ISSUE -> Pair(Icons.Outlined.Adjust, Color(0xFF2EA44F))
    NotificationType.PIPELINE -> Pair(Icons.Filled.PlayCircle, Color(0xFFFC6D26))
    NotificationType.MENTION -> Pair(Icons.Outlined.AlternateEmail, Color(0xFF8957E5))
    NotificationType.DISCUSSION -> Pair(Icons.Outlined.ChatBubbleOutline, Color(0xFF8957E5))
  }

  val backgroundColor by animateColorAsState(
    targetValue = if (notification.isUnread)
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    else
      Color.Transparent,
    label = "item_bg"
  )

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(backgroundColor)
      .clickable(onClick = onItemClick)
      .padding(horizontal = 16.dp, vertical = 13.dp),
    verticalAlignment = Alignment.Top
  ) {
    // Solid Vibrant Rounded Squircle Icon (GitHub Mobile signature style)
    Box(
      modifier = Modifier
        .size(34.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(squircleColor),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(20.dp)
      )
    }

    Spacer(modifier = Modifier.width(12.dp))

    // Content
    Column(modifier = Modifier.weight(1f)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = notification.projectPath,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = notification.timestamp,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = notification.title,
        fontSize = 14.sp,
        fontWeight = if (notification.isUnread) FontWeight.Bold else FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      if (notification.subtitle.isNotBlank()) {
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = notification.subtitle,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      if (notification.statusBadge != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          color = squircleColor.copy(alpha = 0.12f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = notification.statusBadge,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = squircleColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.width(10.dp))

    // Unread indicator dot & quick toggle
    Box(
      modifier = Modifier
        .size(32.dp)
        .clickable(onClick = onToggleRead),
      contentAlignment = Alignment.Center
    ) {
      if (notification.isUnread) {
        Box(
          modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(GitLabOrange)
        )
      }
    }
  }
}
