package com.cboyxd.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.data.model.GitLabProject
import com.cboyxd.gitlab.ui.components.EmptyState
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.EventRow
import com.cboyxd.gitlab.ui.components.FullScreenState
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlIconAction
import com.cboyxd.gitlab.ui.components.GlRow
import com.cboyxd.gitlab.ui.components.GlSectionBand
import com.cboyxd.gitlab.ui.components.GlSectionHeader
import com.cboyxd.gitlab.ui.components.GlTopBar
import com.cboyxd.gitlab.ui.components.InlineError
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.components.MarkdownContext
import com.cboyxd.gitlab.ui.components.destination
import com.cboyxd.gitlab.ui.components.shareText
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.ScreenDestination
import com.cboyxd.gitlab.ui.viewmodel.UserDetailState

@Composable
fun UserDetailScreen(
  state: UserDetailState,
  onBack: () -> Unit,
  onRetry: () -> Unit,
  onOpenProject: (Long) -> Unit,
  onNavigate: (ScreenDestination) -> Unit,
  onOpenInBrowser: (String) -> Unit,
  markdown: MarkdownContext,
  modifier: Modifier = Modifier,
) {
  val user = state.user.data
  val title = user?.name?.takeIf { it.isNotBlank() }
    ?: user?.username?.takeIf { it.isNotBlank() }?.let { "@$it" }
    ?: state.username.takeIf { it.isNotBlank() }?.let { "@$it" }
    ?: "Profile"

  val context = LocalContext.current
  val listState = rememberLazyListState()
  val headerGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

  Column(modifier = modifier.fillMaxSize().background(Gl.colors.bg)) {
    GlTopBar(title = title, onBack = onBack, showTitle = user == null || headerGone) {
      val url = user?.webUrl?.takeIf { it.isNotBlank() }
      if (url != null) {
        GlIconAction(Icons.Outlined.Share, "Share profile", onClick = { shareText(context, url) })
        GlIconAction(Icons.AutoMirrored.Outlined.OpenInNew, "Open in browser", onClick = { onOpenInBrowser(url) })
      }
    }
    when {
      user == null && state.user.error != null -> FullScreenState {
        if (state.user.error.startsWith("No user named")) {
          EmptyState(
            icon = Icons.Outlined.PersonOff,
            title = "Person not found",
            body = state.user.error,
          )
        } else {
          ErrorState(state.user.error, onRetry)
        }
      }
      user == null -> FullScreenState { LoadingState(label = "Loading profile") }
      else -> LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = GlSpace.group),
      ) {
        item(key = "header") {
          ProfileSummary(user = user, onOpenLink = onOpenInBrowser, bioContext = markdown, modifier = Modifier.padding(bottom = 12.dp))
        }

        item(key = "projects-band") { GlSectionBand() }
        item(key = "projects-header") { GlSectionHeader("Projects", modifier = Modifier.padding(top = 0.dp)) }
        val projects = state.projects
        when {
          projects.initialLoading -> item(key = "projects-loading") { LoadingState(label = "Loading projects") }
          projects.error != null && projects.data.isEmpty() -> item(key = "projects-error") {
            InlineError(projects.error, Modifier.padding(horizontal = GlSpace.margin), onRetry)
          }
          projects.data.isEmpty() -> item(key = "projects-empty") {
            SectionNote("No public projects.")
          }
          else -> itemsIndexed(projects.data, key = { _, project -> "project-${project.id}" }) { index, project ->
            if (index > 0) GlDivider(inset = 64.dp)
            UserProjectRow(project) { onOpenProject(project.id) }
          }
        }

        item(key = "activity-band") { GlSectionBand(Modifier.padding(top = 12.dp)) }
        item(key = "activity-header") { GlSectionHeader("Activity", modifier = Modifier.padding(top = 0.dp)) }
        val events = state.events
        when {
          events.initialLoading -> item(key = "activity-loading") { LoadingState(label = "Loading activity") }
          events.error != null && events.data.isEmpty() -> item(key = "activity-error") {
            InlineError(events.error, Modifier.padding(horizontal = GlSpace.margin), onRetry)
          }
          events.data.isEmpty() -> item(key = "activity-empty") {
            SectionNote("No recent public activity.")
          }
          else -> itemsIndexed(events.data, key = { index, event -> "event-$index-${event.createdAt}" }) { index, event ->
            if (index > 0) GlDivider(inset = 60.dp)
            val target = event.destination()
            EventRow(event = event, onClick = target?.let { { onNavigate(it) } })
          }
        }
      }
    }
  }
}

@Composable
private fun UserProjectRow(project: GitLabProject, onClick: () -> Unit) {
  GlRow(
    title = project.name,
    subtitle = listOfNotNull(
      project.description?.takeIf { it.isNotBlank() }?.lineSequence()?.firstOrNull(),
      if (project.starCount == 1) "1 star" else "${project.starCount} stars",
    ).joinToString(" · "),
    leading = { ProjectTile(project) },
    onClick = onClick,
    onClickLabel = "Open project",
  )
}

@Composable
private fun SectionNote(text: String) {
  Text(
    text = text,
    style = Gl.type.bodySmall,
    color = Gl.colors.textSecondary,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
    modifier = Modifier.padding(horizontal = GlSpace.margin),
  )
}

