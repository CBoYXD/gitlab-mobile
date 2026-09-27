package com.aistudio.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistudio.gitlab.data.api.formatGitLabTime
import com.aistudio.gitlab.data.model.GitLabProject
import com.aistudio.gitlab.data.model.GitLabUser
import com.aistudio.gitlab.ui.components.Avatar
import com.aistudio.gitlab.ui.components.EmptyState
import com.aistudio.gitlab.ui.components.ErrorState
import com.aistudio.gitlab.ui.components.EventRow
import com.aistudio.gitlab.ui.components.FullScreenState
import com.aistudio.gitlab.ui.components.GlCard
import com.aistudio.gitlab.ui.components.GlDivider
import com.aistudio.gitlab.ui.components.GlIconAction
import com.aistudio.gitlab.ui.components.GlRow
import com.aistudio.gitlab.ui.components.GlSectionHeader
import com.aistudio.gitlab.ui.components.GlTopBar
import com.aistudio.gitlab.ui.components.InlineError
import com.aistudio.gitlab.ui.components.LoadingState
import com.aistudio.gitlab.ui.components.MarkdownContext
import com.aistudio.gitlab.ui.components.MarkdownText
import com.aistudio.gitlab.ui.components.StatBlock
import com.aistudio.gitlab.ui.components.destination
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlSpace
import com.aistudio.gitlab.ui.viewmodel.ScreenDestination
import com.aistudio.gitlab.ui.viewmodel.UserDetailState

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

  Column(modifier = modifier.fillMaxSize().background(Gl.colors.bg)) {
    GlTopBar(title = title, onBack = onBack) {
      val url = user?.webUrl?.takeIf { it.isNotBlank() }
      if (url != null) GlIconAction(Icons.AutoMirrored.Outlined.OpenInNew, "Open in browser", onClick = { onOpenInBrowser(url) })
    }
    GlDivider()
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = GlSpace.group),
      ) {
        item(key = "header") { ProfileHeader(user, markdown, onOpenInBrowser) }

        item(key = "projects-header") { GlSectionHeader("Projects") }
        val projects = state.projects
        when {
          projects.initialLoading -> item(key = "projects-loading") { LoadingState(label = "Loading projects") }
          projects.error != null && projects.data.isEmpty() -> item(key = "projects-error") {
            InlineError(projects.error, Modifier.padding(horizontal = GlSpace.margin), onRetry)
          }
          projects.data.isEmpty() -> item(key = "projects-empty") {
            SectionNote("No public projects.")
          }
          else -> item(key = "projects") {
            GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin)) {
              projects.data.forEachIndexed { index, project ->
                if (index > 0) GlDivider(inset = 64.dp)
                UserProjectRow(project) { onOpenProject(project.id) }
              }
            }
          }
        }

        item(key = "activity-header") { GlSectionHeader("Activity") }
        val events = state.events
        when {
          events.initialLoading -> item(key = "activity-loading") { LoadingState(label = "Loading activity") }
          events.error != null && events.data.isEmpty() -> item(key = "activity-error") {
            InlineError(events.error, Modifier.padding(horizontal = GlSpace.margin), onRetry)
          }
          events.data.isEmpty() -> item(key = "activity-empty") {
            SectionNote("No recent public activity.")
          }
          else -> item(key = "activity") {
            GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin)) {
              events.data.forEachIndexed { index, event ->
                if (index > 0) GlDivider(inset = 60.dp)
                val target = event.destination()
                EventRow(event = event, onClick = target?.let { { onNavigate(it) } })
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ProfileHeader(user: GitLabUser, markdown: MarkdownContext, onOpenInBrowser: (String) -> Unit) {
  val c = Gl.colors
  Column(modifier = Modifier.fillMaxWidth().padding(GlSpace.margin)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Avatar(user.avatarUrl, user.name.ifBlank { user.username }, 72.dp)
      Spacer(Modifier.width(GlSpace.margin))
      Column(modifier = Modifier.weight(1f)) {
        Text(user.name.ifBlank { user.username }, style = Gl.type.title, color = c.textPrimary)
        val handle = listOfNotNull(
          user.username.takeIf { it.isNotBlank() }?.let { "@$it" },
          user.pronouns?.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
        if (handle.isNotBlank()) Text(handle, style = Gl.type.bodySmall, color = c.textSecondary)
      }
    }
    if (!user.bio.isNullOrBlank()) {
      Spacer(Modifier.height(12.dp))
      MarkdownText(user.bio, context = markdown, textStyle = Gl.type.bodySmall)
    }
    if (user.followers != null || user.following != null) {
      Spacer(Modifier.height(12.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(GlSpace.group)) {
        user.followers?.let { StatBlock(it.toString(), if (it == 1) "follower" else "followers") }
        user.following?.let { StatBlock(it.toString(), "following") }
      }
    }
  }
  val work = listOfNotNull(user.jobTitle?.takeIf { it.isNotBlank() }, user.organization?.takeIf { it.isNotBlank() }).joinToString(" at ")
    .ifBlank { user.workInformation.orEmpty() }
  val rows = buildList<Triple<ImageVector, String, String?>> {
    user.location?.takeIf { it.isNotBlank() }?.let { add(Triple(Icons.Outlined.LocationOn, it, null)) }
    work.takeIf { it.isNotBlank() }?.let { add(Triple(Icons.Outlined.Work, it, null)) }
    user.websiteUrl?.takeIf { it.isNotBlank() }?.let { add(Triple(Icons.Outlined.Link, it.removePrefix("https://").removePrefix("http://"), it.withScheme())) }
    user.publicEmail?.takeIf { it.isNotBlank() }?.let { add(Triple(Icons.Outlined.Mail, it, "mailto:$it")) }
    user.linkedin?.takeIf { it.isNotBlank() }?.let { add(Triple(Icons.Outlined.Link, "LinkedIn: $it", "https://www.linkedin.com/in/$it")) }
    user.twitter?.takeIf { it.isNotBlank() }?.let { add(Triple(Icons.Outlined.Link, "X: @$it", "https://x.com/$it")) }
    user.discord?.takeIf { it.isNotBlank() }?.let { add(Triple(Icons.Outlined.Chat, "Discord: $it", null)) }
    user.createdAt?.takeIf { it.isNotBlank() }?.let { add(Triple(Icons.Outlined.CalendarMonth, "Joined ${formatGitLabTime(it)}", null)) }
  }
  if (rows.isNotEmpty()) {
    GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin)) {
      rows.forEachIndexed { index, (icon, text, link) ->
        if (index > 0) GlDivider(inset = 52.dp)
        GlRow(
          title = text,
          leading = { Icon(icon, contentDescription = null, tint = c.textSecondary) },
          onClick = link?.let { { onOpenInBrowser(it) } },
          onClickLabel = link?.let { "Open link" },
        )
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

private fun String.withScheme(): String = if (startsWith("http://") || startsWith("https://")) this else "https://$this"
