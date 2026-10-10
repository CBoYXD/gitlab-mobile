package com.cboyxd.gitlab.ui.screens

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import com.cboyxd.gitlab.ui.components.ListsDropdownFilter
import com.cboyxd.gitlab.ui.components.ListsFilterBar
import com.cboyxd.gitlab.ui.components.PipelineRow
import com.cboyxd.gitlab.ui.components.RefPill
import com.cboyxd.gitlab.ui.components.compactAge
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
import com.cboyxd.gitlab.data.model.GitLabBranch
import com.cboyxd.gitlab.data.model.GitLabContributor
import com.cboyxd.gitlab.data.model.GitLabIssue
import com.cboyxd.gitlab.data.model.GitLabLabel
import com.cboyxd.gitlab.data.model.GitLabMergeRequest
import com.cboyxd.gitlab.data.model.GitLabPipeline
import com.cboyxd.gitlab.data.model.GitLabRelease
import com.cboyxd.gitlab.data.model.GitLabTag
import com.cboyxd.gitlab.ui.components.Avatar
import com.cboyxd.gitlab.ui.components.CommitRow
import com.cboyxd.gitlab.ui.components.EmptyState
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.EventRow
import com.cboyxd.gitlab.ui.components.FullScreenState
import com.cboyxd.gitlab.ui.components.GlBadge
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlFilterRow
import com.cboyxd.gitlab.ui.components.GlTopBar
import com.cboyxd.gitlab.ui.components.InlineError
import com.cboyxd.gitlab.ui.components.IssueRow
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.components.MergeRequestRow
import com.cboyxd.gitlab.ui.components.MonoPill
import com.cboyxd.gitlab.ui.components.PipelineCard
import com.cboyxd.gitlab.ui.components.Tone
import com.cboyxd.gitlab.ui.components.destination
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.Loadable
import com.cboyxd.gitlab.ui.viewmodel.ProjectDetailState
import com.cboyxd.gitlab.ui.viewmodel.ProjectSection
import com.cboyxd.gitlab.ui.viewmodel.ScreenDestination

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
    GlTopBar(title = section.title, overline = state.project.data?.nameWithNamespace, onBack = onBack)
    when (section) {
      ProjectSection.ISSUES -> {
        var filter by rememberSaveable { mutableStateOf(SectionIssueFilter.OPEN) }
        val all = state.issues.data
        ListsFilterBar {
          ListsDropdownFilter(
            name = "State",
            options = SectionIssueFilter.entries.toList(),
            selected = filter,
            label = { it.label },
            onSelect = { filter = it },
            countOf = { countIssues(all, it) },
          )
        }
        GlDivider()
        val visible = all.filter { matchesIssue(it, filter) }
        Feed(
          feed = state.issues,
          visibleEmpty = visible.isEmpty(),
          onRetry = retry,
          empty = EmptyText(Icons.Outlined.Adjust, "No ${filter.label.lowercase()} issues", "Issues in this project appear here."),
        ) {
          items(visible, key = { "${it.projectId}-${it.iid}" }) { issue ->
            IssueRow(issue = issue, onClick = { onOpenIssue(issue) }, labels = labels)
            GlDivider()
          }
        }
      }
      ProjectSection.MERGE_REQUESTS -> {
        var filter by rememberSaveable { mutableStateOf(SectionMrFilter.OPEN) }
        val all = state.mergeRequests.data
        ListsFilterBar {
          ListsDropdownFilter(
            name = "State",
            options = SectionMrFilter.entries.toList(),
            selected = filter,
            label = { it.label },
            onSelect = { filter = it },
            countOf = { f -> all.count { matchesMr(it, f) } },
          )
        }
        GlDivider()
        val visible = all.filter { matchesMr(it, filter) }
        Feed(
          feed = state.mergeRequests,
          visibleEmpty = visible.isEmpty(),
          onRetry = retry,
          empty = EmptyText(Icons.Outlined.CallMerge, "No ${filter.label.lowercase()} merge requests", "Merge requests in this project appear here."),
        ) {
          items(visible, key = { "${it.projectId}-${it.iid}" }) { mr ->
            MergeRequestRow(mr = mr, onClick = { onOpenMergeRequest(mr) }, labels = labels)
            GlDivider()
          }
        }
      }
      ProjectSection.PIPELINES -> Feed(
        feed = state.pipelines,
        visibleEmpty = state.pipelines.data.isEmpty(),
        onRetry = retry,
        empty = EmptyText(Icons.Outlined.PlayCircleOutline, "No pipelines yet", "Pipelines run for this project appear here."),
      ) {
        items(state.pipelines.data, key = { it.id }) { pipeline ->
          PipelineRow(
            pipeline = pipeline,
            onRetry = if (canWrite) onRetryPipeline else null,
            onClick = { onOpenPipeline(pipeline) },
          )
          GlDivider()
        }
      }
      ProjectSection.COMMITS -> {
        state.effectiveRef?.let { ref ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = GlSpace.margin, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Outlined.AccountTree, contentDescription = null, tint = Gl.colors.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            RefPill(ref)
          }
          GlDivider()
        }
        Feed(
          feed = state.commits,
          visibleEmpty = state.commits.data.isEmpty(),
          onRetry = retry,
          empty = EmptyText(Icons.Outlined.Commit, "No commits yet", "Commits on this branch appear here."),
        ) {
          items(state.commits.data, key = { it.id }) { commit ->
            CommitRow(commit = commit, onClick = { onOpenCommit(commit.id) })
            GlDivider()
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
          GlDivider()
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
          GlDivider()
        }
      }
      ProjectSection.RELEASES -> Feed(
        feed = state.releases,
        visibleEmpty = state.releases.data.isEmpty(),
        onRetry = retry,
        empty = EmptyText(Icons.Outlined.NewReleases, "No releases yet", "Releases published for this project appear here."),
      ) {
        // GitLab lists releases newest first; the first one that isn't upcoming is the latest.
        val latest = state.releases.data.firstOrNull { !it.upcoming }?.tagName
        items(state.releases.data, key = { it.tagName }) { release ->
          ReleaseRow(release, latest = release.tagName == latest, onClick = { onOpenRelease(release.tagName) })
          GlDivider()
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
          GlDivider(inset = 68.dp)
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
          GlDivider(inset = 64.dp)
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

/** Branch as GitHub lists them: the name as a blue monospace chip, its badges, then the last commit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BranchRow(branch: GitLabBranch, onClick: () -> Unit) {
  val c = Gl.colors
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Browse ${branch.name}")
      .defaultMinSize(minHeight = 64.dp)
      .padding(horizontal = GlSpace.margin, vertical = 12.dp),
  ) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      RefPill(branch.name, Modifier.align(Alignment.CenterVertically))
      if (branch.default) GlBadge("Default", modifier = Modifier.align(Alignment.CenterVertically))
      if (branch.protected) GlBadge("Protected", tone = Tone.Warning, modifier = Modifier.align(Alignment.CenterVertically))
      if (branch.merged) GlBadge("Merged", tone = Tone.Merged, modifier = Modifier.align(Alignment.CenterVertically))
    }
    branch.commit?.let { commit -> CommitLine(commit.title, commit.committedDate ?: commit.createdAt) }
  }
}

@Composable
private fun TagRow(tag: GitLabTag, onClick: () -> Unit) {
  val c = Gl.colors
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Browse ${tag.name}")
      .defaultMinSize(minHeight = 64.dp)
      .padding(horizontal = GlSpace.margin, vertical = 12.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(8.dp))
      RefPill(tag.name, Modifier.weight(1f, fill = false))
      if (tag.release != null) {
        Spacer(Modifier.width(8.dp))
        GlBadge("Release", tone = Tone.Info)
      }
      if (tag.protected) {
        Spacer(Modifier.width(6.dp))
        GlBadge("Protected", tone = Tone.Warning)
      }
    }
    tag.commit?.let { commit -> CommitLine(commit.title, commit.committedDate ?: commit.createdAt) }
  }
}

/** Last commit under a ref: its title, with when it landed on the right. */
@Composable
private fun CommitLine(title: String, time: String?) {
  val c = Gl.colors
  Spacer(Modifier.height(6.dp))
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(title, style = Gl.type.bodySmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    val age = compactAge(time.orEmpty())
    if (age.isNotBlank()) {
      Spacer(Modifier.width(12.dp))
      Text(age, style = Gl.type.bodySmall, color = c.textSecondary, maxLines = 1)
    }
  }
}

/** Release: its name in bold, then "3d · Latest" as GitHub shows it, and the tag. */
@Composable
private fun ReleaseRow(release: GitLabRelease, latest: Boolean, onClick: () -> Unit) {
  val c = Gl.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open release")
      .defaultMinSize(minHeight = 72.dp)
      .padding(horizontal = GlSpace.margin, vertical = 12.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = if (latest) c.success else c.textSecondary, modifier = Modifier.padding(top = 2.dp).size(22.dp))
    Spacer(Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        release.name?.takeIf { it.isNotBlank() } ?: release.tagName,
        style = Gl.type.rowTitle.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
        color = c.textPrimary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.height(2.dp))
      Text(
        buildAnnotatedString {
          val parts = listOfNotNull(
            compactAge(release.releasedAt.orEmpty()).takeIf { it.isNotBlank() },
            release.author?.let { it.name.ifBlank { it.username } }?.takeIf { it.isNotBlank() },
          )
          append(parts.joinToString(" · "))
          when {
            release.upcoming -> {
              if (parts.isNotEmpty()) append(" · ")
              withStyle(androidx.compose.ui.text.SpanStyle(color = c.warning)) { append("Upcoming") }
            }
            latest -> {
              if (parts.isNotEmpty()) append(" · ")
              withStyle(androidx.compose.ui.text.SpanStyle(color = c.success)) { append("Latest") }
            }
          }
        },
        style = Gl.type.bodySmall,
        color = c.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.height(6.dp))
      RefPill(release.tagName)
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
    Avatar(url = person.avatarUrl, name = person.name, size = 36.dp)
    Spacer(Modifier.width(16.dp))
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
