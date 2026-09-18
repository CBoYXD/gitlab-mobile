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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.GitLabCommit
import com.example.data.model.GitLabIssue
import com.example.data.model.GitLabMergeRequest
import com.example.data.model.GitLabPipeline
import com.example.data.model.GitLabProject
import com.example.data.model.GitLabTreeItem
import com.example.ui.components.CodeViewer
import com.example.ui.components.PipelineStatusBadge
import com.example.ui.theme.GitLabLightOrange
import com.example.ui.theme.GitLabOrange
import com.example.ui.theme.GitLabPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
  project: GitLabProject?,
  tree: List<GitLabTreeItem>,
  currentPath: String,
  issues: List<GitLabIssue>,
  mergeRequests: List<GitLabMergeRequest>,
  pipelines: List<GitLabPipeline>,
  commits: List<GitLabCommit>,
  onBack: () -> Unit,
  onToggleStar: (Long) -> Unit,
  onNavigateTree: (path: String) -> Unit,
  onOpenFile: (filePath: String) -> Unit,
  onSelectIssue: (GitLabIssue) -> Unit,
  onSelectMr: (GitLabMergeRequest) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Overview", "Files", "Issues", "Merge Requests", "CI / CD", "Commits")

  if (project == null) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(color = GitLabOrange)
    }
    return
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = project.name,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = project.pathWithNamespace,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("project_back_button")) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { onToggleStar(project.id) }) {
            Icon(
              imageVector = if (project.isStarred) Icons.Filled.Star else Icons.Outlined.StarOutline,
              contentDescription = "Star",
              tint = if (project.isStarred) GitLabLightOrange else MaterialTheme.colorScheme.onSurface
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // Tab Row
      ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = GitLabOrange,
        edgePadding = 16.dp
      ) {
        tabs.forEachIndexed { index, title ->
          Tab(
            selected = (selectedTab == index),
            onClick = { selectedTab = index },
            text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
            modifier = Modifier.testTag("project_tab_$index")
          )
        }
      }

      when (selectedTab) {
        0 -> ProjectOverviewTab(project = project, commits = commits)
        1 -> ProjectFilesTab(
          tree = tree,
          currentPath = currentPath,
          onNavigateTree = onNavigateTree,
          onOpenFile = onOpenFile
        )
        2 -> ProjectIssuesTab(issues = issues.filter { it.projectId == project.id || project.id == 101L }, onSelectIssue = onSelectIssue)
        3 -> ProjectMergeRequestsTab(mrs = mergeRequests.filter { it.projectId == project.id || project.id == 101L }, onSelectMr = onSelectMr)
        4 -> ProjectPipelinesTab(pipelines = pipelines)
        5 -> ProjectCommitsTab(commits = commits)
      }
    }
  }
}

@Composable
fun ProjectOverviewTab(project: GitLabProject, commits: List<GitLabCommit>) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      // Metadata Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = project.description ?: "GitLab Repository",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = GitLabLightOrange, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("${project.starCount} stars", fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Outlined.CallSplit, contentDescription = null, tint = GitLabPurple, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("${project.forksCount} forks", fontSize = 12.sp)
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
              Text(project.defaultBranch, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
          }
        }
      }
    }

    item {
      // Quick Git Clone card
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text("Repository Clone URL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "git clone ${project.webUrl}.git",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.5.sp,
            color = GitLabOrange
          )
        }
      }
    }

    item {
      Text("README.md Preview", fontWeight = FontWeight.Bold, fontSize = 15.sp)
      Spacer(modifier = Modifier.height(6.dp))
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "# ${project.name}\n\n${project.description ?: ""}\n\n### Quick Start\nExecute your GitLab CI runner or deploy to Kubernetes directly from GitLab Mobile.\n\n- Real GitLab API v4\n- Auto DevOps & Security SAST scans\n- Native Git diff inspection",
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

@Composable
fun ProjectFilesTab(
  tree: List<GitLabTreeItem>,
  currentPath: String,
  onNavigateTree: (String) -> Unit,
  onOpenFile: (String) -> Unit
) {
  Column(modifier = Modifier.fillMaxSize()) {
    // Breadcrumbs
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = if (currentPath.isEmpty()) "root /" else "root / $currentPath",
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      if (currentPath.isNotEmpty()) {
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
          onClick = {
            val parent = currentPath.substringBeforeLast("/", "")
            onNavigateTree(parent)
          },
          modifier = Modifier.size(24.dp)
        ) {
          Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Up directory", modifier = Modifier.size(16.dp))
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      items(tree, key = { it.id }) { item ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
              if (item.type == "tree") {
                onNavigateTree(item.path)
              } else {
                onOpenFile(item.path)
              }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (item.type == "tree") Icons.Filled.Folder else Icons.Filled.Description,
            contentDescription = null,
            tint = if (item.type == "tree") GitLabOrange else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = item.name,
            fontSize = 13.5.sp,
            fontFamily = if (item.type == "blob") FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
          )
          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

@Composable
fun ProjectIssuesTab(issues: List<GitLabIssue>, onSelectIssue: (GitLabIssue) -> Unit) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    items(issues) { issue ->
      IssueItemCard(issue = issue, onClick = { onSelectIssue(issue) })
    }
  }
}

@Composable
fun ProjectMergeRequestsTab(mrs: List<GitLabMergeRequest>, onSelectMr: (GitLabMergeRequest) -> Unit) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    items(mrs) { mr ->
      MergeRequestItemCard(mr = mr, onClick = { onSelectMr(mr) })
    }
  }
}

@Composable
fun ProjectPipelinesTab(pipelines: List<GitLabPipeline>) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    items(pipelines) { pipeline ->
      PipelineCardItem(pipeline = pipeline)
    }
  }
}

@Composable
fun ProjectCommitsTab(commits: List<GitLabCommit>) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    items(commits) { commit ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = commit.title,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "${commit.authorName} • ${commit.createdAt}",
              fontSize = 11.5.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = commit.shortId,
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                color = GitLabOrange,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
      }
    }
  }
}
