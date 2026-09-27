package com.aistudio.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistudio.gitlab.data.model.GitLabInstance
import com.aistudio.gitlab.data.model.GitLabUser
import com.aistudio.gitlab.data.model.GitLabUserStatus
import com.aistudio.gitlab.ui.components.Avatar
import com.aistudio.gitlab.ui.components.EmptyState
import com.aistudio.gitlab.ui.components.ErrorState
import com.aistudio.gitlab.ui.components.GitLabTanukiIcon
import com.aistudio.gitlab.ui.components.GlButton
import com.aistudio.gitlab.ui.components.GlButtonKind
import com.aistudio.gitlab.ui.components.GlCard
import com.aistudio.gitlab.ui.components.GlIconAction
import com.aistudio.gitlab.ui.components.GlSectionHeader
import com.aistudio.gitlab.ui.components.GlTopBar
import com.aistudio.gitlab.ui.components.LoadingState
import com.aistudio.gitlab.ui.components.MarkdownText
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlSpace
import com.aistudio.gitlab.ui.viewmodel.Loadable

private const val README_PREVIEW_CHARS = 8_000

@Composable
fun UserProfileScreen(
  activeInstance: GitLabInstance?,
  user: Loadable<GitLabUser?>,
  status: GitLabUserStatus?,
  profileReadme: String?,
  onBack: () -> Unit,
  onOpenSwitcher: () -> Unit,
  onManageServers: () -> Unit,
  onSetStatus: (String) -> Unit,
  onOpenInBrowser: (String) -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showStatusDialog by rememberSaveable { mutableStateOf(false) }
  val profile = user.data

  Column(modifier = modifier.fillMaxSize().background(Gl.colors.bg)) {
    GlTopBar(title = "Profile", onBack = onBack) {
      val webUrl = profile?.webUrl
      if (!webUrl.isNullOrBlank()) {
        GlIconAction(icon = Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Open in browser", onClick = { onOpenInBrowser(webUrl) })
      }
      GlIconAction(
        icon = Icons.Outlined.SwapHoriz,
        contentDescription = "Switch server",
        onClick = onOpenSwitcher,
        modifier = Modifier.testTag("profile_settings_button"),
      )
    }

    when {
      user.initialLoading -> LoadingState(label = "Loading profile")
      profile == null && user.error != null -> ErrorState(message = user.error, onRetry = onRetry)
      profile == null -> EmptyState(
        icon = Icons.Outlined.PersonOutline,
        title = "You're not signed in",
        body = "Sign in to this server to see your profile.",
        actionLabel = "Manage servers",
        onAction = onManageServers,
      )
      else -> ProfileContent(
        user = profile,
        status = status,
        profileReadme = profileReadme,
        activeInstance = activeInstance,
        onEditStatus = { showStatusDialog = true },
        onManageServers = onManageServers,
        onOpenInBrowser = onOpenInBrowser,
      )
    }
  }

  if (showStatusDialog) {
    StatusDialog(
      initial = status?.message.orEmpty(),
      onDismiss = { showStatusDialog = false },
      onSave = {
        showStatusDialog = false
        onSetStatus(it.trim())
      },
    )
  }
}

@Composable
private fun ProfileContent(
  user: GitLabUser,
  status: GitLabUserStatus?,
  profileReadme: String?,
  activeInstance: GitLabInstance?,
  onEditStatus: () -> Unit,
  onManageServers: () -> Unit,
  onOpenInBrowser: (String) -> Unit,
) {
  val displayName = user.name.ifBlank { user.username }
  val about = profileReadme?.takeIf { it.isNotBlank() } ?: user.bio?.takeIf { it.isNotBlank() }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = GlSpace.group),
  ) {
    item(key = "header") {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = GlSpace.margin, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Avatar(url = user.avatarUrl, name = displayName, size = 72.dp)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(displayName, style = Gl.type.title, color = Gl.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
          val handle = listOfNotNull(
            user.username.takeIf { it.isNotBlank() }?.let { "@$it" },
            user.pronouns?.takeIf { it.isNotBlank() },
          ).joinToString(" · ")
          if (handle.isNotBlank()) {
            Text(handle, style = Gl.type.bodySmall, color = Gl.colors.textSecondary)
          }
        }
      }
    }

    item(key = "status") {
      GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditStatus, role = Role.Button, onClickLabel = "Edit status")
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = GlSpace.margin, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Outlined.EmojiEmotions, contentDescription = null, tint = Gl.colors.textSecondary, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(12.dp))
          val message = status?.message?.takeIf { it.isNotBlank() }
          Text(
            text = message ?: "Set a status",
            style = Gl.type.bodySmall,
            color = if (message != null) Gl.colors.textPrimary else Gl.colors.textSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Icon(Icons.Outlined.Edit, contentDescription = null, tint = Gl.colors.textTertiary, modifier = Modifier.size(18.dp))
        }
      }
    }

    item(key = "details") {
      val work = listOfNotNull(
        user.jobTitle?.takeIf { it.isNotBlank() },
        user.organization?.takeIf { it.isNotBlank() },
      ).joinToString(" at ")
      val website = user.websiteUrl?.takeIf { it.isNotBlank() }
      val hasFollow = user.followers != null || user.following != null
      val location = user.location?.takeIf { it.isNotBlank() }
      if (work.isNotBlank() || website != null || hasFollow || location != null) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
          if (location != null) DetailRow(Icons.Outlined.LocationOn, location)
          if (work.isNotBlank()) DetailRow(if (user.jobTitle.isNullOrBlank()) Icons.Outlined.Business else Icons.Outlined.Work, work)
          if (website != null) {
            DetailRow(Icons.Outlined.Link, website.removePrefix("https://").removePrefix("http://"), onClick = { onOpenInBrowser(website) })
          }
          if (hasFollow) {
            DetailRow(Icons.Outlined.People, "${user.followers ?: 0} followers · ${user.following ?: 0} following", numeric = true)
          }
        }
      }
    }

    item(key = "server") {
      GlSectionHeader(title = "Server")
      GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin), contentPadding = PaddingValues(GlSpace.margin)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          GitLabTanukiIcon(size = 28.dp)
          Spacer(Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(activeInstance?.name ?: "No server", style = Gl.type.rowTitle, color = Gl.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
              text = listOfNotNull(
                activeInstance?.url?.removePrefix("https://"),
                activeInstance?.version?.takeIf { it.isNotBlank() }?.let { "GitLab $it" },
              ).joinToString(" · "),
              style = Gl.type.meta,
              color = Gl.colors.textSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Spacer(Modifier.height(12.dp))
        GlButton(text = "Manage servers", onClick = onManageServers, kind = GlButtonKind.Secondary, modifier = Modifier.fillMaxWidth())
      }
    }

    if (about != null) {
      item(key = "about") {
        var expanded by rememberSaveable { mutableStateOf(false) }
        val long = about.length > README_PREVIEW_CHARS
        GlSectionHeader(title = if (profileReadme.isNullOrBlank()) "About" else "README")
        GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin), contentPadding = PaddingValues(GlSpace.margin)) {
          MarkdownText(
            markdown = if (long && !expanded) about.take(README_PREVIEW_CHARS).trimEnd() + "…" else about,
          )
          if (long) {
            Spacer(Modifier.height(8.dp))
            GlButton(
              text = if (expanded) "Show less" else "Show more",
              onClick = { expanded = !expanded },
              kind = GlButtonKind.Ghost,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DetailRow(icon: ImageVector, text: String, onClick: (() -> Unit)? = null, numeric: Boolean = false) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open website") else Modifier)
      .defaultMinSize(minHeight = if (onClick != null) GlSpace.touch else 36.dp)
      .padding(horizontal = GlSpace.margin, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(icon, contentDescription = null, tint = Gl.colors.textSecondary, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(12.dp))
    Text(
      text = text,
      style = if (numeric) Gl.type.numeric else Gl.type.bodySmall,
      color = if (onClick != null) Gl.colors.textPrimary else Gl.colors.textSecondary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun StatusDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
  var draft by rememberSaveable { mutableStateOf(initial) }
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Gl.colors.surface,
    title = { Text("Set your status", style = Gl.type.sectionTitle, color = Gl.colors.textPrimary) },
    text = {
      OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        label = { Text("Status") },
        placeholder = { Text("Out sick") },
        supportingText = { Text("Leave empty to clear") },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = Gl.colors.textPrimary,
          unfocusedBorderColor = Gl.colors.borderStrong,
          focusedLabelColor = Gl.colors.textPrimary,
          unfocusedLabelColor = Gl.colors.textSecondary,
          cursorColor = Gl.colors.textPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
      )
    },
    confirmButton = { GlButton(text = "Save status", onClick = { onSave(draft) }) },
    dismissButton = { GlButton(text = "Cancel", onClick = onDismiss, kind = GlButtonKind.Ghost) },
  )
}
