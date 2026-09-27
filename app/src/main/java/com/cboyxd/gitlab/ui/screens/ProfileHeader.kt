package com.cboyxd.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.data.api.formatGitLabTime
import com.cboyxd.gitlab.data.model.GitLabUser
import com.cboyxd.gitlab.ui.components.Avatar
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.MarkdownContext
import com.cboyxd.gitlab.ui.components.MarkdownText
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace

/**
 * Top of a person's page in the GitHub Mobile manner: an 80dp avatar beside the bold name and
 * "username · pronouns", then the status field (own profile only), then detail rows with icons.
 */
@Composable
internal fun ProfileSummary(
  user: GitLabUser,
  onOpenLink: (String) -> Unit,
  modifier: Modifier = Modifier,
  statusMessage: String? = null,
  onEditStatus: (() -> Unit)? = null,
  bioContext: MarkdownContext? = null,
  showBio: Boolean = true,
) {
  val c = Gl.colors
  val displayName = user.name.ifBlank { user.username }
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = GlSpace.margin, end = GlSpace.margin, top = 8.dp, bottom = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Avatar(url = user.avatarUrl, name = displayName, size = 80.dp)
      Spacer(Modifier.width(20.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          displayName,
          style = Gl.type.headline,
          color = c.textPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.semantics { heading() },
        )
        val handle = listOfNotNull(
          user.username.takeIf { it.isNotBlank() },
          user.pronouns?.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
        if (handle.isNotBlank()) {
          Text(handle, style = Gl.type.body, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    }

    if (onEditStatus != null) {
      StatusField(message = statusMessage, onClick = onEditStatus)
      Spacer(Modifier.height(8.dp))
    }

    val bio = user.bio?.takeIf { it.isNotBlank() }
    if (showBio && bio != null) {
      MarkdownText(
        bio,
        context = bioContext ?: MarkdownContext(),
        textStyle = Gl.type.body,
        modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = 8.dp),
      )
    }

    profileDetails(user).forEach { detail ->
      DetailLine(detail.icon, detail.text, emphasized = detail.url != null, onClick = detail.url?.let { url -> { onOpenLink(url) } })
    }
    if (user.followers != null || user.following != null) {
      FollowLine(user.followers ?: 0, user.following ?: 0)
    }
  }
}

private data class ProfileDetail(val icon: ImageVector, val text: String, val url: String?)

private fun profileDetails(user: GitLabUser): List<ProfileDetail> {
  val work = listOfNotNull(user.jobTitle?.takeIf { it.isNotBlank() }, user.organization?.takeIf { it.isNotBlank() })
    .joinToString(" at ")
    .ifBlank { user.workInformation.orEmpty() }
  return buildList {
    user.location?.takeIf { it.isNotBlank() }?.let { add(ProfileDetail(Icons.Outlined.LocationOn, it, null)) }
    work.takeIf { it.isNotBlank() }?.let { add(ProfileDetail(Icons.Outlined.Work, it, null)) }
    user.websiteUrl?.takeIf { it.isNotBlank() }?.let { add(ProfileDetail(Icons.Outlined.Link, it.removePrefix("https://").removePrefix("http://").trimEnd('/'), it.withScheme())) }
    user.publicEmail?.takeIf { it.isNotBlank() }?.let { add(ProfileDetail(Icons.Outlined.Mail, it, "mailto:$it")) }
    user.linkedin?.takeIf { it.isNotBlank() }?.let { add(ProfileDetail(Icons.Outlined.Link, "LinkedIn: $it", "https://www.linkedin.com/in/$it")) }
    user.twitter?.takeIf { it.isNotBlank() }?.let { add(ProfileDetail(Icons.Outlined.Link, "X: @$it", "https://x.com/$it")) }
    user.discord?.takeIf { it.isNotBlank() }?.let { add(ProfileDetail(Icons.Outlined.Chat, "Discord: $it", null)) }
    user.createdAt?.takeIf { it.isNotBlank() }?.let { add(ProfileDetail(Icons.Outlined.CalendarMonth, "Joined ${formatGitLabTime(it)}", null)) }
  }
}

private fun String.withScheme(): String = if (startsWith("http://") || startsWith("https://")) this else "https://$this"

/** Bordered field that reads like an input: smiley, the status or a prompt, and a pencil. */
@Composable
private fun StatusField(message: String?, onClick: () -> Unit) {
  val c = Gl.colors
  val shape = RoundedCornerShape(6.dp)
  val text = message?.takeIf { it.isNotBlank() }
  Row(
    modifier = Modifier
      .padding(horizontal = GlSpace.margin)
      .fillMaxWidth()
      .clip(shape)
      .background(c.surface2)
      .border(1.dp, c.borderStrong, shape)
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Edit status")
      .defaultMinSize(minHeight = 52.dp)
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(Icons.Outlined.EmojiEmotions, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(22.dp))
    Spacer(Modifier.width(12.dp))
    Text(
      text = text ?: "Set your status",
      style = Gl.type.body,
      color = if (text != null) c.textPrimary else c.textSecondary,
      modifier = Modifier.weight(1f),
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Icon(Icons.Outlined.Edit, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
  }
}

@Composable
private fun DetailLine(icon: ImageVector, text: String, emphasized: Boolean, onClick: (() -> Unit)?) {
  val c = Gl.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open link") else Modifier)
      .defaultMinSize(minHeight = if (onClick != null) GlSpace.touch else 40.dp)
      .padding(horizontal = GlSpace.margin, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(icon, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(14.dp))
    Text(
      text = text,
      style = if (emphasized) Gl.type.body.copy(fontWeight = FontWeight.SemiBold) else Gl.type.body,
      color = if (emphasized) c.textPrimary else c.textSecondary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/** "16 followers • 14 following", counts in bold. */
@Composable
private fun FollowLine(followers: Int, following: Int) {
  val c = Gl.colors
  val bold = SpanStyle(color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
  val text = buildAnnotatedString {
    withStyle(bold) { append(followers.toString()) }
    append(if (followers == 1) " follower" else " followers")
    append("  •  ")
    withStyle(bold) { append(following.toString()) }
    append(" following")
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 40.dp)
      .padding(horizontal = GlSpace.margin, vertical = 6.dp)
      .semantics(mergeDescendants = true) { contentDescription = "$followers followers, $following following" },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(14.dp))
    Text(text, style = Gl.type.body, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

/** "username/README.md" heading in mono, a hairline, then the rendered document. */
@Composable
internal fun ProfileReadme(username: String, markdown: String, context: MarkdownContext = MarkdownContext(), footer: (@Composable () -> Unit)? = null) {
  val c = Gl.colors
  Column(Modifier.fillMaxWidth().background(c.bg)) {
    val title = buildAnnotatedString {
      if (username.isNotBlank()) {
        withStyle(SpanStyle(color = c.textSecondary)) { append(username) }
        withStyle(SpanStyle(color = c.textTertiary)) { append("/") }
      }
      withStyle(SpanStyle(color = c.textPrimary, fontWeight = FontWeight.SemiBold)) { append("README.md") }
    }
    Text(
      title,
      style = Gl.type.mono.copy(fontSize = Gl.type.body.fontSize, lineHeight = Gl.type.body.lineHeight),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = 18.dp).semantics { heading() },
    )
    GlDivider()
    MarkdownText(markdown, context = context, modifier = Modifier.padding(GlSpace.margin))
    footer?.invoke()
  }
}
