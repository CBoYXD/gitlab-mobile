package com.cboyxd.gitlab.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cboyxd.gitlab.data.model.GitLabProject
import com.cboyxd.gitlab.ui.components.compactAge
import com.cboyxd.gitlab.ui.components.formatCompact
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace

/**
 * A project in the Projects tab, search and group lists, like a GitHub repository row: avatar, the
 * namespace above the name, the description, then stars, forks, visibility and last activity.
 */
@Composable
internal fun ListsProjectRow(
  project: GitLabProject,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  showStats: Boolean = true,
  trailing: (@Composable RowScope.() -> Unit)? = null,
) {
  val c = Gl.colors
  val namespace = project.namespace?.fullPath?.takeIf { it.isNotBlank() }
    ?: project.nameWithNamespace.substringBeforeLast("/", "").trim()
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open ${project.name}")
      .defaultMinSize(minHeight = 64.dp)
      .padding(start = GlSpace.margin, end = if (trailing != null) 4.dp else GlSpace.margin, top = 12.dp, bottom = 12.dp),
    verticalAlignment = Alignment.Top,
  ) {
    ProjectTile(project, size = 32.dp, modifier = Modifier.padding(top = 2.dp))
    Spacer(Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
      if (namespace.isNotBlank()) {
        Text(namespace, style = Gl.type.bodySmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Text(
        project.name,
        style = Gl.type.rowTitle.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
        color = c.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      val description = project.description?.lineSequence()?.firstOrNull { it.isNotBlank() }
      if (!description.isNullOrBlank()) {
        Spacer(Modifier.height(2.dp))
        Text(description, style = Gl.type.bodySmall, color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
      }
      if (showStats) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          ProjectStat(Icons.Outlined.StarOutline, formatCompact(project.starCount), "${project.starCount} stars")
          ProjectStat(Icons.Outlined.CallSplit, formatCompact(project.forksCount), "${project.forksCount} forks")
          when (project.visibility.lowercase()) {
            "private" -> ProjectStat(Icons.Outlined.Lock, "Private", "Private")
            "internal" -> ProjectStat(Icons.Outlined.Shield, "Internal", "Internal")
          }
          val age = compactAge(project.lastActivityAt)
          if (age.isNotBlank()) {
            Text(
              "Updated $age",
              style = Gl.type.meta,
              color = c.textTertiary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
          }
        }
      }
    }
    if (trailing != null) {
      Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
  }
}

@Composable
private fun ProjectStat(icon: ImageVector, value: String, description: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
  ) {
    Icon(icon, contentDescription = null, tint = Gl.colors.textSecondary, modifier = Modifier.size(15.dp))
    Spacer(Modifier.width(4.dp))
    Text(value, style = Gl.type.meta.copy(fontFeatureSettings = "tnum"), color = Gl.colors.textSecondary, maxLines = 1)
  }
}
