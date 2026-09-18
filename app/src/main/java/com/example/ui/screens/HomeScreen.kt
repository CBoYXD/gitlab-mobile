package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.StarOutline
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
import com.example.data.model.GitLabProject
import com.example.ui.components.GitLabTanukiIcon
import com.example.ui.components.PipelineStatusBadge
import com.example.ui.theme.GitLabDanger
import com.example.ui.theme.GitLabInfo
import com.example.ui.theme.GitLabLightOrange
import com.example.ui.theme.GitLabOrange
import com.example.ui.theme.GitLabPurple
import com.example.ui.theme.GitLabSuccess
import com.example.ui.viewmodel.BottomTab
import com.example.ui.viewmodel.GitLabUiState
import com.example.ui.viewmodel.ScreenDestination

@Composable
fun HomeScreen(
  uiState: GitLabUiState,
  onNavigate: (ScreenDestination) -> Unit,
  onSwitchTab: (BottomTab) -> Unit,
  onOpenInstanceSwitcher: () -> Unit,
  onToggleStar: (Long) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 80.dp)
  ) {
    // Top App Bar with Active Instance Pill
    item {
      HomeHeader(
        activeInstanceName = uiState.activeInstance.name,
        activeInstanceUrl = uiState.activeInstance.url,
        isCustom = uiState.activeInstance.isCustom,
        onInstanceClick = onOpenInstanceSwitcher,
        onSearchClick = { onSwitchTab(BottomTab.PROJECTS) }
      )
    }

    // User Profile Card
    item {
      UserSummaryCard(
        username = uiState.activeInstance.username.ifBlank { "tanuki_engineer" },
        instanceUrl = uiState.activeInstance.url,
        version = uiState.activeInstance.version
      )
    }

    // "My Work" GitHub-style Navigation Items
    item {
      Text(
        text = "My Work",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
      )

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      ) {
        Column {
          WorkItemRow(
            icon = Icons.Outlined.Adjust,
            iconTint = GitLabSuccess,
            title = "Issues",
            count = uiState.issues.count { it.state == "opened" },
            onClick = { onSwitchTab(BottomTab.WORK_ITEMS) }
          )
          WorkItemDivider()
          WorkItemRow(
            icon = Icons.Outlined.AltRoute,
            iconTint = GitLabPurple,
            title = "Merge Requests",
            count = uiState.mergeRequests.count { it.state == "opened" },
            onClick = { onSwitchTab(BottomTab.WORK_ITEMS) }
          )
          WorkItemDivider()
          WorkItemRow(
            icon = Icons.Outlined.PlayCircle,
            iconTint = GitLabOrange,
            title = "CI / CD Pipelines",
            count = uiState.projectPipelines.size,
            onClick = { onSwitchTab(BottomTab.PIPELINES) }
          )
          WorkItemDivider()
          WorkItemRow(
            icon = Icons.Outlined.Folder,
            iconTint = GitLabInfo,
            title = "Projects & Repositories",
            count = uiState.projects.size,
            onClick = { onSwitchTab(BottomTab.PROJECTS) }
          )
        }
      }
    }

    // Favorites / Starred Projects
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Favorite Projects",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "See all (${uiState.projects.size})",
          color = GitLabOrange,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.clickable { onSwitchTab(BottomTab.PROJECTS) }
        )
      }

      val starred = uiState.projects.filter { it.isStarred }.ifEmpty { uiState.projects.take(3) }
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(starred) { project ->
          FavoriteProjectCard(
            project = project,
            onClick = { onNavigate(ScreenDestination.ProjectDetail(project.id)) },
            onToggleStar = { onToggleStar(project.id) }
          )
        }
      }
    }

    // Recent Activity / Pipelines stream
    item {
      Text(
        text = "Recent CI/CD Activity",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
      )

      if (uiState.projectPipelines.isEmpty()) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Text(
            text = "No recent pipeline runs recorded.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
            fontSize = 13.sp
          )
        }
      } else {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
          Column {
            uiState.projectPipelines.take(3).forEachIndexed { index, pipeline ->
              if (index > 0) WorkItemDivider()
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onSwitchTab(BottomTab.PIPELINES) }
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = "Pipeline #${pipeline.id}",
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PipelineStatusBadge(status = pipeline.status)
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "branch: ${pipeline.ref} • ${pipeline.sha.take(8)} • ${pipeline.createdAt}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Icon(
                  imageVector = Icons.Default.ChevronRight,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun HomeHeader(
  activeInstanceName: String,
  activeInstanceUrl: String,
  isCustom: Boolean,
  onInstanceClick: () -> Unit,
  onSearchClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onInstanceClick)
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .padding(horizontal = 10.dp, vertical = 6.dp)
        .testTag("instance_switcher_button")
    ) {
      GitLabTanukiIcon(size = 22.dp)
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = activeInstanceName,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.width(4.dp))
          Box(
            modifier = Modifier
              .size(7.dp)
              .clip(CircleShape)
              .background(GitLabSuccess)
          )
        }
        Text(
          text = activeInstanceUrl.removePrefix("https://").removePrefix("http://"),
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Spacer(modifier = Modifier.width(4.dp))
      Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = "Switch Instance",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp)
      )
    }

    IconButton(
      onClick = onSearchClick,
      modifier = Modifier.testTag("home_search_button")
    ) {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = "Search",
        tint = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
fun UserSummaryCard(
  username: String,
  instanceUrl: String,
  version: String
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
    shape = RoundedCornerShape(14.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(GitLabOrange),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = username.take(1).uppercase(),
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "@$username",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "GitLab $version • Connected",
          fontSize = 12.sp,
          color = GitLabSuccess,
          fontWeight = FontWeight.Medium
        )
      }
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
      ) {
        Text(
          text = "v4 API",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }
    }
  }
}

@Composable
fun WorkItemRow(
  icon: ImageVector,
  iconTint: Color,
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
    Box(
      modifier = Modifier
        .size(34.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(iconTint.copy(alpha = 0.12f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = iconTint,
        modifier = Modifier.size(20.dp)
      )
    }
    Spacer(modifier = Modifier.width(14.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.weight(1f)
    )
    if (count > 0) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
      ) {
        Text(
          text = count.toString(),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
      }
    }
    Spacer(modifier = Modifier.width(8.dp))
    Icon(
      imageVector = Icons.Default.ChevronRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
      modifier = Modifier.size(18.dp)
    )
  }
}

@Composable
fun WorkItemDivider() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 64.dp)
      .height(0.6.dp)
      .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
  )
}

@Composable
fun FavoriteProjectCard(
  project: GitLabProject,
  onClick: () -> Unit,
  onToggleStar: () -> Unit
) {
  Card(
    modifier = Modifier
      .width(220.dp)
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(22.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(GitLabPurple.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = project.name.take(1).uppercase(),
              color = GitLabPurple,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = project.name,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        IconButton(
          onClick = onToggleStar,
          modifier = Modifier.size(24.dp)
        ) {
          Icon(
            imageVector = if (project.isStarred) Icons.Filled.Star else Icons.Outlined.StarOutline,
            contentDescription = "Star",
            tint = if (project.isStarred) GitLabLightOrange else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = project.description ?: "GitLab Repository",
        fontSize = 11.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 15.sp
      )

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = GitLabLightOrange,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = "${project.starCount}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(4.dp)
        ) {
          Text(
            text = project.visibility,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
          )
        }
      }
    }
  }
}
