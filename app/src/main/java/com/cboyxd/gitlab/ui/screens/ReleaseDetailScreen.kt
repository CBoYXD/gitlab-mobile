package com.cboyxd.gitlab.ui.screens

import com.cboyxd.gitlab.ui.components.Avatar
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.cboyxd.gitlab.data.model.GitLabRelease
import com.cboyxd.gitlab.ui.components.EmptyState
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.FullScreenState
import com.cboyxd.gitlab.ui.components.GlBadge
import com.cboyxd.gitlab.ui.components.GlCard
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlIconAction
import com.cboyxd.gitlab.ui.components.GlRow
import com.cboyxd.gitlab.ui.components.GlSectionHeader
import com.cboyxd.gitlab.ui.components.GlTopBar
import com.cboyxd.gitlab.ui.components.IconTile
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.components.MarkdownContext
import com.cboyxd.gitlab.ui.components.MarkdownText
import com.cboyxd.gitlab.ui.components.MonoPill
import com.cboyxd.gitlab.ui.components.Tone
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.ProjectDetailState

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
  val listState = rememberLazyListState()
  val scrolledPastTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
  val releaseUrl = web?.let { "$it/-/releases/${Uri.encode(tagName)}" }
  Column(modifier = modifier.fillMaxSize().background(Gl.colors.bg)) {
    GlTopBar(
      title = release?.name?.takeIf { it.isNotBlank() } ?: tagName,
      overline = state.project.data?.pathWithNamespace?.replace("/", " / "),
      showTitle = scrolledPastTitle || release == null,
      onBack = onBack,
      actions = {
        DetailBarActions(
          webUrl = releaseUrl,
          title = release?.name?.takeIf { it.isNotBlank() } ?: tagName,
          menu = listOfNotNull(releaseUrl?.let { url -> DetailMenuItem("Open in browser", { onOpenInBrowser(url) }) }),
        )
      },
    )
    if (scrolledPastTitle) GlDivider()
    val error = releases.error
    when {
      release != null -> ReleaseBody(release, markdown, listState, onOpenCommit, onOpenInBrowser)
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
  listState: androidx.compose.foundation.lazy.LazyListState,
  onOpenCommit: (String) -> Unit,
  onOpenInBrowser: (String) -> Unit,
) {
  val c = Gl.colors
  LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = GlSpace.group)) {
    item(key = "header") {
      Column(modifier = Modifier.fillMaxWidth().padding(start = GlSpace.margin, end = GlSpace.margin, top = 8.dp, bottom = 20.dp)) {
        Text(
          release.name?.takeIf { it.isNotBlank() } ?: release.tagName,
          style = Gl.type.headline,
          color = c.textPrimary,
          modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          DetailRefPill(release.tagName)
          if (release.upcoming) {
            Spacer(Modifier.width(8.dp))
            GlBadge("Upcoming", tone = Tone.Warning)
          }
        }
        val author = release.author
        if (author != null || !release.releasedAt.isNullOrBlank()) {
          Spacer(Modifier.height(14.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            val name = author?.let { it.username.ifBlank { it.name } }.orEmpty()
            if (author != null) {
              Avatar(author.avatarUrl, name, 28.dp)
              Spacer(Modifier.width(10.dp))
            }
            Text(
              text = listOfNotNull(
                name.takeIf { it.isNotBlank() }?.let { "$it released this" } ?: "Released",
                release.releasedAt?.takeIf { it.isNotBlank() },
              ).joinToString(" · "),
              style = Gl.type.bodySmall,
              color = c.textSecondary,
            )
          }
        }
      }
    }

    release.commit?.let { commit ->
      item(key = "commit") {
        GlDivider()
        DetailRow(
          icon = Icons.Outlined.Commit,
          title = commit.title.ifBlank { commit.shortId },
          subtitle = listOf(commit.shortId, commit.committedDate.orEmpty()).filter { it.isNotBlank() }.joinToString(" · "),
          onClick = { onOpenCommit(commit.id) },
          onClickLabel = "Open commit",
        ) {
          Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = c.textTertiary)
        }
      }
    }

    item(key = "notes-header") { DetailSectionHeader("Release notes") }
    item(key = "notes") {
      val notes = release.description
      Column(Modifier.fillMaxWidth().padding(GlSpace.margin)) {
        if (notes.isNullOrBlank()) {
          Text("No release notes.", style = Gl.type.body, color = c.textSecondary)
        } else {
          MarkdownText(notes, context = markdown)
        }
      }
    }

    val assets = release.assets
    val links = assets?.links.orEmpty()
    val sources = assets?.sources.orEmpty()
    if (links.isNotEmpty() || sources.isNotEmpty()) {
      item(key = "assets-header") { DetailSectionHeader("Assets") }
      item(key = "assets") {
        Column {
          links.forEachIndexed { index, link ->
            if (index > 0) GlDivider(inset = 56.dp)
            DetailRow(
              icon = Icons.Outlined.Link,
              title = link.name.ifBlank { link.url },
              subtitle = link.url.substringAfter("://").substringBefore('/'),
              onClick = { onOpenInBrowser(link.url) },
              onClickLabel = "Open ${link.name.ifBlank { "asset" }} in browser",
            ) {
              link.linkType?.takeIf { it.isNotBlank() }?.let { GlBadge(it.replaceFirstChar { ch -> ch.uppercase() }) }
              Spacer(Modifier.width(4.dp))
              Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = c.textTertiary)
            }
          }
          sources.forEachIndexed { index, source ->
            if (index > 0 || links.isNotEmpty()) GlDivider(inset = 56.dp)
            DetailRow(
              icon = Icons.Outlined.Archive,
              title = "Source code (${source.format})",
              onClick = { onOpenInBrowser(source.url) },
              onClickLabel = "Download source code as ${source.format}",
            ) {
              Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = c.textTertiary)
            }
          }
        }
      }
    }

    item(key = "bottom") { Spacer(Modifier.navigationBarsPadding()) }
  }
}
