package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GitLabFileDiff
import com.example.data.model.GitLabIssue
import com.example.data.model.GitLabMergeRequest
import com.example.ui.components.CodeViewer
import com.example.ui.components.DiffViewer
import com.example.ui.components.GitLabLabelChip
import com.example.ui.components.IssueStateIcon
import com.example.ui.components.MergeRequestStateIcon
import com.example.ui.theme.GitLabOrange
import com.example.ui.theme.GitLabPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
  filePath: String,
  content: String?,
  isLoading: Boolean,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val clipboard = LocalClipboardManager.current
  val context = LocalContext.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = filePath.substringAfterLast("/"),
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
            Text(
              text = filePath,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("file_viewer_back")) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          if (!content.isNullOrBlank()) {
            IconButton(onClick = {
              clipboard.setText(AnnotatedString(content))
              Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
            }) {
              Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy code")
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          color = GitLabOrange,
          modifier = Modifier.align(Alignment.Center)
        )
      } else if (content != null) {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(12.dp)
        ) {
          item {
            CodeViewer(code = content)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeRequestDetailScreen(
  mr: GitLabMergeRequest,
  diffs: List<GitLabFileDiff>,
  isLoadingDiffs: Boolean,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text("MR !${mr.iid}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("mr_detail_back")) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Header details card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              MergeRequestStateIcon(state = mr.state)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = mr.state.uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (mr.state == "merged") GitLabPurple else GitLabOrange
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = mr.title,
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp,
              lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = "${mr.sourceBranch} → ${mr.targetBranch}",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Authored by @${mr.author.username}",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!mr.description.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = mr.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
              )
            }
          }
        }
      }

      // Changes / Diff Section
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Changes (${diffs.size} files)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        }
      }

      if (isLoadingDiffs) {
        item {
          Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GitLabOrange)
          }
        }
      } else {
        items(diffs) { diff ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = diff.newPath.ifEmpty { diff.oldPath },
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
              )
              Spacer(modifier = Modifier.height(8.dp))
              DiffViewer(diffText = diff.diff)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(
  issue: GitLabIssue,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var newCommentText by remember { mutableStateOf("") }
  var commentsList by remember {
    mutableStateOf(
      listOf(
        Pair("Dmitry Ivanov (@divanov)", "Thanks for reporting. Investigating cache lifecycle in runner pods now."),
        Pair("Elena Rostova (@erostova)", "Verified on v16.11-ee runner with Kubernetes executor.")
      )
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text("Issue #${issue.iid}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("issue_detail_back")) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = newCommentText,
            onValueChange = { newCommentText = it },
            placeholder = { Text("Leave a comment...", fontSize = 13.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
            onClick = {
              if (newCommentText.isNotBlank()) {
                commentsList = commentsList + Pair("You (@you)", newCommentText)
                newCommentText = ""
              }
            },
            enabled = newCommentText.isNotBlank()
          ) {
            Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = GitLabOrange)
          }
        }
      }
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IssueStateIcon(state = issue.state)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = issue.state.uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (issue.state == "opened") GitLabOrange else GitLabPurple
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = issue.title,
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp,
              lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Opened ${issue.createdAt} by @${issue.author.username}",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (issue.labels.isNotEmpty()) {
              Spacer(modifier = Modifier.height(8.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                issue.labels.forEach { label ->
                  GitLabLabelChip(label = label)
                }
              }
            }

            if (!issue.description.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = issue.description,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 19.sp
              )
            }
          }
        }
      }

      item {
        Text("Discussion", fontWeight = FontWeight.Bold, fontSize = 15.sp)
      }

      items(commentsList) { (author, text) ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = author,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = GitLabOrange
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = text,
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurface,
              lineHeight = 18.sp
            )
          }
        }
      }
    }
  }
}
