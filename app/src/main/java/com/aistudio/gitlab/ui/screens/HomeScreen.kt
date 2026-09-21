package com.aistudio.gitlab.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Domain
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.ViewQuilt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.aistudio.gitlab.data.model.GitLabProject
import com.aistudio.gitlab.ui.components.GitLabTanukiIcon
import com.aistudio.gitlab.ui.theme.GitLabOrange
import com.aistudio.gitlab.ui.theme.GitLabSuccess
import com.aistudio.gitlab.ui.viewmodel.BottomTab
import com.aistudio.gitlab.ui.viewmodel.GitLabUiState
import com.aistudio.gitlab.ui.viewmodel.NotificationType
import com.aistudio.gitlab.ui.viewmodel.ScreenDestination

@Composable
fun HomeScreen(
  uiState: GitLabUiState,
  onNavigate: (ScreenDestination) -> Unit,
  onSwitchTab: (BottomTab) -> Unit,
  onOpenInstanceSwitcher: () -> Unit,
  onOpenProfile: () -> Unit,
  onToggleStar: (Long) -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(bottom = 88.dp)
  ) {
    // 1. Top App Bar - Exact GitHub Mobile Layout (from screenshots 2 & 3)
    item {
      GitHubHomeTopBar(
        activeInstanceName = uiState.activeInstance?.name ?: "Connect GitLab",
        username = uiState.currentUser?.username?.takeIf { it.isNotBlank() }
          ?: uiState.activeInstance?.username?.takeIf { it.isNotBlank() }
          ?: "Sign in",
        onSearchClick = { onSwitchTab(BottomTab.PROJECTS) },
        onRefreshClick = onRefresh,
        onAddClick = onOpenInstanceSwitcher,
        onAvatarClick = onOpenProfile
      )
    }

    // 2. Active Server Connection Pill
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenInstanceSwitcher)
            .testTag("instance_switcher_button"),
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            GitLabTanukiIcon(size = 14.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = uiState.activeInstance?.name ?: "Connect GitLab",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (uiState.activeInstance?.statusOk == true) GitLabSuccess else MaterialTheme.colorScheme.outline)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Default.ChevronRight,
              contentDescription = "Switch Server",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }

    // 3. "My Work" Section (exact match from Screenshot 2 & 3)
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "My Work",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Icon(
          imageVector = Icons.Default.MoreHoriz,
          contentDescription = "Options",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp)
        )
      }

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      ) {
        Column {
          // 1. Issues (Solid green squircle)
          GitHubWorkItemRow(
            icon = Icons.Outlined.Adjust,
            squircleBg = Color(0xFF2EA44F),
            title = "Issues",
            count = uiState.issues.count { it.state == "opened" },
            onClick = { onNavigate(ScreenDestination.IssuesList) }
          )
          WorkItemDivider()

          // 2. Merge Requests (Solid blue squircle) - STRICTLY GITLAB NAMING
          GitHubWorkItemRow(
            icon = Icons.Outlined.AltRoute,
            squircleBg = Color(0xFF1F6FEB),
            title = "Merge Requests",
            count = uiState.mergeRequests.count { it.state == "opened" },
            onClick = { onNavigate(ScreenDestination.MergeRequestsList) }
          )
          WorkItemDivider()

          // 3. CI / CD Pipelines (Solid orange squircle)
          GitHubWorkItemRow(
            icon = Icons.Filled.PlayCircle,
            squircleBg = Color(0xFFFC6D26),
            title = "CI / CD Pipelines",
            count = uiState.projectPipelines.size,
            onClick = { onNavigate(ScreenDestination.PipelinesList) }
          )
          WorkItemDivider()

          // 4. Discussions (Solid purple squircle)
          GitHubWorkItemRow(
            icon = Icons.Outlined.ChatBubbleOutline,
            squircleBg = Color(0xFF8957E5),
            title = "Discussions",
            count = uiState.notifications.count {
              it.type == NotificationType.DISCUSSION || it.type == NotificationType.MENTION
            },
            onClick = { onSwitchTab(BottomTab.INBOX) }
          )
          WorkItemDivider()

          // 5. Projects (Solid slate squircle)
          GitHubWorkItemRow(
            icon = Icons.Outlined.ViewQuilt,
            squircleBg = Color(0xFF6E7681),
            title = "Projects",
            count = uiState.projects.size,
            onClick = { onSwitchTab(BottomTab.PROJECTS) }
          )
          WorkItemDivider()

          // 6. Groups (GitLab naming!) (Solid orange squircle)
          GitHubWorkItemRow(
            icon = Icons.Outlined.Domain,
            squircleBg = Color(0xFFF0883E),
            title = "Groups",
            count = uiState.groups.size,
            onClick = { onSwitchTab(BottomTab.PROJECTS) }
          )
          WorkItemDivider()

          // 7. Starred Projects (GitLab naming!) (Solid yellow squircle)
          GitHubWorkItemRow(
            icon = Icons.Filled.Star,
            squircleBg = Color(0xFFD29922),
            title = "Starred Projects",
            count = uiState.projects.count { it.isStarred },
            onClick = { onSwitchTab(BottomTab.PROJECTS) }
          )
        }
      }
    }

    // 4. "Favorites" Section (exact match from Screenshot 2 & 3)
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Favorites",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Icon(
          imageVector = Icons.Default.MoreHoriz,
          contentDescription = "Options",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp)
        )
      }

      val starredProjects = uiState.projects.filter { it.isStarred }

      if (starredProjects.isEmpty()) {
        // Empty Favorites State (Screenshot 3)
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Add favorite projects for quick access at any time, without having to search",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSwitchTab(BottomTab.PROJECTS) },
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant
            ) {
              Text(
                text = "ADD FAVORITE PROJECTS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = GitLabOrange,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp)
              )
            }
          }
        }
      } else {
        // Populated Favorites List (Screenshot 2: e.g. GrowCrypt / copytrade)
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
          Column {
            starredProjects.forEachIndexed { index, project ->
              if (index > 0) WorkItemDivider()
              FavoriteRepoRow(
                project = project,
                onClick = { onNavigate(ScreenDestination.ProjectDetail(project.id)) },
                onToggleStar = { onToggleStar(project.id) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun GitHubHomeTopBar(
  activeInstanceName: String,
  username: String,
  onSearchClick: () -> Unit,
  onRefreshClick: () -> Unit,
  onAddClick: () -> Unit,
  onAvatarClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = "Home",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(
        onClick = onSearchClick,
        modifier = Modifier.size(40.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = "Search",
          tint = MaterialTheme.colorScheme.onBackground
        )
      }

      IconButton(
        onClick = onRefreshClick,
        modifier = Modifier.size(40.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Refresh",
          tint = MaterialTheme.colorScheme.onBackground
        )
      }

      IconButton(
        onClick = onAddClick,
        modifier = Modifier.size(40.dp)
      ) {
        Icon(
          imageVector = Icons.Outlined.AddCircleOutline,
          contentDescription = "Add",
          tint = MaterialTheme.colorScheme.onBackground
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      // User Avatar Circle (Screenshot 1 & 2)
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(Color(0xFF2E7D32))
          .border(1.5.dp, GitLabOrange, CircleShape)
          .clickable(onClick = onAvatarClick)
          .testTag("home_avatar_button"),
        contentAlignment = Alignment.Center
      ) {
        GitLabTanukiIcon(size = 20.dp)
      }
    }
  }
}

@Composable
fun GitHubWorkItemRow(
  icon: ImageVector,
  squircleBg: Color,
  title: String,
  count: Int,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 13.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Solid Vibrant Rounded Squircle with White Icon
    Box(
      modifier = Modifier
        .size(32.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(squircleBg),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = Color.White,
        modifier = Modifier.size(18.dp)
      )
    }

    Spacer(modifier = Modifier.width(14.dp))

    Text(
      text = title,
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f)
    )

    if (count > 0) {
      Text(
        text = count.toString(),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.width(6.dp))
    }

    Icon(
      imageVector = Icons.Default.ChevronRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
      modifier = Modifier.size(18.dp)
    )
  }
}

@Composable
fun FavoriteRepoRow(
  project: GitLabProject,
  onClick: () -> Unit,
  onToggleStar: () -> Unit
) {
  val parts = project.pathWithNamespace.split("/")
  val namespace = if (parts.size > 1) parts[0] else "GitLab"
  val repoName = if (parts.size > 1) parts[1] else project.name

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 13.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Repo icon / logo (matches Screenshot 2 GrowCrypt)
    Box(
      modifier = Modifier
        .size(32.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = repoName.take(1).uppercase(),
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = GitLabOrange
      )
    }

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = namespace,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = repoName,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    IconButton(
      onClick = onToggleStar,
      modifier = Modifier.size(32.dp)
    ) {
      Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = "Starred",
        tint = Color(0xFFD29922),
        modifier = Modifier.size(18.dp)
      )
    }
  }
}

@Composable
fun WorkItemDivider() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 62.dp)
      .height(0.6.dp)
      .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
  )
}
