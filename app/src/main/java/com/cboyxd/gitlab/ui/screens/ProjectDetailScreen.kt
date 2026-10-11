package com.cboyxd.gitlab.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import com.cboyxd.gitlab.ui.components.LoadingPlaceholder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.data.model.GitLabProject
import com.cboyxd.gitlab.data.model.GitLabRelease
import com.cboyxd.gitlab.ui.components.ColorTile
import com.cboyxd.gitlab.ui.components.EmptyState
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.FullScreenState
import com.cboyxd.gitlab.ui.components.GlBadge
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlIconAction
import com.cboyxd.gitlab.ui.components.GlRow
import com.cboyxd.gitlab.ui.components.GlSectionBand
import com.cboyxd.gitlab.ui.components.GlTopBar
import com.cboyxd.gitlab.ui.components.InlineError
import com.cboyxd.gitlab.ui.components.LanguageBar
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.components.MarkdownContext
import com.cboyxd.gitlab.ui.components.MarkdownText
import com.cboyxd.gitlab.ui.components.PERSONAL_IMAGE_TAG
import com.cboyxd.gitlab.ui.components.SwapIcon
import com.cboyxd.gitlab.ui.components.Tone
import com.cboyxd.gitlab.ui.components.formatCompact
import com.cboyxd.gitlab.ui.components.pressable
import com.cboyxd.gitlab.ui.components.AvatarImage
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.theme.GlTile
import com.cboyxd.gitlab.ui.viewmodel.ProjectDetailState
import com.cboyxd.gitlab.ui.viewmodel.ProjectSection

/**
 * A project's page, laid out like a GitHub Mobile repository: a dark header with the name, stats
 * and actions; colored navigation rows; the current branch with Code and Commits; the README.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
  state: ProjectDetailState,
  pendingStar: Boolean,
  canWrite: Boolean,
  markdown: MarkdownContext,
  onBack: () -> Unit,
  onRetry: () -> Unit,
  onToggleStar: (Long) -> Unit,
  onOpenSection: (ProjectSection) -> Unit,
  onOpenCode: () -> Unit,
  onOpenRelease: (String) -> Unit,
  onOpenInBrowser: (String) -> Unit,
  modifier: Modifier = Modifier,
  onChangeRef: (String?) -> Unit = {},
  onLoadRefs: () -> Unit = {},
  onFilterRefs: (String) -> Unit = {},
  onRefresh: () -> Unit = onRetry,
) {
  val project = state.project.data
  val context = LocalContext.current
  val listState = rememberLazyListState()
  // The bar takes the name once the page's own big heading has scrolled out of view.
  val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 1 || (listState.firstVisibleItemIndex == 1 && listState.firstVisibleItemScrollOffset > 120) } }
  var showRefs by rememberSaveable { mutableStateOf(false) }
  var menuOpen by remember { mutableStateOf(false) }
  val c = Gl.colors

  Column(modifier = modifier.fillMaxSize().background(c.bg)) {
    GlTopBar(
      title = project?.name ?: "Project",
      overline = project?.let { namespaceOf(it) },
      showTitle = collapsed || project == null,
      background = if (collapsed || project == null) c.bg else c.sunken,
      onBack = onBack,
      actions = {
        val web = project?.webUrl
        if (!web.isNullOrBlank()) {
          GlIconAction(Icons.Outlined.Share, "Share", onClick = { shareUrl(context, project.name, web) })
          Box {
            GlIconAction(Icons.Outlined.MoreVert, "More options", onClick = { menuOpen = true })
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = c.surface) {
              DropdownMenuItem(
                text = { Text("Open in browser", style = Gl.type.body, color = c.textPrimary) },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = c.textSecondary) },
                onClick = { menuOpen = false; onOpenInBrowser(web) },
              )
              DropdownMenuItem(
                text = { Text("Activity", style = Gl.type.body, color = c.textPrimary) },
                leadingIcon = { Icon(Icons.Outlined.History, null, tint = c.textSecondary) },
                onClick = { menuOpen = false; onOpenSection(ProjectSection.ACTIVITY) },
              )
            }
          }
        }
      },
    )

    if (project == null) {
      FullScreenState {
        val error = state.project.error
        if (error != null) ErrorState(message = error, onRetry = onRetry) else LoadingState(label = "Loading project")
      }
    } else {
      PullToRefreshBox(
        // Only a refresh of a page already on screen; the first load has its own spinner.
        isRefreshing = state.project.loading && state.project.loaded,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
      ) {
      ProjectPage(
        project = project,
        state = state,
        listState = listState,
        canWrite = canWrite,
        pendingStar = pendingStar,
        markdown = markdown,
        onRetry = onRetry,
        onToggleStar = { onToggleStar(project.id) },
        onOpenSection = onOpenSection,
        onOpenCode = onOpenCode,
        onOpenRelease = onOpenRelease,
        onOpenInBrowser = onOpenInBrowser,
        onChangeBranch = { showRefs = true },
      )
      }
    }
  }

  if (showRefs && project != null) {
    RefPickerSheet(
      state = state,
      onLoadRefs = onLoadRefs,
      onFilter = onFilterRefs,
      onDismiss = {
        showRefs = false
        onFilterRefs("")
      },
      onSelect = { selected ->
        showRefs = false
        onFilterRefs("")
        onChangeRef(selected.takeIf { it != project.defaultBranch })
      },
    )
  }
}

@Composable
private fun ProjectPage(
  project: GitLabProject,
  state: ProjectDetailState,
  listState: androidx.compose.foundation.lazy.LazyListState,
  canWrite: Boolean,
  pendingStar: Boolean,
  markdown: MarkdownContext,
  onRetry: () -> Unit,
  onToggleStar: () -> Unit,
  onOpenSection: (ProjectSection) -> Unit,
  onOpenCode: () -> Unit,
  onOpenRelease: (String) -> Unit,
  onOpenInBrowser: (String) -> Unit,
  onChangeBranch: () -> Unit,
) {
  val c = Gl.colors
  LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize().testTag("project_detail_list"),
    contentPadding = PaddingValues(bottom = GlSpace.group),
  ) {
    val refreshError = state.project.error
    if (refreshError != null) {
      item(key = "refresh-error") {
        InlineError(
          message = refreshError,
          onRetry = onRetry,
          modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = 4.dp),
        )
      }
    }

    item(key = "header") {
      ProjectHeader(
        project = project,
        state = state,
        canWrite = canWrite,
        pendingStar = pendingStar,
        onToggleStar = onToggleStar,
        onOpenInBrowser = onOpenInBrowser,
      )
      GlDivider()
    }

    item(key = "sections") {
      Column(Modifier.padding(vertical = 4.dp)) {
        // Exact counts once they arrive; the project's cached counter only as a stand-in until then.
        val counts = state.counts.data
        if (project.issuesEnabled != false) {
          val issues = counts?.openIssues ?: project.openIssuesCount.takeIf { !state.counts.loaded }
          NavRow(Icons.Outlined.Adjust, GlTile.green, "Issues", issues?.let(::formatCompact), loading = issues == null && state.counts.loading) {
            onOpenSection(ProjectSection.ISSUES)
          }
        }
        if (project.mergeRequestsEnabled != false) {
          val mergeRequests = counts?.openMergeRequests
          NavRow(Icons.Outlined.MergeType, GlTile.blue, "Merge requests", mergeRequests?.let(::formatCompact), loading = mergeRequests == null && state.counts.loading) {
            onOpenSection(ProjectSection.MERGE_REQUESTS)
          }
        }
        if (project.jobsEnabled != false) {
          NavRow(Icons.Outlined.PlayCircleOutline, GlTile.yellow, "Pipelines", null) { onOpenSection(ProjectSection.PIPELINES) }
        }
        val releases = state.releases
        val releaseCount = releases.data.size.takeIf { releases.loaded && releases.error == null }
        // A full page (20) means there are more on the server.
        NavRow(Icons.Outlined.Sell, GlTile.gray, "Releases", releaseCount?.takeIf { it > 0 }?.let { if (it >= 20) "$it+" else it.toString() }) {
          onOpenSection(ProjectSection.RELEASES)
        }
        releases.data.firstOrNull { !it.upcoming }?.let { latest ->
          LatestReleaseCard(latest, onClick = { onOpenRelease(latest.tagName) })
        }
        val contributors = state.contributors
        NavRow(
          Icons.Outlined.Group,
          GlTile.orange,
          "Contributors",
          contributors.data.size.takeIf { contributors.loaded && contributors.error == null && it > 0 }?.toString(),
        ) { onOpenSection(ProjectSection.CONTRIBUTORS) }
      }
    }

    item(key = "code") {
      GlDivider()
      Column(Modifier.padding(vertical = 4.dp)) {
        val ref = state.effectiveRef
        if (ref != null && !project.emptyRepo) {
          BranchRow(ref = ref, isTag = state.tags.data.any { it.name == ref } && state.branches.data.none { it.name == ref }, onChange = onChangeBranch)
        }
        NavRow(Icons.Outlined.Code, GlTile.slate, "Code", null, onClick = onOpenCode)
        NavRow(Icons.Outlined.Commit, GlTile.slate, "Commits", null) { onOpenSection(ProjectSection.COMMITS) }
        NavRow(Icons.Outlined.AccountTree, GlTile.slate, "Branches", state.branches.takeIf { it.loaded && it.error == null }?.data?.size?.toString()) {
          onOpenSection(ProjectSection.BRANCHES)
        }
        NavRow(Icons.Outlined.LocalOffer, GlTile.slate, "Tags", state.tags.takeIf { it.loaded && it.error == null }?.data?.size?.toString()) {
          onOpenSection(ProjectSection.TAGS)
        }
      }
    }

    val languages = state.languages.data
    if (state.languages.error == null && languages.isNotEmpty()) {
      item(key = "languages") {
        GlDivider()
        Column(Modifier.padding(horizontal = GlSpace.margin, vertical = GlSpace.margin)) {
          Text("Languages", style = Gl.type.label, color = c.textSecondary, modifier = Modifier.semantics { heading() })
          Spacer(Modifier.height(10.dp))
          LanguageBar(languages)
        }
      }
    }

    item(key = "readme-header") {
      GlDivider()
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 56.dp)
          .padding(horizontal = GlSpace.margin),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(
          text = project.readmeUrl?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "README.md",
          style = Gl.type.mono.copy(fontSize = Gl.type.body.fontSize),
          color = c.textSecondary,
          modifier = Modifier.semantics { heading() },
        )
      }
    }

    item(key = "readme") {
      val readme = state.readme
      val readmeText = readme.data
      val readmeError = readme.error
      when {
        project.emptyRepo || state.effectiveRef == null -> EmptyState(
          icon = Icons.Outlined.FolderOff,
          title = "Empty repository",
          body = "Push a commit to see files and the README here.",
        )
        readmeError != null && readmeText == null -> InlineError(
          message = readmeError,
          onRetry = onRetry,
          modifier = Modifier.padding(horizontal = GlSpace.margin),
        )
        readme.loading || !readme.loaded -> LoadingState(label = "Loading README")
        readmeText.isNullOrBlank() -> Text(
          "This project has no README.",
          style = Gl.type.bodySmall,
          color = c.textSecondary,
          modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = 8.dp),
        )
        else -> Box(Modifier.padding(horizontal = GlSpace.margin, vertical = 8.dp)) {
          MarkdownText(readmeText, context = markdown)
        }
      }
    }

    item(key = "bottom") { Spacer(Modifier.navigationBarsPadding()) }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectHeader(
  project: GitLabProject,
  state: ProjectDetailState,
  canWrite: Boolean,
  pendingStar: Boolean,
  onToggleStar: () -> Unit,
  onOpenInBrowser: (String) -> Unit,
) {
  val c = Gl.colors
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(c.sunken)
      .padding(start = GlSpace.margin, end = GlSpace.margin, top = 8.dp, bottom = 20.dp),
  ) {
    val namespace = namespaceOf(project)
    if (namespace.isNotBlank()) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        NamespaceAvatar(project)
        Spacer(Modifier.width(10.dp))
        Text(namespace, style = Gl.type.body, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Spacer(Modifier.height(10.dp))
    }
    Text(
      project.name,
      style = Gl.type.headline.copy(fontSize = Gl.type.headline.fontSize * 1.1f),
      color = c.textPrimary,
      maxLines = 3,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.semantics { heading() },
    )
    val description = project.description
    if (!description.isNullOrBlank()) {
      Spacer(Modifier.height(10.dp))
      Text(description, style = Gl.type.body, color = c.textPrimary, maxLines = 5, overflow = TextOverflow.Ellipsis)
    }

    Spacer(Modifier.height(14.dp))
    val visibility = project.visibility.ifBlank { "public" }
    HeaderLine(
      icon = when (visibility.lowercase()) {
        "private" -> Icons.Outlined.Lock
        "internal" -> Icons.Outlined.Shield
        else -> Icons.Outlined.Public
      },
      text = AnnotatedString(visibility.replaceFirstChar { it.uppercase() }),
    )
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      HeaderLine(Icons.Outlined.StarOutline, countLine(project.starCount, "star", "stars"))
      Spacer(Modifier.width(24.dp))
      HeaderLine(Icons.Outlined.CallSplit, countLine(project.forksCount, "fork", "forks"))
    }

    val badges = buildList {
      if (project.archived) add("Archived" to Tone.Warning)
      project.forkedFrom?.let { add("Fork of ${it.pathWithNamespace.ifBlank { it.name }}" to Tone.Neutral) }
      // "other" is GitLab's word for a license file it couldn't identify; its nickname is just "LICENSE".
      project.license?.takeIf { it.key != "other" }?.let { license -> (license.nickname ?: license.name).takeIf { it.isNotBlank() }?.let { add(it to Tone.Neutral) } }
    }
    if (badges.isNotEmpty() || project.topics.isNotEmpty()) {
      Spacer(Modifier.height(12.dp))
      FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        badges.forEach { (text, tone) -> GlBadge(text = text, tone = tone, icon = if (text == "Archived") Icons.Outlined.Archive else null) }
        project.topics.forEach { topic ->
          Text(
            topic,
            style = Gl.type.badge,
            color = c.accent,
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .background(c.accentBg)
              .padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }
    }

    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      HeaderButton(
        onClick = onToggleStar,
        enabled = canWrite && !pendingStar,
        contentDescription = when {
          !canWrite -> "Sign in to star"
          project.isStarred -> "Unstar project"
          else -> "Star project"
        },
      ) {
        SwapIcon(
          target = project.isStarred,
          on = Icons.Outlined.Star,
          off = Icons.Outlined.StarOutline,
          contentDescription = "",
          tintOn = Color(0xFFE9B825),
          tintOff = if (canWrite) c.textPrimary else c.textTertiary,
        )
      }
      HeaderButton(
        onClick = { onOpenInBrowser("${project.webUrl}/-/forks/new") },
        contentDescription = "Fork in browser",
        modifier = Modifier.weight(2.2f),
      ) {
        Icon(Icons.Outlined.CallSplit, contentDescription = null, tint = c.textPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Fork", style = Gl.type.label.copy(fontWeight = FontWeight.SemiBold), color = c.textPrimary)
      }
      val cloneUrl = project.httpUrlToRepo.ifBlank { "${project.webUrl}.git" }
      HeaderButton(
        onClick = {
          clipboard.setText(AnnotatedString(cloneUrl))
          Toast.makeText(context, "Clone URL copied", Toast.LENGTH_SHORT).show()
        },
        contentDescription = "Copy clone URL",
      ) {
        Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = c.textPrimary, modifier = Modifier.size(20.dp))
      }
      HeaderButton(onClick = { onOpenInBrowser(project.webUrl) }, contentDescription = "Open in browser") {
        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = c.textPrimary, modifier = Modifier.size(20.dp))
      }
    }
    if (state.languages.initialLoading) {
      // Keeps the header height stable while secondary data arrives.
      Spacer(Modifier.height(0.dp))
    }
  }
}

@Composable
private fun HeaderLine(icon: ImageVector, text: AnnotatedString) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) {}) {
    Icon(icon, contentDescription = null, tint = Gl.colors.textSecondary, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(12.dp))
    Text(text, style = Gl.type.body, color = Gl.colors.textSecondary)
  }
}

@Composable
private fun countLine(count: Int, singular: String, plural: String): AnnotatedString = buildAnnotatedString {
  withStyle(SpanStyle(color = Gl.colors.textPrimary, fontWeight = FontWeight.SemiBold)) { append(formatCompact(count)) }
  append(" ")
  append(if (count == 1) singular else plural)
}

@Composable
private fun RowScope.HeaderButton(
  onClick: () -> Unit,
  contentDescription: String,
  modifier: Modifier = Modifier.weight(1f),
  enabled: Boolean = true,
  content: @Composable RowScope.() -> Unit,
) {
  val c = Gl.colors
  val shape = RoundedCornerShape(8.dp)
  Row(
    modifier = modifier
      .height(52.dp)
      .clip(shape)
      .background(c.surface2)
      .border(1.dp, c.border, shape)
      .pressable(onClick = onClick, enabled = enabled)
      .semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
    content = content,
  )
}

@Composable
private fun NavRow(icon: ImageVector, tile: Color, title: String, count: String?, loading: Boolean = false, onClick: () -> Unit) {
  GlRow(
    title = title,
    leading = { ColorTile(icon, tile) },
    trailing = when {
      count != null -> { { Text(count, style = Gl.type.body.copy(fontFeatureSettings = "tnum"), color = Gl.colors.textSecondary) } }
      // A quiet placeholder so the count doesn't pop in beside an empty row.
      loading -> { { LoadingPlaceholder() } }
      else -> null
    },
    onClick = onClick,
    onClickLabel = "Open $title",
  )
}

/** The newest release under the Releases row, as GitHub shows it. */
@Composable
private fun LatestReleaseCard(release: GitLabRelease, onClick: () -> Unit) {
  val c = Gl.colors
  val shape = RoundedCornerShape(12.dp)
  Column(
    modifier = Modifier
      .padding(start = 64.dp, end = GlSpace.margin, top = 2.dp, bottom = 8.dp)
      .fillMaxWidth()
      .clip(shape)
      .background(c.surface)
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open release")
      .padding(horizontal = 16.dp, vertical = 12.dp),
  ) {
    Text(
      release.name?.takeIf { it.isNotBlank() } ?: release.tagName,
      style = Gl.type.rowTitle.copy(fontWeight = FontWeight.SemiBold),
      color = c.textPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      buildAnnotatedString {
        (release.releasedAt ?: release.createdAt)?.takeIf { it.isNotBlank() }?.let { append(it); append(" · ") }
        withStyle(SpanStyle(color = c.success)) { append("Latest") }
      },
      style = Gl.type.bodySmall,
      color = c.textSecondary,
    )
  }
}

@Composable
private fun BranchRow(ref: String, isTag: Boolean, onChange: () -> Unit) {
  val c = Gl.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 64.dp)
      .padding(start = GlSpace.margin, end = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
      Icon(if (isTag) Icons.Outlined.LocalOffer else Icons.Outlined.AccountTree, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(22.dp))
    }
    Spacer(Modifier.width(16.dp))
    Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
      Text(if (isTag) "Current tag" else "Current branch", style = Gl.type.bodySmall, color = c.textSecondary)
      Text(ref, style = Gl.type.rowTitle, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    TextButton(onClick = onChange) {
      Text("Change", style = Gl.type.label.copy(fontWeight = FontWeight.SemiBold), color = c.accent)
    }
  }
}

/** Owner avatar in the header: the namespace's image, else the project's, else an initial. */
@Composable
private fun NamespaceAvatar(project: GitLabProject) {
  val c = Gl.colors
  val shape = RoundedCornerShape(6.dp)
  // Group avatars come back as server-relative paths ("/uploads/..."); resolve them on the project's host.
  val origin = project.webUrl.substringBefore("://", "").let { scheme ->
    if (scheme.isEmpty()) "" else scheme + "://" + project.webUrl.substringAfter("://").substringBefore('/')
  }
  val url = project.namespace?.avatarUrl?.takeIf { it.isNotBlank() }
    ?.let { if (it.startsWith("/") && origin.isNotEmpty()) origin + it else it }
    ?: project.avatarUrl
  Box(
    modifier = Modifier
      .testTag(PERSONAL_IMAGE_TAG)
      .size(24.dp)
      .clip(shape)
      .background(c.surface3),
    contentAlignment = Alignment.Center,
  ) {
    Text(namespaceOf(project).firstOrNull()?.uppercase() ?: "?", style = Gl.type.badge, color = c.textSecondary)
    if (!url.isNullOrBlank()) {
      AvatarImage(url = url, modifier = Modifier.matchParentSize())
    }
  }
}

internal fun namespaceOf(project: GitLabProject): String =
  project.namespace?.name?.takeIf { it.isNotBlank() }
    ?: project.nameWithNamespace.substringBeforeLast("/", "").trim()

internal fun shareUrl(context: android.content.Context, title: String, url: String) {
  val send = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_SUBJECT, title)
    putExtra(Intent.EXTRA_TEXT, url)
  }
  runCatching { context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
