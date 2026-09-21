package com.aistudio.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import com.aistudio.gitlab.data.model.GitLabInstance
import com.aistudio.gitlab.data.model.GitLabUser
import com.aistudio.gitlab.data.model.GitLabUserAchievement
import com.aistudio.gitlab.data.model.GitLabUserStatus
import com.aistudio.gitlab.ui.components.GitLabTanukiIcon
import com.aistudio.gitlab.ui.theme.GitLabDanger
import com.aistudio.gitlab.ui.theme.GitLabOrange
import com.aistudio.gitlab.ui.theme.GitLabSuccess

@Composable
fun UserProfileScreen(
  activeInstance: GitLabInstance?,
  user: GitLabUser?,
  status: GitLabUserStatus?,
  achievements: List<GitLabUserAchievement>,
  profileReadme: String?,
  onBack: () -> Unit,
  onOpenInstanceSwitcher: () -> Unit,
  onSetStatus: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var showStatusDialog by remember { mutableStateOf(false) }
  var draftStatus by remember(status?.message) { mutableStateOf(status?.message.orEmpty()) }
  val displayName = user?.name?.takeIf { it.isNotBlank() }
    ?: user?.username?.takeIf { it.isNotBlank() }
    ?: "Not signed in"
  val subtitle = listOfNotNull(
    user?.username?.takeIf { it.isNotBlank() },
    user?.pronouns?.takeIf { it.isNotBlank() }
  ).joinToString(" • ").ifBlank { activeInstance?.url.orEmpty() }
  val statusText = status?.message?.takeIf { it.isNotBlank() } ?: "Set your status"
  val link = user?.websiteUrl?.takeIf { it.isNotBlank() } ?: user?.webUrl?.takeIf { it.isNotBlank() }
  val about = profileReadme?.takeIf { it.isNotBlank() } ?: user?.bio?.takeIf { it.isNotBlank() }
  val online = activeInstance?.statusOk == true

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(bottom = 40.dp)
  ) {
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("profile_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onBackground
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = {
            val url = user?.webUrl ?: activeInstance?.url
            if (!url.isNullOrBlank()) {
              context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
          }) {
            Icon(
              imageVector = Icons.Outlined.Share,
              contentDescription = "Open profile",
              tint = MaterialTheme.colorScheme.onBackground
            )
          }
          IconButton(
            onClick = onOpenInstanceSwitcher,
            modifier = Modifier.testTag("profile_settings_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.Settings,
              contentDescription = "Settings / Instances",
              tint = MaterialTheme.colorScheme.onBackground
            )
          }
        }
      }
    }

    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (!user?.avatarUrl.isNullOrBlank()) {
          AsyncImage(
            model = user?.avatarUrl,
            contentDescription = displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .size(76.dp)
              .clip(CircleShape)
          )
        } else {
          Box(
            modifier = Modifier
              .size(76.dp)
              .clip(CircleShape)
              .background(Color(0xFF2E7D32)),
            contentAlignment = Alignment.Center
          ) {
            GitLabTanukiIcon(size = 46.dp)
          }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
          Text(
            text = displayName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = subtitle,
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    item {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showStatusDialog = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = Icons.Outlined.EmojiEmotions,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = statusText,
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }

    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        val location = listOfNotNull(
          user?.location?.takeIf { it.isNotBlank() },
          user?.organization?.takeIf { it.isNotBlank() },
          user?.jobTitle?.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
        if (location.isNotBlank()) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = location, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
          }
        }

        if (!link.isNullOrBlank()) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.Link,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = link,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        if (user?.followers != null || user?.following != null) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.People,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "${user?.followers ?: 0} followers • ${user?.following ?: 0} following",
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        if (achievements.isNotEmpty()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.WorkspacePremium,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              achievements.take(6).forEachIndexed { index, achievement ->
                BadgeEmoji(
                  background = ACHIEVEMENT_COLORS[index % ACHIEVEMENT_COLORS.size],
                  text = achievement.title.take(1)
                )
              }
            }
          }
        }
      }
    }

    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Outlined.Dns,
                contentDescription = null,
                tint = GitLabOrange,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = "Active Server", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = (if (online) GitLabSuccess else GitLabDanger).copy(alpha = 0.15f)
            ) {
              Text(
                text = if (online) "Connected" else "Offline",
                color = if (online) GitLabSuccess else GitLabDanger,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = activeInstance?.name ?: "No server connected",
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = buildString {
              append(activeInstance?.url ?: "Add a GitLab server")
              val version = activeInstance?.version?.takeIf { it.isNotBlank() }
              if (version != null) append(" • GitLab $version")
            },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clickable(onClick = onOpenInstanceSwitcher),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              text = "SWITCH OR ADD GITLAB INSTANCE",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = GitLabOrange,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              modifier = Modifier.padding(vertical = 8.dp)
            )
          }
        }
      }
    }

    if (!about.isNullOrBlank()) {
      item {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = if (profileReadme.isNullOrBlank()) "About" else "${user?.username ?: "profile"} / README.md",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
        ) {
          Text(
            text = about.take(6000),
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = Color(0xFFC9D1D9),
            modifier = Modifier.padding(16.dp)
          )
        }
      }
    }
  }

  if (showStatusDialog) {
    AlertDialog(
      onDismissRequest = { showStatusDialog = false },
      title = { Text("Set your status", fontWeight = FontWeight.Bold) },
      text = {
        OutlinedTextField(
          value = draftStatus,
          onValueChange = { draftStatus = it },
          label = { Text("Status") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onSetStatus(draftStatus.trim())
            showStatusDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = GitLabOrange)
        ) { Text("Save") }
      },
      dismissButton = {
        TextButton(onClick = { showStatusDialog = false }) { Text("Cancel") }
      }
    )
  }
}

@Composable
private fun BadgeEmoji(background: Color, text: String) {
  Box(
    modifier = Modifier
      .size(26.dp)
      .clip(CircleShape)
      .background(background.copy(alpha = 0.25f))
      .border(1.dp, background, CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Text(text = text, fontSize = 13.sp)
  }
}

private val ACHIEVEMENT_COLORS = listOf(
  Color(0xFF2EA44F),
  Color(0xFF1F6FEB),
  Color(0xFF8957E5),
  Color(0xFFD29922)
)
