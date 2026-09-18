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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.StarOutline
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
import com.example.data.model.GitLabProject
import com.example.ui.theme.GitLabLightOrange
import com.example.ui.theme.GitLabOrange
import com.example.ui.theme.GitLabPurple
import com.example.ui.viewmodel.ScreenDestination

@Composable
fun ProjectsScreen(
  projects: List<GitLabProject>,
  searchQuery: String,
  onSearchChange: (String) -> Unit,
  onSelectProject: (Long) -> Unit,
  onToggleStar: (Long) -> Unit,
  onAddProject: (name: String, description: String, visibility: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedFilter by remember { mutableStateOf("All") }
  var showNewProjectDialog by remember { mutableStateOf(false) }

  val filteredProjects = projects.filter { project ->
    val matchesFilter = when (selectedFilter) {
      "Starred" -> project.isStarred
      "Public" -> project.visibility.equals("public", ignoreCase = true)
      "Internal" -> project.visibility.equals("internal", ignoreCase = true)
      "Private" -> project.visibility.equals("private", ignoreCase = true)
      else -> true
    }
    matchesFilter
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showNewProjectDialog = true },
        containerColor = GitLabOrange,
        contentColor = Color.White,
        modifier = Modifier.testTag("new_project_fab")
      ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = "New Project")
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Projects",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "${filteredProjects.size} repos",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        placeholder = { Text("Search projects and namespaces...") },
        leadingIcon = {
          Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchChange("") }) {
              Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
            }
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .testTag("projects_search_input"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )

      // Filter Chips
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val filters = listOf("All", "Starred", "Public", "Internal", "Private")
        items(filters) { filter ->
          FilterChip(
            selected = (selectedFilter == filter),
            onClick = { selectedFilter = filter },
            label = { Text(filter, fontSize = 12.sp) },
            shape = RoundedCornerShape(16.dp)
          )
        }
      }

      // Project Cards List
      if (filteredProjects.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "No projects found",
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Try adjusting your search query or filters",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          }
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredProjects, key = { it.id }) { project ->
            ProjectCardItem(
              project = project,
              onClick = { onSelectProject(project.id) },
              onToggleStar = { onToggleStar(project.id) }
            )
          }
        }
      }
    }
  }

  if (showNewProjectDialog) {
    NewProjectDialog(
      onDismiss = { showNewProjectDialog = false },
      onCreate = { name, desc, vis ->
        onAddProject(name, desc, vis)
        showNewProjectDialog = false
      }
    )
  }
}

@Composable
fun ProjectCardItem(
  project: GitLabProject,
  onClick: () -> Unit,
  onToggleStar: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("project_item_${project.id}"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(GitLabPurple.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = project.name.take(1).uppercase(),
              color = GitLabPurple,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = project.nameWithNamespace.substringBeforeLast("/", "").trim(),
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = project.name,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        IconButton(
          onClick = onToggleStar,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = if (project.isStarred) Icons.Filled.Star else Icons.Outlined.StarOutline,
            contentDescription = "Star",
            tint = if (project.isStarred) GitLabLightOrange else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (!project.description.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = project.description,
          fontSize = 12.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 16.sp
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Star count
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = GitLabLightOrange,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(text = "${project.starCount}", fontSize = 11.5.sp)
        }

        // Forks count
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.CallSplit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(text = "${project.forksCount}", fontSize = 11.5.sp)
        }

        // Open issues
        if (project.openIssuesCount > 0) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.Adjust,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(text = "${project.openIssuesCount}", fontSize = 11.5.sp)
          }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Visibility Badge
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(4.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (project.visibility == "private") Icons.Default.Lock else Icons.Default.Public,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = project.visibility,
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

@Composable
fun NewProjectDialog(
  onDismiss: () -> Unit,
  onCreate: (name: String, description: String, visibility: String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var visibility by remember { mutableStateOf("public") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create New Project", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Project Name") },
          placeholder = { Text("my-microservice") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description (Optional)") },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 3
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = (visibility == "public"),
            onClick = { visibility = "public" },
            label = { Text("Public") }
          )
          FilterChip(
            selected = (visibility == "internal"),
            onClick = { visibility = "internal" },
            label = { Text("Internal") }
          )
          FilterChip(
            selected = (visibility == "private"),
            onClick = { visibility = "private" },
            label = { Text("Private") }
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onCreate(name, description, visibility) },
        enabled = name.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = GitLabOrange)
      ) {
        Text("Create Project")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
