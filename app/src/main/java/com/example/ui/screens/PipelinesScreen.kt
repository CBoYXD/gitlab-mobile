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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GitLabPipeline
import com.example.data.model.GitLabStage
import com.example.ui.components.PipelineStatusBadge
import com.example.ui.theme.GitLabOrange
import com.example.ui.theme.GitLabPurple
import com.example.ui.theme.GitLabSuccess

@Composable
fun PipelinesScreen(
  pipelines: List<GitLabPipeline>,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier
) {
  var filterStatus by remember { mutableStateOf("All") }

  val filteredPipelines = pipelines.filter { pipeline ->
    when (filterStatus) {
      "Passed" -> pipeline.status == "success"
      "Running" -> pipeline.status == "running"
      "Failed" -> pipeline.status == "failed"
      else -> true
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(bottom = 80.dp)
  ) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "CI / CD Pipelines",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "GitLab Auto DevOps & Runner execution",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      IconButton(onClick = onRefresh) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Refresh",
          tint = GitLabOrange
        )
      }
    }

    // Filter Chips
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      val statuses = listOf("All", "Passed", "Running", "Failed")
      items(statuses) { status ->
        FilterChip(
          selected = (filterStatus == status),
          onClick = { filterStatus = status },
          label = { Text(status, fontSize = 12.sp) },
          shape = RoundedCornerShape(16.dp)
        )
      }
    }

    // Pipelines List
    if (filteredPipelines.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "No pipelines matching filter.",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 14.sp
        )
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(filteredPipelines, key = { it.id }) { pipeline ->
          PipelineCardItem(pipeline = pipeline)
        }
      }
    }
  }
}

@Composable
fun PipelineCardItem(pipeline: GitLabPipeline) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("pipeline_card_${pipeline.id}"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Header: Pipeline ID + Status Badge + Duration
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Pipeline #${pipeline.id}",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.width(8.dp))
          PipelineStatusBadge(status = pipeline.status)
        }

        if (pipeline.duration != null && pipeline.duration > 0) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.Timer,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            val mins = pipeline.duration / 60
            val secs = pipeline.duration % 60
            Text(
              text = "${mins}m ${secs}s",
              fontSize = 11.5.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Branch + Commit info
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(4.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Outlined.AltRoute,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = pipeline.ref,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.Commit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = pipeline.sha.take(8),
            fontSize = 11.5.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (!pipeline.coverage.isNullOrBlank()) {
          Surface(
            color = GitLabSuccess.copy(alpha = 0.12f),
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = "Coverage ${pipeline.coverage}",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = GitLabSuccess,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
        }
      }

      // Stages Progress Flow (Visual pipeline DAG)
      if (pipeline.stages.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Stages",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          items(pipeline.stages) { stage ->
            StagePill(stage = stage)
            if (stage != pipeline.stages.last()) {
              Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(12.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Triggered by @${pipeline.user.username} • ${pipeline.createdAt}",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
          onClick = { /* retry action */ },
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
          modifier = Modifier.height(28.dp),
          shape = RoundedCornerShape(6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = GitLabOrange
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Retry", fontSize = 11.sp, color = GitLabOrange)
        }
      }
    }
  }
}

@Composable
fun StagePill(stage: GitLabStage) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(6.dp),
    border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      PipelineStatusBadge(status = stage.status, showLabel = false)
      Text(
        text = stage.name,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}
