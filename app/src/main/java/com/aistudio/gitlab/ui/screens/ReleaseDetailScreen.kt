package com.aistudio.gitlab.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aistudio.gitlab.data.model.GitLabRelease
import com.aistudio.gitlab.ui.components.EmptyState
import com.aistudio.gitlab.ui.components.ErrorState
import com.aistudio.gitlab.ui.components.FullScreenState
import com.aistudio.gitlab.ui.components.GlBadge
import com.aistudio.gitlab.ui.components.GlCard
import com.aistudio.gitlab.ui.components.GlDivider
import com.aistudio.gitlab.ui.components.GlIconAction
import com.aistudio.gitlab.ui.components.GlRow
import com.aistudio.gitlab.ui.components.GlSectionHeader
import com.aistudio.gitlab.ui.components.GlTopBar
import com.aistudio.gitlab.ui.components.IconTile
import com.aistudio.gitlab.ui.components.LoadingState
import com.aistudio.gitlab.ui.components.MarkdownContext
import com.aistudio.gitlab.ui.components.MarkdownText
import com.aistudio.gitlab.ui.components.MonoPill
import com.aistudio.gitlab.ui.components.Tone
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlSpace
import com.aistudio.gitlab.ui.viewmodel.ProjectDetailState

/** A release: tag, notes rendered as Markdown, and downloadable assets. */
@Composable
fun ReleaseDetailScreen(
  state: ProjectDetailState,
  tagName: String,
  markdown: MarkdownContext,
  onBack: () -> Unit,
  onRetry: () -> Unit,
  onOpenCommit: (String) -> Unit,
  onOpenInBrowser: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val releases = state.releases
  val release = releases.data.firstOrNull { it.tagName == tagName }
  val web = state.webUrl
  Column(modifier = modifier.fillMaxSize().background(Gl.colors.bg)) {
    GlTopBar(
      title = release?.name?.takeIf { it.isNotBlank() } ?: tagName,
      subtitle = state.project.data?.name,
      onBack = onBack,
      actions = {
        if (web != null) {
          GlIconAction(
            Icons.AutoMirrored.Outlined.OpenInNew,
            "Open in browser",
            onClick = { onOpenInBrowser("$web/-/releases/${Uri.encode(tagName)}") },
          )
        }
      },
    )
    val error = releases.error
    when {
      release != null -> ReleaseBody(release, markdown, onOpenCommit, onOpenInBrowser)
      error != null -> FullScreenState { ErrorState(message = error, onRetry = onRetry) }
      releases.loading || !releases.loaded -> FullScreenState { LoadingState(label = "Loading release") }
      else -> FullScreenState {
        EmptyState(
          icon = Icons.Outlined.NewReleases,
          title = "Release not found",
          body = "It may have been deleted. Go back to the release list and refresh.",
          actionLabel = "Refresh releases",
          onAction = onRetry,
        )
      }
    }
  }
}

@Composable
private fun ReleaseBody(
  release: GitLabRelease,
  markdown: MarkdownContext,
  onOpenCommit: (String) -> Unit,
  onOpenInBrowser: (String) -> Unit,
) {
  val c = Gl.colors
  LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = GlSpace.group)) {
    item(key = "header") {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = GlSpace.margin, vertical = 8.dp)) {
        Text(
          release.name?.takeIf { it.isNotBlank() } ?: release.tagName,
          style = Gl.type.title,
          color = c.textPrimary,
          modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          MonoPill(release.tagName)
          if (release.upcoming) {
            Spacer(Modifier.width(8.dp))
            GlBadge("Upcoming", tone = Tone.Warning)
          }
        }
        val meta = listOfNotNull(
          release.releasedAt?.takeIf { it.isNotBlank() }?.let { "Released $it" },
          release.author?.let { it.name.ifBlank { it.username } }?.takeIf { it.isNotBlank() }?.let { "by $it" },
        ).joinToString(" ")
        if (meta.isNotBlank()) {
          Spacer(Modifier.height(6.dp))
          Text(meta, style = Gl.type.meta, color = c.textSecondary)
        }
      }
    }

    release.commit?.let { commit ->
      item(key = "commit") {
        GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = 8.dp)) {
          GlRow(
            title = commit.title.ifBlank { commit.shortId },
            subtitle = listOf(commit.shortId, commit.committedDate.orEmpty()).filter { it.isNotBlank() }.joinToString(" · "),
            leading = { IconTile(Icons.Outlined.Commit) },
            onClick = { onOpenCommit(commit.id) },
            onClickLabel = "Open commit",
          )
        }
      }
    }

    item(key = "notes-header") { GlSectionHeader("Release notes") }
    item(key = "notes") {
      val notes = release.description
      GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin), contentPadding = PaddingValues(GlSpace.margin)) {
        if (notes.isNullOrBlank()) {
          Text("No release notes.", style = Gl.type.bodySmall, color = c.textSecondary)
        } else {
          MarkdownText(notes, context = markdown)
        }
      }
    }

    val assets = release.assets
    val links = assets?.links.orEmpty()
    val sources = assets?.sources.orEmpty()
    if (links.isNotEmpty() || sources.isNotEmpty()) {
      item(key = "assets-header") { GlSectionHeader("Assets") }
      item(key = "assets") {
        GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin)) {
          links.forEachIndexed { index, link ->
            if (index > 0) GlDivider(inset = 64.dp)
            GlRow(
              title = link.name.ifBlank { link.url },
              subtitle = link.url.substringAfter("://").substringBefore('/'),
              leading = { IconTile(Icons.Outlined.Link) },
              trailing = {
                link.linkType?.takeIf { it.isNotBlank() }?.let { GlBadge(it.replaceFirstChar { ch -> ch.uppercase() }) }
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = c.textTertiary)
              },
              onClick = { onOpenInBrowser(link.url) },
              onClickLabel = "Open ${link.name.ifBlank { "asset" }} in browser",
            )
          }
          sources.forEachIndexed { index, source ->
            if (index > 0 || links.isNotEmpty()) GlDivider(inset = 64.dp)
            GlRow(
              title = "Source code (${source.format})",
              leading = { IconTile(Icons.Outlined.Archive) },
              trailing = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = c.textTertiary) },
              onClick = { onOpenInBrowser(source.url) },
              onClickLabel = "Download source code as ${source.format}",
              titleMaxLines = 1,
            )
          }
        }
      }
    }

    item(key = "bottom") { Spacer(Modifier.navigationBarsPadding()) }
  }
}
