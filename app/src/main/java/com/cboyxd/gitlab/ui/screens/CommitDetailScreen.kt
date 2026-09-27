package com.cboyxd.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.data.model.GitLabPipeline
import com.cboyxd.gitlab.ui.components.Avatar
import com.cboyxd.gitlab.ui.components.EmptyState
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.FullScreenState
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlTopBar
import com.cboyxd.gitlab.ui.components.InlineError
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.components.MonoPill
import com.cboyxd.gitlab.ui.components.diffFileItems
import com.cboyxd.gitlab.ui.components.diffStats
import com.cboyxd.gitlab.ui.components.pipelineStatusStyle
import com.cboyxd.gitlab.ui.components.rememberPreparedDiffs
import com.cboyxd.gitlab.ui.components.toneColors
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.CommitDetailState
import java.util.Locale

/** A commit: message, author, SHA and parents, its pipeline, then every changed file. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommitDetailScreen(
  state: CommitDetailState,
  onRetry: () -> Unit,
  onOpenPipeline: (GitLabPipeline) -> Unit,
  onOpenInBrowser: (String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val commit = state.commit.data
  val c = Gl.colors
  val clipboard = LocalClipboardManager.current
  val listState = rememberLazyListState()
  val scrolledPastTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
  Column(modifier = modifier.fillMaxSize().background(c.bg)) {
    val menu = buildList {
      commit?.webUrl?.takeIf { it.isNotBlank() }?.let { web -> add(DetailMenuItem("Open in browser", { onOpenInBrowser(web) })) }
      commit?.let { add(DetailMenuItem("Copy SHA", { clipboard.setText(AnnotatedString(it.id)) })) }
    }
    GlTopBar(
      title = commit?.title ?: state.sha.take(8),
      overline = "Commit ${commit?.shortId ?: state.sha.take(8)}",
      showTitle = scrolledPastTitle || commit == null,
      onBack = onBack,
    ) {
      DetailBarActions(webUrl = commit?.webUrl, title = commit?.title.orEmpty(), menu = menu)
    }
    if (scrolledPastTitle) GlDivider()
    when {
      commit == null && state.commit.error != null && !state.commit.initialLoading -> FullScreenState { ErrorState(state.commit.error, onRetry) }
      commit == null -> FullScreenState { LoadingState(label = "Loading commit") }
      else -> {
        val diffs = state.diffs
        val stats = remember(diffs.data) { diffStats(diffs.data) }
        val prepared = rememberPreparedDiffs(diffs.data)
        var collapsed by remember(diffs.data) { mutableStateOf(emptySet<Int>()) }
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = GlSpace.group),
        ) {
          item(key = "header") {
            Column(
              modifier = Modifier.fillMaxWidth().padding(start = GlSpace.margin, end = GlSpace.margin, top = 8.dp, bottom = 20.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(commit.title, style = Gl.type.headline, color = c.textPrimary, modifier = Modifier.semantics { heading() })
              val body = commit.message.substringAfter('\n', "").trim()
              if (body.isNotEmpty()) {
                Text(body, style = Gl.type.mono, color = c.textSecondary)
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                val author = commit.authorName.ifBlank { "Someone" }
                Avatar(null, author, 28.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                  buildAnnotatedString {
                    pushStyle(SpanStyle(color = c.textPrimary, fontWeight = FontWeight.SemiBold))
                    append(author)
                    pop()
                    append(" committed")
                    (commit.committedDate ?: commit.createdAt).takeIf { it.isNotBlank() }?.let { append(" · $it") }
                  },
                  style = Gl.type.bodySmall,
                  color = c.textSecondary,
                )
              }
              FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailRefPill(commit.shortId)
                if (commit.parentIds.isNotEmpty()) {
                  Text("parents", style = Gl.type.meta, color = c.textTertiary, modifier = Modifier.align(Alignment.CenterVertically))
                  commit.parentIds.forEach { MonoPill(it.take(8), Modifier.align(Alignment.CenterVertically)) }
                }
              }
            }
          }
          commit.lastPipeline?.let { pipeline ->
            item(key = "pipeline-header") { DetailSectionHeader("Pipeline") }
            item(key = "pipeline") {
              val style = pipelineStatusStyle(pipeline.status)
              DetailRow(
                icon = style.icon,
                iconTint = toneColors(style.tone).first,
                title = "Pipeline #${pipeline.id} ${style.label.lowercase(Locale.ROOT)}",
                subtitle = pipeline.ref.takeIf { it.isNotBlank() },
                onClick = { onOpenPipeline(pipeline) },
                onClickLabel = "Open pipeline",
              ) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = c.textTertiary)
              }
            }
          }
          item(key = "changes-header") {
            DetailSectionHeader("Files changed") {
              if (diffs.loaded && diffs.error == null) {
                Row(
                  modifier = Modifier
                    .padding(end = 12.dp)
                    .semantics(mergeDescendants = true) {
                      contentDescription = "${diffs.data.size} files, ${stats.added} additions, ${stats.removed} deletions"
                    },
                ) {
                  Text("+${stats.added}", style = Gl.type.numeric, color = c.success)
                  Spacer(Modifier.width(6.dp))
                  Text("−${stats.removed}", style = Gl.type.numeric, color = c.danger)
                }
              }
            }
          }
          when {
            diffs.initialLoading -> item(key = "diff-loading") { LoadingState(label = "Loading changes") }
            diffs.error != null && diffs.data.isEmpty() -> item(key = "diff-error") {
              InlineError(diffs.error, modifier = Modifier.padding(GlSpace.margin), onRetry = onRetry)
            }
            diffs.data.isEmpty() -> item(key = "diff-empty") {
              EmptyState(Icons.Outlined.Difference, "No changes", "This commit doesn't change any files.")
            }
            else -> diffFileItems(
              diffs = diffs.data,
              parsed = prepared.parsed,
              prepared = prepared.texts,
              collapsed = collapsed,
              viewed = emptySet(),
              onToggleCollapsed = { i -> collapsed = if (i in collapsed) collapsed - i else collapsed + i },
              onToggleViewed = null,
            )
          }
          item(key = "bottom") { Spacer(Modifier.height(0.dp).navigationBarsPadding()) }
        }
      }
    }
  }
}

