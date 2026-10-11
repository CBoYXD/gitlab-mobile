package com.cboyxd.gitlab.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.ui.components.GlIconAction
import com.cboyxd.gitlab.ui.components.StatusStyle
import com.cboyxd.gitlab.ui.components.Tone
import com.cboyxd.gitlab.ui.components.pressable
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace

// Pieces shared by the issue, merge request, commit, release and pipeline pages.

/** Filled state pill ("Open", "Merged"): white on the solid status color, icon and label both. */
@Composable
internal fun DetailStatePill(style: StatusStyle, modifier: Modifier = Modifier) {
  val c = Gl.colors
  val (bg, fg) = when (style.tone) {
    Tone.Success -> c.successSolid to Color.White
    Tone.Danger -> c.dangerSolid to Color.White
    Tone.Merged -> c.mergedSolid to Color.White
    Tone.Info -> c.primarySolid to Color.White
    Tone.Warning -> c.warningBg to c.warning
    Tone.Neutral -> c.surface3 to c.textPrimary
  }
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(50))
      .background(bg)
      .padding(start = 10.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(style.icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
    Spacer(Modifier.width(6.dp))
    Text(style.label, style = Gl.type.label, color = fg, maxLines = 1)
  }
}

/** Branch or ref name in a blue monospace chip, as GitHub shows head → base. */
@Composable
internal fun DetailRefPill(name: String, modifier: Modifier = Modifier) {
  val c = Gl.colors
  Text(
    text = name,
    style = Gl.type.mono,
    color = c.accent,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(c.accentBg)
      .border(1.dp, c.accent.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
      .padding(horizontal = 8.dp, vertical = 3.dp),
  )
}

/** Bold heading on a darker band that opens a section of a detail page ("Changes", "Status"). */
@Composable
internal fun DetailSectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable RowScope.() -> Unit)? = null) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(Gl.colors.sunken)
      .padding(start = GlSpace.margin, end = 4.dp, top = 24.dp, bottom = 12.dp)
      .heightIn(min = 32.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      title,
      style = Gl.type.sectionTitle,
      color = Gl.colors.textPrimary,
      modifier = Modifier.weight(1f).semantics { heading() },
    )
    trailing?.invoke(this)
  }
}

/** Flat navigation row: leading icon, title and optional detail, trailing content. */
@Composable
internal fun DetailRow(
  icon: ImageVector?,
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  iconTint: Color = Gl.colors.textSecondary,
  onClick: (() -> Unit)? = null,
  onClickLabel: String? = null,
  leading: (@Composable () -> Unit)? = null,
  trailing: (@Composable RowScope.() -> Unit)? = null,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(Gl.colors.bg)
      .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick) else Modifier.semantics(mergeDescendants = true) {})
      .defaultMinSize(minHeight = 60.dp)
      .padding(horizontal = GlSpace.margin, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    when {
      leading != null -> { leading(); Spacer(Modifier.width(16.dp)) }
      icon != null -> {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
      }
    }
    Column(Modifier.weight(1f)) {
      Text(title, style = Gl.type.rowTitle, color = Gl.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
      if (!subtitle.isNullOrBlank()) {
        Text(subtitle, style = Gl.type.bodySmall, color = Gl.colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
      }
    }
    if (trailing != null) {
      Spacer(Modifier.width(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
  }
}

internal data class DetailMenuItem(val label: String, val onClick: () -> Unit, val enabled: Boolean = true)

/** Blue share action plus an overflow menu, the trailing actions of every detail bar. */
@Composable
internal fun RowScope.DetailBarActions(webUrl: String?, title: String, menu: List<DetailMenuItem>) {
  val context = LocalContext.current
  if (!webUrl.isNullOrBlank()) {
    GlIconAction(Icons.Outlined.Share, "Share", onClick = { shareLink(context, webUrl, title) })
  }
  if (menu.isNotEmpty()) {
    var open by remember { mutableStateOf(false) }
    Box {
      GlIconAction(Icons.Outlined.MoreVert, "More options", onClick = { open = true })
      DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Gl.colors.surface2) {
        menu.forEach { item ->
          DropdownMenuItem(
            text = { Text(item.label, style = Gl.type.body, color = if (item.enabled) Gl.colors.textPrimary else Gl.colors.textTertiary) },
            enabled = item.enabled,
            onClick = {
              open = false
              item.onClick()
            },
          )
        }
      }
    }
  }
}

internal fun shareLink(context: Context, url: String, title: String) {
  val send = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_SUBJECT, title)
    putExtra(Intent.EXTRA_TEXT, url)
  }
  try {
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  } catch (_: ActivityNotFoundException) {
    // Nothing can share text; nothing sensible to do.
  }
}

/**
 * Floating actions at the bottom right of an issue or merge request: a "Comment" pill when the
 * viewer can comment, and a round button for the details sheet.
 */
@Composable
internal fun BoxScope.DetailFloatingActions(onComment: (() -> Unit)?, onInfo: () -> Unit) {
  val c = Gl.colors
  Row(
    modifier = Modifier
      .align(Alignment.BottomEnd)
      .navigationBarsPadding()
      .padding(end = GlSpace.margin, bottom = GlSpace.margin),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (onComment != null) {
      Row(
        modifier = Modifier
          .shadow(6.dp, RoundedCornerShape(14.dp))
          .clip(RoundedCornerShape(14.dp))
          .background(c.surface3)
          .pressable(onClick = onComment, onClickLabel = "Write a comment")
          .heightIn(min = 52.dp)
          .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, tint = c.textPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Comment", style = Gl.type.label, color = c.textPrimary)
      }
    }
    Box(
      modifier = Modifier
        .shadow(6.dp, RoundedCornerShape(14.dp))
        .size(52.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(c.surface3)
        .pressable(onClick = onInfo, onClickLabel = "Show details"),
      contentAlignment = Alignment.Center,
    ) {
      Icon(Icons.Outlined.Info, contentDescription = "Details", tint = c.textPrimary)
    }
  }
}

/** Small round count chip for reactions (👍 3). */
@Composable
internal fun DetailReactionChip(emoji: String, count: Int, description: String) {
  val c = Gl.colors
  Row(
    modifier = Modifier
      .clip(CircleShape)
      .background(c.surface2)
      .border(1.dp, c.border, CircleShape)
      .padding(horizontal = 10.dp, vertical = 4.dp)
      .semantics(mergeDescendants = true) { contentDescription = description },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(emoji, style = Gl.type.bodySmall)
    Spacer(Modifier.width(6.dp))
    Text(count.toString(), style = Gl.type.numeric, color = c.textPrimary)
  }
}

/** "3h ago" → "3h": ages read shorter beside a name, as on GitHub. */
internal fun shortAge(time: String): String = time.removeSuffix(" ago")
