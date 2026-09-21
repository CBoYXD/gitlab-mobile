package com.aistudio.gitlab.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.gitlab.data.model.GitLabIssue
import com.aistudio.gitlab.data.model.GitLabMergeRequest
import com.aistudio.gitlab.data.model.GitLabProject
import com.aistudio.gitlab.ui.components.GitLabLabelChip
import com.aistudio.gitlab.ui.components.IssueStateIcon
import com.aistudio.gitlab.ui.components.MergeRequestStateIcon
import com.aistudio.gitlab.ui.theme.GitLabMerged
import com.aistudio.gitlab.ui.theme.GitLabOrange
import com.aistudio.gitlab.ui.theme.GitLabPurple
import com.aistudio.gitlab.ui.theme.GitLabSuccess
import com.aistudio.gitlab.ui.viewmodel.ScreenDestination

@Composable
fun WorkItemsScreen(
  issues: List<GitLabIssue>,
  mergeRequests: List<GitLabMergeRequest>,
  projects: List<GitLabProject>,
  onSelectIssue: (GitLabIssue) -> Unit,
  onSelectMergeRequest: (GitLabMergeRequest) -> Unit,
  onCreateIssue: (projectId: Long, title: String, description: String?, labels: String?) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTabIndex by remember { mutableIntStateOf(0) }
  var stateFilter by remember { mutableStateOf("Opened") }
  var showCreateIssueDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    floatingActionButton = {
      if (selectedTabIndex == 0) {
        FloatingActionButton(
          onClick = { showCreateIssueDialog = true },
          containerColor = GitLabOrange,
          contentColor = Color.White,
          modifier = Modifier.testTag("create_issue_fab")
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = "New Issue")
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // Screen Title
      Text(
        text = "Work Items",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
      )

      // Tab switcher: Issues vs Merge Requests
      TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = GitLabOrange
      ) {
        Tab(
          selected = (selectedTabIndex == 0),
          onClick = { selectedTabIndex = 0 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("Issues", fontWeight = FontWeight.SemiBold)
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                color = if (selectedTabIndex == 0) GitLabOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
              ) {
                Text(
                  text = "${issues.count { it.state == "opened" }}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (selectedTabIndex == 0) GitLabOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          },
          modifier = Modifier.testTag("tab_issues")
        )
        Tab(
          selected = (selectedTabIndex == 1),
          onClick = { selectedTabIndex = 1 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("Merge Requests", fontWeight = FontWeight.SemiBold)
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                color = if (selectedTabIndex == 1) GitLabPurple.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
              ) {
                Text(
                  text = "${mergeRequests.count { it.state == "opened" }}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (selectedTabIndex == 1) GitLabPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          },
          modifier = Modifier.testTag("tab_merge_requests")
        )
      }

      // Filter Chips
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val filters = if (selectedTabIndex == 0) {
          listOf("Opened", "Closed", "All")
        } else {
          listOf("Opened", "Merged", "Closed", "All")
        }
        items(filters) { filter ->
          FilterChip(
            selected = (stateFilter == filter),
            onClick = { stateFilter = filter },
            label = { Text(filter, fontSize = 12.sp) },
            shape = RoundedCornerShape(16.dp)
          )
        }
      }

      if (selectedTabIndex == 0) {
        // Issues List
        val filteredIssues = issues.filter {
          when (stateFilter) {
            "Opened" -> it.state == "opened"
            "Closed" -> it.state == "closed"
            else -> true
          }
        }
        if (filteredIssues.isEmpty()) {
          EmptyState("No issues matching filter")
        } else {
          LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(filteredIssues, key = { it.id }) { issue ->
              IssueItemCard(issue = issue, onClick = { onSelectIssue(issue) })
            }
          }
        }
      } else {
        // Merge Requests List
        val filteredMRs = mergeRequests.filter {
          when (stateFilter) {
            "Opened" -> it.state == "opened"
            "Merged" -> it.state == "merged"
            "Closed" -> it.state == "closed"
            else -> true
          }
        }
        if (filteredMRs.isEmpty()) {
          EmptyState("No merge requests matching filter")
        } else {
          LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(filteredMRs, key = { it.id }) { mr ->
              MergeRequestItemCard(mr = mr, onClick = { onSelectMergeRequest(mr) })
            }
          }
        }
      }
    }
  }

  if (showCreateIssueDialog) {
    CreateIssueDialog(
      projects = projects,
      onDismiss = { showCreateIssueDialog = false },
      onSubmit = { projId, title, desc, labels ->
        onCreateIssue(projId, title, desc, labels)
        showCreateIssueDialog = false
      }
    )
  }
}

@Composable
fun IssueItemCard(
  issue: GitLabIssue,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("issue_item_${issue.id}"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.Top
    ) {
      IssueStateIcon(state = issue.state, modifier = Modifier.padding(top = 2.dp))
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = issue.title,
          fontWeight = FontWeight.Bold,
          fontSize = 14.5.sp,
          color = MaterialTheme.colorScheme.onSurface,
          lineHeight = 19.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "#${issue.iid} opened ${issue.createdAt} by @${issue.author.username}",
          fontSize = 11.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (issue.labels.isNotEmpty()) {
          Spacer(modifier = Modifier.height(8.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(issue.labels) { label ->
              GitLabLabelChip(label = label)
            }
          }
        }
      }
      if (issue.userNotesCount > 0) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(start = 8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = "${issue.userNotesCount}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
fun MergeRequestItemCard(
  mr: GitLabMergeRequest,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("mr_item_${mr.id}"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.Top
    ) {
      MergeRequestStateIcon(state = mr.state, modifier = Modifier.padding(top = 2.dp))
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        if (mr.isDraft) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.padding(bottom = 4.dp)
          ) {
            Text(
              text = "Draft",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }
        }
        Text(
          text = mr.title,
          fontWeight = FontWeight.Bold,
          fontSize = 14.5.sp,
          color = MaterialTheme.colorScheme.onSurface,
          lineHeight = 19.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = "${mr.sourceBranch} → ${mr.targetBranch}",
              fontSize = 11.sp,
              fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "!${mr.iid} opened by @${mr.author.username}",
          fontSize = 11.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      if (!mr.changesCount.isNullOrBlank()) {
        Surface(
          color = GitLabSuccess.copy(alpha = 0.12f),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier.padding(start = 6.dp)
        ) {
          Text(
            text = "+${mr.changesCount}",
            color = GitLabSuccess,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
          )
        }
      }
    }
  }
}

@Composable
fun EmptyState(message: String) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = message,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 14.sp
    )
  }
}

@Composable
fun CreateIssueDialog(
  projects: List<GitLabProject>,
  onDismiss: () -> Unit,
  onSubmit: (projectId: Long, title: String, description: String?, labels: String?) -> Unit
) {
  var selectedProjectId by remember { mutableStateOf(projects.firstOrNull()?.id ?: 0L) }
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var labels by remember { mutableStateOf("feature,mobile") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Issue", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Target Project:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(projects) { p ->
            FilterChip(
              selected = (selectedProjectId == p.id),
              onClick = { selectedProjectId = p.id },
              label = { Text(p.name, fontSize = 11.sp) }
            )
          }
        }
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Issue Title") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description") },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 4
        )
        OutlinedTextField(
          value = labels,
          onValueChange = { labels = it },
          label = { Text("Labels (comma-separated)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmit(selectedProjectId, title, description, labels) },
        enabled = title.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = GitLabOrange)
      ) {
        Text("Submit Issue")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}
