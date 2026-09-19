package com.example.ui.screens

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GitLabInstance
import com.example.ui.components.GitLabTanukiIcon
import com.example.ui.theme.GitLabOrange
import com.example.ui.theme.GitLabPurple
import com.example.ui.theme.GitLabSuccess

@Composable
fun UserProfileScreen(
  activeInstance: GitLabInstance,
  onBack: () -> Unit,
  onOpenInstanceSwitcher: () -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(bottom = 40.dp)
  ) {
    // Top Bar with Back, Share, Settings
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
          IconButton(onClick = { /* Share profile */ }) {
            Icon(
              imageVector = Icons.Outlined.Share,
              contentDescription = "Share",
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

    // Avatar and Header Info
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Avatar circle (matches screenshot style)
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color(0xFF2E7D32)),
          contentAlignment = Alignment.Center
        ) {
          GitLabTanukiIcon(size = 46.dp)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
          Text(
            text = "Bohdan",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "CBoYXD • he/him",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // "Set your status" Pill
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
            .clickable { /* Edit status */ }
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.EmojiEmotions,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Set your status",
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

    // Bio & Details Section
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Ukraine, Kharkiv",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.Link,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "https://t.me/CBoYXD",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.People,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "16 followers • 14 following",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Trophies / Badges row (exact match from Screenshot 1)
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
            BadgeEmoji(background = Color(0xFF2EA44F), text = "🌱")
            BadgeEmoji(background = Color(0xFF1F6FEB), text = "🦈")
            BadgeEmoji(background = Color(0xFF8957E5), text = "🎨")
            BadgeEmoji(background = Color(0xFFD29922), text = "🤠")
          }
        }
      }
    }

    // Active Server / Instance connection card
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
              Text(
                text = "Active Server",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = GitLabSuccess.copy(alpha = 0.15f)
            ) {
              Text(
                text = "Connected",
                color = GitLabSuccess,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = activeInstance.name,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${activeInstance.url} • GitLab ${activeInstance.version}",
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

    // Pinned README.md (Exact replicate of Screenshot 1)
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "CBoYXD / README.md",
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
        Column(modifier = Modifier.padding(16.dp)) {
          val rustCode = buildAnnotatedString {
            withStyle(SpanStyle(color = Color(0xFFFF7B72), fontWeight = FontWeight.Bold)) {
              append("struct ")
            }
            withStyle(SpanStyle(color = Color(0xFFFFA657))) {
              append("Person")
            }
            withStyle(SpanStyle(color = Color(0xFFC9D1D9))) {
              append("<")
            }
            withStyle(SpanStyle(color = Color(0xFF79C0FF))) {
              append("'a")
            }
            withStyle(SpanStyle(color = Color(0xFFC9D1D9))) {
              append("> {\n")
            }

            withStyle(SpanStyle(color = Color(0xFF79C0FF))) {
              append("    name: ")
            }
            withStyle(SpanStyle(color = Color(0xFFFF7B72))) {
              append("&'a ")
            }
            withStyle(SpanStyle(color = Color(0xFF79C0FF))) {
              append("str,\n")
            }

            withStyle(SpanStyle(color = Color(0xFF79C0FF))) {
              append("    role: ")
            }
            withStyle(SpanStyle(color = Color(0xFFFF7B72))) {
              append("&'a ")
            }
            withStyle(SpanStyle(color = Color(0xFF79C0FF))) {
              append("str,\n")
            }

            withStyle(SpanStyle(color = Color(0xFF79C0FF))) {
              append("    location: ")
            }
            withStyle(SpanStyle(color = Color(0xFFFF7B72))) {
              append("&'a ")
            }
            withStyle(SpanStyle(color = Color(0xFF79C0FF))) {
              append("str,\n")
            }

            withStyle(SpanStyle(color = Color(0xFFC9D1D9))) {
              append("}\n\n")
            }

            withStyle(SpanStyle(color = Color(0xFFFF7B72), fontWeight = FontWeight.Bold)) {
              append("let ")
            }
            withStyle(SpanStyle(color = Color(0xFFC9D1D9))) {
              append("me = ")
            }
            withStyle(SpanStyle(color = Color(0xFFFFA657))) {
              append("Person ")
            }
            withStyle(SpanStyle(color = Color(0xFFC9D1D9))) {
              append("{\n")
            }

            withStyle(SpanStyle(color = Color(0xFF79C0FF))) {
              append("    name: ")
            }
            withStyle(SpanStyle(color = Color(0xFFA5D6FF))) {
              append("\"CBoYXD\",\n")
            }

            withStyle(SpanStyle(color = Color(0xFF79C0FF))) {
              append("    role: ")
            }
            withStyle(SpanStyle(color = Color(0xFFA5D6FF))) {
              append("\"Python Backend Developer\",\n")
            }

            withStyle(SpanStyle(color = Color(0xFF79C0FF))) {
              append("    location: ")
            }
            withStyle(SpanStyle(color = Color(0xFFA5D6FF))) {
              append("\"Kharkiv, Ukraine\",\n")
            }

            withStyle(SpanStyle(color = Color(0xFFC9D1D9))) {
              append("};")
            }
          }

          Text(
            text = rustCode,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp
          )
        }
      }
    }
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
