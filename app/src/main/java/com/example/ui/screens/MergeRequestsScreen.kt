package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GitLabMergeRequest
import com.example.ui.components.GitLabLabelChip
import com.example.ui.components.MergeRequestStateIcon
import com.example.ui.theme.GitLabOrange
import com.example.ui.theme.GitLabPurple
import com.example.ui.theme.GitLabSuccess

@Composable
fun MergeRequestsScreen(
  mergeRequests: List<GitLabMergeRequest>,
  onSelectMergeRequest: (GitLabMergeRequest) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var stateFilter by remember { mutableStateOf("Opened") }

  val filteredMRs = when (stateFilter) {
    "Opened" -> mergeRequests.filter { it.state == "opened" }
    "Merged" -> mergeRequests.filter { it.state == "merged" }
    "Closed" -> mergeRequests.filter { it.state == "closed" }
    else -> mergeRequests
  }

  val openCount = mergeRequests.count { it.state == "opened" }

  Scaffold(
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(MaterialTheme.colorScheme.background)
    ) {
      // Top Bar - GitLab Naming: Merge Requests
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 6.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("mrs_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onBackground
            )
          }
          Text(
            text = "Merge Requests",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = GitLabPurple.copy(alpha = 0.15f)
          ) {
            Text(
              text = "$openCount open",
              color = GitLabPurple,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
          }
        }
      }

      // Filter Chips Row
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf("Opened", "Merged", "Closed", "All").forEach { state ->
          item {
            val isSelected = (stateFilter == state)
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { stateFilter = state },
              color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
              border = androidx.compose.foundation.BorderStroke(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) GitLabOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
              ),
              shape = RoundedCornerShape(20.dp)
            ) {
              Text(
                text = state,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) GitLabOrange else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Merge Requests List
      if (filteredMRs.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.AltRoute,
                contentDescription = null,
                tint = GitLabPurple,
                modifier = Modifier.size(32.dp)
              )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = "No $stateFilter merge requests",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "There are no merge requests matching this filter.",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = 88.dp)
        ) {
          items(filteredMRs, key = { it.id }) { mr ->
            MergeRequestItemRow(
              mr = mr,
              onClick = { onSelectMergeRequest(mr) }
            )
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 54.dp)
                .height(0.6.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MergeRequestItemRow(
  mr: GitLabMergeRequest,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.Top
  ) {
    MergeRequestStateIcon(state = mr.state)
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = mr.title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "!${mr.iid} · ${mr.sourceBranch} → ${mr.targetBranch}",
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = "by ${mr.author.name} · updated ${mr.updatedAt.take(10)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (mr.labels.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          mr.labels.take(3).forEach { label ->
            GitLabLabelChip(label = label)
          }
        }
      }
    }

    if (mr.userNotesCount > 0) {
      Spacer(modifier = Modifier.width(8.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 2.dp)
      ) {
        Icon(
          imageVector = Icons.Default.ChatBubbleOutline,
          contentDescription = "Comments",
          modifier = Modifier.size(13.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
          text = mr.userNotesCount.toString(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
