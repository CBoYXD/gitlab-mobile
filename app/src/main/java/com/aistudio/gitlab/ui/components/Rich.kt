package com.aistudio.gitlab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistudio.gitlab.data.model.GitLabEvent
import com.aistudio.gitlab.data.model.GitLabLabel
import com.aistudio.gitlab.data.model.GitLabUser
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlSpace
import com.aistudio.gitlab.ui.viewmodel.ScreenDestination

/**
 * A GitLab label in the project's own colors. The API supplies text_color chosen for contrast
 * against the label color; scoped labels (key::value) show both halves.
 */
@Composable
fun ColoredLabel(name: String, label: GitLabLabel?, modifier: Modifier = Modifier) {
  val bg = label?.color?.parseHexColor()
  val fg = label?.textColor?.parseHexColor()
  if (bg == null || fg == null) {
    GlBadge(text = name, modifier = modifier)
    return
  }
  val scoped = name.split("::", limit = 2)
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(50))
      .background(bg)
      .semantics(mergeDescendants = true) { contentDescription = "Label $name" },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(scoped[0], style = Gl.type.badge, color = fg, maxLines = 1, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    if (scoped.size == 2) {
      Text(
        scoped[1],
        style = Gl.type.badge,
        color = Gl.colors.textPrimary,
        maxLines = 1,
        modifier = Modifier
          .padding(1.5.dp)
          .clip(RoundedCornerShape(50))
          .background(Gl.colors.surface)
          .padding(horizontal = 7.dp, vertical = 0.5.dp),
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabelRow(names: List<String>, labels: Map<String, GitLabLabel>, modifier: Modifier = Modifier, max: Int = Int.MAX_VALUE) {
  if (names.isEmpty()) return
  FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    names.take(max).forEach { ColoredLabel(it, labels[it]) }
    if (names.size > max) GlBadge("+${names.size - max}")
  }
}

private fun String.parseHexColor(): Color? {
  val hex = trim().removePrefix("#")
  val value = when (hex.length) {
    6 -> hex.toLongOrNull(16)?.let { it or 0xFF000000 }
    3 -> hex.map { "$it$it" }.joinToString("").toLongOrNull(16)?.let { it or 0xFF000000 }
    else -> null
  } ?: return null
  return Color(value.toInt())
}

/** A person: avatar, name, @handle. */
@Composable
fun UserRow(user: GitLabUser, modifier: Modifier = Modifier, trailing: String? = null, onClick: (() -> Unit)? = null) {
  GlRow(
    title = user.name.ifBlank { user.username },
    subtitle = user.username.takeIf { it.isNotBlank() }?.let { "@$it" },
    leading = { Avatar(user.avatarUrl, user.name.ifBlank { user.username }, 36.dp) },
    trailing = trailing?.let { { Text(it, style = Gl.type.numeric, color = Gl.colors.textSecondary) } },
    onClick = onClick,
    modifier = modifier,
  )
}

/** Proportional language bar with a legend, as on a GitHub repository page. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguageBar(languages: List<Pair<String, Double>>, modifier: Modifier = Modifier) {
  if (languages.isEmpty()) return
  val shown = languages.take(6)
  val rest = languages.drop(6).sumOf { it.second }
  val parts = if (rest > 0.05) shown + ("Other" to rest) else shown
  val c = Gl.colors
  // Neutral-to-status palette cycled for categories; each language also has its name and share.
  val palette = listOf(c.syntaxLiteral, c.syntaxKeyword, c.syntaxString, c.syntaxMeta, c.danger, c.info, c.textTertiary)
  Column(modifier = modifier) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp))
        .semantics { contentDescription = parts.joinToString { "${it.first} ${String.format(java.util.Locale.US, "%.1f", it.second)}%" } },
    ) {
      parts.forEachIndexed { index, (_, share) ->
        Box(
          Modifier
            .weight(share.toFloat().coerceAtLeast(0.5f))
            .height(8.dp)
            .background(palette[index % palette.size])
        )
        if (index < parts.lastIndex) Spacer(Modifier.width(2.dp))
      }
    }
    Spacer(Modifier.height(10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      parts.forEachIndexed { index, (name, share) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.size(8.dp).clip(CircleShape).background(palette[index % palette.size]))
          Spacer(Modifier.width(6.dp))
          Text(name, style = Gl.type.meta, color = c.textPrimary)
          Spacer(Modifier.width(4.dp))
          Text(String.format(java.util.Locale.US, "%.1f%%", share), style = Gl.type.meta.copy(fontFeatureSettings = "tnum"), color = c.textSecondary)
        }
      }
    }
  }
}

/** Where tapping an activity event goes, if the app has a screen for it. */
fun GitLabEvent.destination(): ScreenDestination? {
  val pid = projectId ?: return null
  val noteIid = note?.noteableIid
  return when {
    targetType == "Issue" && targetIid != null -> ScreenDestination.IssueDetail(pid, targetIid)
    targetType == "MergeRequest" && targetIid != null -> ScreenDestination.MergeRequestDetail(pid, targetIid)
    note?.noteableType == "Issue" && noteIid != null -> ScreenDestination.IssueDetail(pid, noteIid)
    note?.noteableType == "MergeRequest" && noteIid != null -> ScreenDestination.MergeRequestDetail(pid, noteIid)
    else -> ScreenDestination.ProjectDetail(pid)
  }
}

/** One-line summary of an activity event in plain words. */
fun GitLabEvent.summary(): String {
  val who = author.name.ifBlank { author.username }
  val push = pushData
  return when {
    push != null -> {
      val count = push.commitCount
      val what = if (count == 1) "1 commit" else "$count commits"
      when (push.action) {
        "created" -> "$who created ${push.refType ?: "branch"} ${push.ref.orEmpty()}"
        "removed" -> "$who deleted ${push.refType ?: "branch"} ${push.ref.orEmpty()}"
        else -> "$who pushed $what to ${push.ref.orEmpty()}"
      }
    }
    note != null -> "$who commented on ${readableTarget(note.noteableType)}${note.noteableIid?.let { " ${referencePrefix(note.noteableType)}$it" }.orEmpty()}"
    else -> {
      val verb = actionName.replace('_', ' ').ifBlank { "updated" }
      val ref = targetIid?.let { " ${referencePrefix(targetType)}$it" }.orEmpty()
      "$who $verb ${readableTarget(targetType)}$ref".trim()
    }
  }
}

private fun readableTarget(type: String?) = when (type) {
  "Issue" -> "issue"
  "MergeRequest" -> "merge request"
  "Milestone" -> "milestone"
  "Note", "DiffNote", "DiscussionNote" -> "comment"
  "WikiPage::Meta" -> "wiki page"
  null -> ""
  else -> type.lowercase()
}

private fun referencePrefix(type: String?) = if (type == "MergeRequest") "!" else "#"

@Composable
fun EventRow(event: GitLabEvent, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
  val detail = event.pushData?.commitTitle ?: event.targetTitle ?: event.note?.body?.lineSequence()?.firstOrNull()
  Row(
    modifier = modifier
      .fillMaxWidth()
      .then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button) else Modifier.semantics(mergeDescendants = true) {})
      .defaultMinSize(minHeight = 56.dp)
      .padding(horizontal = GlSpace.margin, vertical = 10.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Avatar(event.author.avatarUrl, event.author.name.ifBlank { event.author.username }, 32.dp)
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(event.summary(), style = Gl.type.bodySmall, color = Gl.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
      if (!detail.isNullOrBlank()) {
        Spacer(Modifier.height(2.dp))
        Text(detail, style = Gl.type.meta, color = Gl.colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
      }
      Spacer(Modifier.height(2.dp))
      Text(event.createdAt, style = Gl.type.meta, color = Gl.colors.textTertiary)
    }
  }
}

/** Small stat used on detail pages: value in tabular figures, label beneath. */
@Composable
fun StatBlock(value: String, label: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
    Text(value, style = Gl.type.sectionTitle.copy(fontFeatureSettings = "tnum"), color = Gl.colors.textPrimary, maxLines = 1)
    Text(label, style = Gl.type.meta, color = Gl.colors.textSecondary, maxLines = 1)
  }
}

/** Grouped digits for counters: 12 452 reads faster than 12452. */
fun formatCount(value: Int): String = String.format(java.util.Locale.US, "%,d", value).replace(',', '\u2009')

/** Compact counters as GitHub shows them: 999, 6.1k, 12.5k, 1.2M. */
fun formatCompact(value: Int): String = when {
  value < 1_000 -> value.toString()
  value < 1_000_000 -> trimZero(value / 1_000.0) + "k"
  else -> trimZero(value / 1_000_000.0) + "M"
}

private fun trimZero(v: Double): String {
  val rounded = if (v >= 100) String.format(java.util.Locale.US, "%.0f", v) else String.format(java.util.Locale.US, "%.1f", v)
  return rounded.removeSuffix(".0")
}

/** Inline stat: icon, bold number, quiet label, e.g. "☆ 6.1k stars". The full number is announced. */
@Composable
fun InlineStat(icon: androidx.compose.ui.graphics.vector.ImageVector, value: Int, singular: String, plural: String, modifier: Modifier = Modifier) {
  val c = Gl.colors
  Row(
    modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$value ${if (value == 1) singular else plural}" },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    androidx.compose.material3.Icon(icon, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(6.dp))
    Text(formatCompact(value), style = Gl.type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontFeatureSettings = "tnum"), color = c.textPrimary)
    Spacer(Modifier.width(4.dp))
    Text(if (value == 1) singular else plural, style = Gl.type.label, color = c.textSecondary)
  }
}
