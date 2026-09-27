package com.aistudio.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.CallMerge
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.aistudio.gitlab.data.model.GitLabBranch
import com.aistudio.gitlab.data.model.GitLabContributor
import com.aistudio.gitlab.data.model.GitLabIssue
import com.aistudio.gitlab.data.model.GitLabLabel
import com.aistudio.gitlab.data.model.GitLabMergeRequest
import com.aistudio.gitlab.data.model.GitLabPipeline
import com.aistudio.gitlab.data.model.GitLabRelease
import com.aistudio.gitlab.data.model.GitLabTag
import com.aistudio.gitlab.ui.components.Avatar
import com.aistudio.gitlab.ui.components.CommitRow
import com.aistudio.gitlab.ui.components.EmptyState
import com.aistudio.gitlab.ui.components.ErrorState
import com.aistudio.gitlab.ui.components.EventRow
import com.aistudio.gitlab.ui.components.FullScreenState
import com.aistudio.gitlab.ui.components.GlBadge
import com.aistudio.gitlab.ui.components.GlDivider
import com.aistudio.gitlab.ui.components.GlFilterRow
import com.aistudio.gitlab.ui.components.GlTopBar
import com.aistudio.gitlab.ui.components.InlineError
import com.aistudio.gitlab.ui.components.IssueRow
import com.aistudio.gitlab.ui.components.LoadingState
import com.aistudio.gitlab.ui.components.MergeRequestRow
import com.aistudio.gitlab.ui.components.MonoPill
import com.aistudio.gitlab.ui.components.PipelineCard
import com.aistudio.gitlab.ui.components.Tone
import com.aistudio.gitlab.ui.components.destination
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlSpace
import com.aistudio.gitlab.ui.viewmodel.Loadable
import com.aistudio.gitlab.ui.viewmodel.ProjectDetailState
import com.aistudio.gitlab.ui.viewmodel.ProjectSection
import com.aistudio.gitlab.ui.viewmodel.ScreenDestination

private enum class SectionIssueFilter(val label: String) { OPEN("Open"), CLOSED("Closed"), ALL("All") }
private enum class SectionMrFilter(val label: String) { OPEN("Open"), MERGED("Merged"), CLOSED("Closed"), ALL("All") }

/** One of a project's lists: issues, merge requests, pipelines, commits, refs, releases, people, activity. */
@Composable
fun ProjectSectionScreen(
  section: ProjectSection,
  state: ProjectDetailState,
  labels: Map<String, GitLabLabel>,
  canWrite: Boolean,
  onBack: () -> Unit,
  onRetry: (ProjectSection) -> Unit,
  onOpenIssue: (GitLabIssue) -> Unit,
  onOpenMergeRequest: (GitLabMergeRequest) -> Unit,
  onOpenPipeline: (GitLabPipeline) -> Unit,
  onRetryPipeline: (GitLabPipeline) -> Unit,
  onOpenCommit: (String) -> Unit,
  onBrowseRef: (String) -> Unit,
  onOpenRelease: (String) -> Unit,
  onNavigate: (ScreenDestination) -> Unit,
  @Suppress("UNUSED_PARAMETER") onOpenInBrowser: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val retry = { onRetry(section) }
  Column(modifier = modifier.fillMaxSize().background(Gl.colors.bg)) {
    GlTopBar(title = section.title, subtitle = state.project.data?.name, onBack = onBack)
    when (section) {
      ProjectSection.ISSUES -> {
        var filter by rememberSaveable { mutableStateOf(SectionIssueFilter.OPEN) }
        val all = state.issues.data
        GlFilterRow(
          options = SectionIssueFilter.entries.toList(),
          selected = filter,
          label = { f -> "${f.label} ${countIssues(all, f)}" },
          onSelect = { filter = it },
        )
        val visible = all.filter { matchesIssue(it, filter) }
        Feed(
          feed = state.issues,
          visibleEmpty = visible.isEmpty(),
          onRetry = retry,
          empty = EmptyText(Icons.Outlined.Adjust, "No ${filter.label.lowercase()} issues", "Issues in this project appear here."),
        ) {
          items(visible, key = { "${it.projectId}-${it.iid}" }) { issue ->
            IssueRow(issue = issue, onClick = { onOpenIssue(issue) }, labels = labels)
            GlDivider(inset = 48.dp)
          }
        }
      }
      ProjectSection.MERGE_REQUESTS -> {
        var filter by rememberSaveable { mutableStateOf(SectionMrFilter.OPEN) }
        val all = state.mergeRequests.data
        GlFilterRow(
          options = SectionMrFilter.entries.toList(),
          selected = filter,
          label = { f -> "${f.label} ${all.count { matchesMr(it, f) }}" },
          onSelect = { filter = it },
        )
        val visible = all.filter { matchesMr(it, filter) }
        Feed(
          feed = state.mergeRequests,
          visibleEmpty = visible.isEmpty(),
          onRetry = retry,
          empty = EmptyText(Icons.Outlined.CallMerge, "No ${filter.label.lowercase()} merge requests", "Merge requests in this project appear here."),
        ) {
          items(visible, key = { "${it.projectId}-${it.iid}" }) { mr ->
            MergeRequestRow(mr = mr, onClick = { onOpenMergeRequest(mr) }, labels = labels)
            GlDivider(inset = 48.dp)
          }
        }
      }
      ProjectSection.PIPELINES -> Feed(
        feed = state.pipelines,
        visibleEmpty = state.pipelines.data.isEmpty(),
        onRetry = retry,
        empty = EmptyText(Icons.Outlined.PlayCircleOutline, "No pipelines yet", "Pipelines run for this project appear here."),
        spaced = true,
      ) {
        items(state.pipelines.data, key = { it.id }) { pipeline ->
          PipelineCard(
            pipeline = pipeline,
            onRetry = if (canWrite) onRetryPipeline else null,
            onClick = { onOpenPipeline(pipeline) },
            modifier = Modifier.padding(horizontal = GlSpace.margin),
          )
        }
      }
      ProjectSection.COMMITS -> {
        state.effectiveRef?.let { ref ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = GlSpace.margin, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("On", style = Gl.type.meta, color = Gl.colors.textSecondary)
            Spacer(Modifier.width(8.dp))
            MonoPill(ref)
          }
        }
        Feed(
          feed = state.commits,
          visibleEmpty = state.commits.data.isEmpty(),
          onRetry = retry,
          empty = EmptyText(Icons.Outlined.Commit, "No commits yet", "Commits on this branch appear here."),
        ) {
          items(state.commits.data, key = { it.id }) { commit ->
            CommitRow(commit = commit, onClick = { onOpenCommit(commit.id) })
            GlDivider(inset = GlSpace.margin)
          }
        }
      }
      ProjectSection.BRANCHES -> Feed(
        feed = state.branches,
        visibleEmpty = state.branches.data.isEmpty(),
        onRetry = retry,
        empty = EmptyText(Icons.Outlined.AccountTree, "No branches", "Push a branch to see it here."),
      ) {
        items(state.branches.data, key = { it.name }) { branch ->
          BranchRow(branch, onClick = { onBrowseRef(branch.name) })
          GlDivider(inset = GlSpace.margin)
        }
      }
      ProjectSection.TAGS -> Feed(
        feed = state.tags,
        visibleEmpty = state.tags.data.isEmpty(),
        onRetry = retry,
        empty = EmptyText(Icons.Outlined.LocalOffer, "No tags yet", "Tags created in this project appear here."),
      ) {
        items(state.tags.data, key = { it.name }) { tag ->
          TagRow(tag, onClick = { onBrowseRef(tag.name) })
          GlDivider(inset = GlSpace.margin)
        }
      }
      ProjectSection.RELEASES -> Feed(
        feed = state.releases,
        visibleEmpty = state.releases.data.isEmpty(),
        onRetry = retry,
        empty = EmptyText(Icons.Outlined.NewReleases, "No releases yet", "Releases published for this project appear here."),
      ) {
        items(state.releases.data, key = { it.tagName }) { release ->
          ReleaseRow(release, onClick = { onOpenRelease(release.tagName) })
          GlDivider(inset = GlSpace.margin)
        }
      }
      ProjectSection.CONTRIBUTORS -> Feed(
        feed = state.contributors,
        visibleEmpty = state.contributors.data.isEmpty(),
        onRetry = retry,
        empty = EmptyText(Icons.Outlined.People, "No contributors to show", "This server doesn't share contributor stats for this project."),
      ) {
        items(state.contributors.data, key = { it.name + it.commits }) { person ->
          ContributorRow(person)
          GlDivider(inset = 64.dp)
        }
      }
      ProjectSection.ACTIVITY -> Feed(
        feed = state.events,
        visibleEmpty = state.events.data.isEmpty(),
        onRetry = retry,
        empty = EmptyText(Icons.Outlined.History, "No recent activity", "Pushes, comments and changes in this project appear here."),
      ) {
        items(state.events.data, key = { it.id }) { event ->
          val target = event.destination()
          EventRow(event = event, onClick = target?.let { { onNavigate(it) } })
          GlDivider(inset = 60.dp)
        }
      }
    }
  }
}

private data class EmptyText(val icon: ImageVector, val title: String, val body: String)

/** Shared loading, error and empty handling for every section. */
@Composable
private fun <T> Feed(
  feed: Loadable<List<T>>,
  visibleEmpty: Boolean,
  onRetry: () -> Unit,
  empty: EmptyText,
  spaced: Boolean = false,
  content: LazyListScope.() -> Unit,
) {
  val error = feed.error
  when {
    error != null && feed.data.isEmpty() -> FullScreenState { ErrorState(message = error, onRetry = onRetry) }
    feed.initialLoading || (!feed.loaded && error == null) -> FullScreenState { LoadingState() }
    visibleEmpty -> FullScreenState { EmptyState(icon = empty.icon, title = empty.title, body = empty.body) }
    else -> LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(top = if (spaced) 8.dp else 0.dp, bottom = GlSpace.group),
      verticalArrangement = if (spaced) Arrangement.spacedBy(12.dp) else Arrangement.Top,
    ) {
      if (error != null) {
        item(key = "error") {
          InlineError(message = error, onRetry = onRetry, modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = 4.dp))
        }
      }
      content()
      item(key = "bottom") { Spacer(Modifier.navigationBarsPadding()) }
    }
  }
}

private fun matchesIssue(issue: GitLabIssue, filter: SectionIssueFilter) = when (filter) {
  SectionIssueFilter.OPEN -> issue.state == "opened"
  SectionIssueFilter.CLOSED -> issue.state == "closed"
  SectionIssueFilter.ALL -> true
}

private fun countIssues(all: List<GitLabIssue>, filter: SectionIssueFilter) = all.count { matchesIssue(it, filter) }

private fun matchesMr(mr: GitLabMergeRequest, filter: SectionMrFilter) = when (filter) {
  SectionMrFilter.OPEN -> mr.state == "opened"
  SectionMrFilter.MERGED -> mr.state == "merged"
  SectionMrFilter.CLOSED -> mr.state == "closed"
  SectionMrFilter.ALL -> true
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BranchRow(branch: GitLabBranch, onClick: () -> Unit) {
  val c = Gl.colors
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Browse ${branch.name}")
      .defaultMinSize(minHeight = 56.dp)
      .padding(horizontal = GlSpace.margin, vertical = 10.dp),
  ) {
    Text(branch.name, style = Gl.type.mono, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    val badges = listOfNotNull(
      "Default".takeIf { branch.default },
      "Protected".takeIf { branch.protected },
      "Merged".takeIf { branch.merged },
    )
    if (badges.isNotEmpty()) {
      Spacer(Modifier.height(4.dp))
      FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        badges.forEach { GlBadge(it, tone = if (it == "Merged") Tone.Merged else Tone.Neutral) }
      }
    }
    branch.commit?.let { commit ->
      Spacer(Modifier.height(4.dp))
      Text(
        listOfNotNull(commit.title.takeIf { it.isNotBlank() }, commit.committedDate?.takeIf { it.isNotBlank() }).joinToString(" · "),
        style = Gl.type.meta,
        color = c.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun TagRow(tag: GitLabTag, onClick: () -> Unit) {
  val c = Gl.colors
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Browse ${tag.name}")
      .defaultMinSize(minHeight = 56.dp)
      .padding(horizontal = GlSpace.margin, vertical = 10.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(tag.name, style = Gl.type.mono, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
      if (tag.release != null) {
        Spacer(Modifier.width(8.dp))
        GlBadge("Release", tone = Tone.Info)
      }
    }
    tag.commit?.let { commit ->
      Spacer(Modifier.height(4.dp))
      Text(
        listOfNotNull(commit.title.takeIf { it.isNotBlank() }, commit.committedDate?.takeIf { it.isNotBlank() }).joinToString(" · "),
        style = Gl.type.meta,
        color = c.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun ReleaseRow(release: GitLabRelease, onClick: () -> Unit) {
  val c = Gl.colors
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open release")
      .defaultMinSize(minHeight = 64.dp)
      .padding(horizontal = GlSpace.margin, vertical = 10.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        release.name?.takeIf { it.isNotBlank() } ?: release.tagName,
        style = Gl.type.rowTitle,
        color = c.textPrimary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f, fill = false),
      )
      if (release.upcoming) {
        Spacer(Modifier.width(8.dp))
        GlBadge("Upcoming", tone = Tone.Warning)
      }
    }
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      MonoPill(release.tagName)
      val meta = listOfNotNull(
        release.releasedAt?.takeIf { it.isNotBlank() },
        release.author?.let { it.name.ifBlank { it.username } }?.takeIf { it.isNotBlank() }?.let { "by $it" },
      ).joinToString(" · ")
      if (meta.isNotBlank()) {
        Spacer(Modifier.width(8.dp))
        Text(meta, style = Gl.type.meta, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
  }
}

@Composable
private fun ContributorRow(person: GitLabContributor) {
  val c = Gl.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .semantics(mergeDescendants = true) {
        contentDescription = "${person.name}, ${person.commits} commits, ${person.additions} additions, ${person.deletions} deletions"
      }
      .defaultMinSize(minHeight = 56.dp)
      .padding(horizontal = GlSpace.margin, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Contributor stats come without avatars; emails are deliberately not shown.
    Avatar(url = null, name = person.name, size = 36.dp)
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(person.name.ifBlank { "Unknown" }, style = Gl.type.rowTitle, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(
        buildAnnotatedString {
          withStyle(androidx.compose.ui.text.SpanStyle(color = c.success)) { append("+${person.additions}") }
          append("  ")
          withStyle(androidx.compose.ui.text.SpanStyle(color = c.danger)) { append("−${person.deletions}") }
        },
        style = Gl.type.meta.copy(fontFeatureSettings = "tnum"),
        color = c.textSecondary,
      )
    }
    Text(
      if (person.commits == 1) "1 commit" else "${person.commits} commits",
      style = Gl.type.numeric,
      color = c.textSecondary,
    )
  }
}
