package com.cboyxd.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.data.model.GitLabInstance
import com.cboyxd.gitlab.data.model.GitLabUser
import com.cboyxd.gitlab.data.model.GitLabUserStatus
import com.cboyxd.gitlab.ui.components.EmptyState
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.GitLabTanukiIcon
import com.cboyxd.gitlab.ui.components.GlButton
import com.cboyxd.gitlab.ui.components.GlButtonKind
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlIconAction
import com.cboyxd.gitlab.ui.components.GlRow
import com.cboyxd.gitlab.ui.components.GlSectionBand
import com.cboyxd.gitlab.ui.components.GlTopBar
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.components.shareText
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.Loadable

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

  val context = LocalContext.current
  Column(modifier = modifier.fillMaxSize().background(Gl.colors.bg)) {
    GlTopBar(title = "Profile", onBack = onBack, showTitle = false) {
      val webUrl = profile?.webUrl
      if (!webUrl.isNullOrBlank()) {
        GlIconAction(icon = Icons.Outlined.Share, contentDescription = "Share profile", onClick = { shareText(context, webUrl) })
      }
      GlIconAction(
        icon = Icons.Outlined.Settings,
        contentDescription = "Accounts",
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
        actionLabel = "Sign in",
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
  val readme = profileReadme?.takeIf { it.isNotBlank() }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = GlSpace.group),
  ) {
    item(key = "header") {
      ProfileSummary(
        user = user,
        onOpenLink = onOpenInBrowser,
        statusMessage = status?.message,
        onEditStatus = onEditStatus,
      )
    }

    item(key = "server") {
      GlDivider(Modifier.padding(top = 12.dp))
      GlRow(
        title = activeInstance?.name ?: "No server",
        subtitle = listOfNotNull(
          activeInstance?.url?.removePrefix("https://"),
          activeInstance?.version?.takeIf { it.isNotBlank() }?.let { "GitLab $it" },
        ).joinToString(" · "),
        leading = { GitLabTanukiIcon(size = 28.dp) },
        trailing = {
          TextButton(onClick = onManageServers) {
            Text("MANAGE", style = Gl.type.label, color = Gl.colors.accent)
          }
        },
      )
    }

    if (readme != null) {
      item(key = "band") { GlSectionBand(height = 24.dp) }
      item(key = "readme") {
        var expanded by rememberSaveable { mutableStateOf(false) }
        val long = readme.length > README_PREVIEW_CHARS
        ProfileReadme(
          username = user.username,
          markdown = if (long && !expanded) readme.take(README_PREVIEW_CHARS).trimEnd() + "…" else readme,
        ) {
          if (long) {
            GlButton(
              text = if (expanded) "Show less" else "Show more",
              onClick = { expanded = !expanded },
              kind = GlButtonKind.Ghost,
              modifier = Modifier.padding(horizontal = 4.dp),
            )
          }
        }
      }
    }
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
          focusedBorderColor = Gl.colors.accent,
          unfocusedBorderColor = Gl.colors.borderStrong,
          focusedLabelColor = Gl.colors.accent,
          unfocusedLabelColor = Gl.colors.textSecondary,
          cursorColor = Gl.colors.accent,
        ),
        modifier = Modifier.fillMaxWidth(),
      )
    },
    confirmButton = { GlButton(text = "Save status", onClick = { onSave(draft) }) },
    dismissButton = { GlButton(text = "Cancel", onClick = onDismiss, kind = GlButtonKind.Ghost) },
  )
}
