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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.gitlab.data.model.GitLabIssue
import com.aistudio.gitlab.data.model.GitLabProject
import com.aistudio.gitlab.ui.components.GitLabLabelChip
import com.aistudio.gitlab.ui.components.IssueStateIcon
import com.aistudio.gitlab.ui.theme.GitLabOrange
import com.aistudio.gitlab.ui.theme.GitLabSuccess

@Composable
fun IssuesScreen(
  issues: List<GitLabIssue>,
  projects: List<GitLabProject>,
  onSelectIssue: (GitLabIssue) -> Unit,
  onCreateIssue: (projectId: Long, title: String, description: String?, labels: String?) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var stateFilter by remember { mutableStateOf("Opened") }
  var showCreateIssueDialog by remember { mutableStateOf(false) }

  val filteredIssues = when (stateFilter) {
    "Opened" -> issues.filter { it.state == "opened" }
    "Closed" -> issues.filter { it.state == "closed" }
    else -> issues
  }

  val openCount = issues.count { it.state == "opened" }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showCreateIssueDialog = true },
        containerColor = GitLabOrange,
        contentColor = Color.White,
        modifier = Modifier.testTag("create_issue_fab")
      ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = "New Issue")
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(MaterialTheme.colorScheme.background)
    ) {
      // Top Bar
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
            modifier = Modifier.testTag("issues_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onBackground
            )
          }
          Text(
            text = "Issues",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = GitLabSuccess.copy(alpha = 0.15f)
          ) {
            Text(
              text = "$openCount open",
              color = GitLabSuccess,
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
        listOf("Opened", "Closed", "All").forEach { state ->
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

      // Issues List
      if (filteredIssues.isEmpty()) {
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
                imageVector = Icons.Outlined.Adjust,
                contentDescription = null,
                tint = GitLabOrange,
                modifier = Modifier.size(32.dp)
              )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = "No $stateFilter issues",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "There are no issues matching this filter.",
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
          items(filteredIssues, key = { it.id }) { issue ->
            IssueItemRow(
              issue = issue,
              onClick = { onSelectIssue(issue) }
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

  // Create Issue Dialog
  if (showCreateIssueDialog) {
    CreateIssueModalDialog(
      projects = projects,
      onDismiss = { showCreateIssueDialog = false },
      onCreate = { projId, title, desc, labels ->
        onCreateIssue(projId, title, desc, labels)
        showCreateIssueDialog = false
      }
    )
  }
}

@Composable
private fun IssueItemRow(
  issue: GitLabIssue,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.Top
  ) {
    IssueStateIcon(state = issue.state)
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = issue.title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = "#${issue.iid} · opened ${issue.createdAt.take(10)} by ${issue.author.name}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      if (issue.labels.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          issue.labels.take(3).forEach { label ->
            GitLabLabelChip(label = label)
          }
          if (issue.labels.size > 3) {
            Text(
              text = "+${issue.labels.size - 3}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.align(Alignment.CenterVertically)
            )
          }
        }
      }
    }

    if (issue.userNotesCount > 0) {
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
          text = issue.userNotesCount.toString(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun CreateIssueModalDialog(
  projects: List<GitLabProject>,
  onDismiss: () -> Unit,
  onCreate: (projectId: Long, title: String, description: String?, labels: String?) -> Unit
) {
  var selectedProjectId by remember { mutableStateOf(projects.firstOrNull()?.id ?: 0L) }
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var labels by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Issue", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        if (projects.isEmpty()) {
          Text(
            text = "No projects available on this server.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        } else {
          Text(
            text = "Project",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = GitLabOrange
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(projects, key = { it.id }) { project ->
              FilterChip(
                selected = project.id == selectedProjectId,
                onClick = { selectedProjectId = project.id },
                label = { Text(project.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
              )
            }
          }
        }

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Title *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description (Markdown)") },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = labels,
          onValueChange = { labels = it },
          label = { Text("Labels (comma separated)") },
          placeholder = { Text("bug, priority") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            onCreate(selectedProjectId, title, description.ifBlank { null }, labels.ifBlank { null })
          }
        },
        enabled = title.isNotBlank() && selectedProjectId > 0,
        colors = ButtonDefaults.buttonColors(containerColor = GitLabOrange)
      ) {
        Text("Create Issue")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
