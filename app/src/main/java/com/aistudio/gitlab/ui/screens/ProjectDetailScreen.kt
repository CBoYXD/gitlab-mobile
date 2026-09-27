package com.aistudio.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.CallMerge
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aistudio.gitlab.data.model.GitLabProject
import com.aistudio.gitlab.ui.components.EmptyState
import androidx.compose.material.icons.outlined.CallSplit
import com.aistudio.gitlab.ui.components.InlineStat
import com.aistudio.gitlab.ui.components.formatCount
import com.aistudio.gitlab.ui.components.ErrorState
import com.aistudio.gitlab.ui.components.FullScreenState
import com.aistudio.gitlab.ui.components.GlBadge
import com.aistudio.gitlab.ui.components.GlCard
import com.aistudio.gitlab.ui.components.GlDivider
import com.aistudio.gitlab.ui.components.GlIconAction
import com.aistudio.gitlab.ui.components.GlRow
import com.aistudio.gitlab.ui.components.GlTopBar
import com.aistudio.gitlab.ui.components.IconTile
import com.aistudio.gitlab.ui.components.InlineError
import com.aistudio.gitlab.ui.components.LanguageBar
import com.aistudio.gitlab.ui.components.LoadingState
import com.aistudio.gitlab.ui.components.MarkdownContext
import com.aistudio.gitlab.ui.components.MarkdownText
import com.aistudio.gitlab.ui.components.MonoPill
import com.aistudio.gitlab.ui.components.PERSONAL_IMAGE_TAG
import com.aistudio.gitlab.ui.components.StatBlock
import com.aistudio.gitlab.ui.components.SwapIcon
import com.aistudio.gitlab.ui.components.Tone
import com.aistudio.gitlab.ui.components.rememberImageModel
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlRadius
import com.aistudio.gitlab.ui.theme.GlSpace
import com.aistudio.gitlab.ui.viewmodel.ProjectDetailState
import com.aistudio.gitlab.ui.viewmodel.ProjectSection

/** A project's page, laid out like a GitHub Mobile repository: header, lists, code, README. */
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
  @Suppress("UNUSED_PARAMETER") onOpenRelease: (String) -> Unit,
  onOpenInBrowser: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val project = state.project.data
  Column(modifier = modifier.fillMaxSize().background(Gl.colors.bg)) {
    GlTopBar(
      title = project?.name ?: "Project",
      subtitle = project?.pathWithNamespace,
      onBack = onBack,
      actions = {
        if (project != null && canWrite) {
          IconButton(onClick = { onToggleStar(project.id) }, enabled = !pendingStar) {
            SwapIcon(
              target = project.isStarred,
              on = Icons.Outlined.Star,
              off = Icons.Outlined.StarOutline,
              contentDescription = if (project.isStarred) "Unstar project" else "Star project",
              tintOn = Gl.colors.textPrimary,
              tintOff = Gl.colors.textPrimary,
            )
          }
        }
        val web = project?.webUrl
        if (!web.isNullOrBlank()) {
          GlIconAction(Icons.AutoMirrored.Outlined.OpenInNew, "Open in browser", onClick = { onOpenInBrowser(web) })
        }
      },
    )

    if (project == null) {
      FullScreenState {
        val error = state.project.error
        if (error != null) ErrorState(message = error, onRetry = onRetry) else LoadingState(label = "Loading project")
      }
    } else {
      ProjectPage(
        project = project,
        state = state,
        markdown = markdown,
        onRetry = onRetry,
        onOpenSection = onOpenSection,
        onOpenCode = onOpenCode,
      )
    }
  }
}

@Composable
private fun ProjectPage(
  project: GitLabProject,
  state: ProjectDetailState,
  markdown: MarkdownContext,
  onRetry: () -> Unit,
  onOpenSection: (ProjectSection) -> Unit,
  onOpenCode: () -> Unit,
) {
  LazyColumn(
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

    item(key = "header") { ProjectHeader(project) }

    item(key = "stats") {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = GlSpace.margin, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        InlineStat(Icons.Outlined.StarOutline, project.starCount, "star", "stars")
        InlineStat(Icons.Outlined.CallSplit, project.forksCount, "fork", "forks")
        val openIssues = project.openIssuesCount
        if (project.issuesEnabled != false && openIssues != null) {
          InlineStat(Icons.Outlined.Adjust, openIssues, "open issue", "open issues")
        }
      }
    }

    val languages = state.languages
    if (languages.initialLoading) {
      item(key = "languages-loading") {
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Gl.colors.textSecondary)
        }
      }
    } else if (languages.error == null && languages.data.isNotEmpty()) {
      item(key = "languages") {
        GlCard(
          modifier = Modifier.padding(horizontal = GlSpace.margin),
          contentPadding = PaddingValues(GlSpace.margin),
        ) {
          Text("Languages", style = Gl.type.label, color = Gl.colors.textPrimary, modifier = Modifier.semantics { heading() })
          Spacer(Modifier.height(10.dp))
          LanguageBar(languages.data)
        }
      }
    }

    item(key = "sections") {
      Spacer(Modifier.height(GlSpace.group))
      GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin)) {
        val rows = buildList {
          if (project.issuesEnabled != false) add(SectionRow(ProjectSection.ISSUES, Icons.Outlined.Adjust, project.openIssuesCount))
          if (project.mergeRequestsEnabled != false) add(SectionRow(ProjectSection.MERGE_REQUESTS, Icons.Outlined.CallMerge, null))
          if (project.jobsEnabled != false) add(SectionRow(ProjectSection.PIPELINES, Icons.Outlined.PlayCircleOutline, null))
          add(SectionRow(ProjectSection.COMMITS, Icons.Outlined.Commit, null))
          add(SectionRow(ProjectSection.BRANCHES, Icons.Outlined.AccountTree, state.branches.takeIf { it.loaded && it.error == null }?.data?.size))
          add(SectionRow(ProjectSection.TAGS, Icons.Outlined.LocalOffer, state.tags.takeIf { it.loaded && it.error == null }?.data?.size))
          add(SectionRow(ProjectSection.RELEASES, Icons.Outlined.NewReleases, state.releases.takeIf { it.loaded && it.error == null }?.data?.size))
          add(SectionRow(ProjectSection.CONTRIBUTORS, Icons.Outlined.People, null))
          add(SectionRow(ProjectSection.ACTIVITY, Icons.Outlined.History, null))
        }
        rows.forEachIndexed { index, row ->
          if (index > 0) GlDivider(inset = 64.dp)
          NavigationRow(
            icon = row.icon,
            title = row.section.title,
            count = row.count,
            onClick = { onOpenSection(row.section) },
          )
        }
      }
    }

    item(key = "code") {
      Spacer(Modifier.height(GlSpace.group))
      GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin)) {
        GlRow(
          title = "Browse code",
          leading = { IconTile(Icons.Outlined.Code) },
          trailing = {
            state.effectiveRef?.let { MonoPill(it, Modifier.padding(end = 4.dp)) }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Gl.colors.textTertiary)
          },
          onClick = onOpenCode,
          onClickLabel = "Browse code",
        )
      }
    }

    item(key = "readme-header") {
      Text(
        text = "README.md",
        style = Gl.type.sectionTitle,
        color = Gl.colors.textPrimary,
        modifier = Modifier
          .padding(start = GlSpace.margin, end = GlSpace.margin, top = GlSpace.group, bottom = GlSpace.tight)
          .semantics { heading() },
      )
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
        readmeText.isNullOrBlank() -> GlCard(
          modifier = Modifier.padding(horizontal = GlSpace.margin),
          contentPadding = PaddingValues(GlSpace.margin),
        ) {
          Text("This project has no README.", style = Gl.type.bodySmall, color = Gl.colors.textSecondary)
        }
        else -> GlCard(
          modifier = Modifier.padding(horizontal = GlSpace.margin),
          contentPadding = PaddingValues(GlSpace.margin),
        ) {
          MarkdownText(readmeText, context = markdown)
        }
      }
    }

    item(key = "bottom") { Spacer(Modifier.navigationBarsPadding()) }
  }
}

private data class SectionRow(val section: ProjectSection, val icon: ImageVector, val count: Int?)

@Composable
private fun NavigationRow(icon: ImageVector, title: String, count: Int?, onClick: () -> Unit) {
  GlRow(
    title = title,
    leading = { IconTile(icon) },
    trailing = {
      if (count != null) {
        Text(count.toString(), style = Gl.type.numeric, color = Gl.colors.textSecondary)
        Spacer(Modifier.width(4.dp))
      }
      Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Gl.colors.textTertiary)
    },
    onClick = onClick,
    onClickLabel = "Open $title",
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectHeader(project: GitLabProject) {
  val c = Gl.colors
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = GlSpace.margin, vertical = 8.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      ProjectAvatar(project)
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        val namespace = project.namespace?.name?.takeIf { it.isNotBlank() }
          ?: project.nameWithNamespace.substringBeforeLast("/", "").trim().takeIf { it.isNotBlank() }
        if (namespace != null) {
          Text(namespace, style = Gl.type.meta, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
          project.name,
          style = Gl.type.title,
          color = c.textPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.semantics { heading() },
        )
      }
    }
    val description = project.description
    if (!description.isNullOrBlank()) {
      Spacer(Modifier.height(10.dp))
      Text(description, style = Gl.type.body, color = c.textPrimary, maxLines = 4, overflow = TextOverflow.Ellipsis)
    }
    val badges = buildList {
      if (project.archived) add("Archived" to Tone.Warning)
      project.forkedFrom?.let { add("Fork of ${it.pathWithNamespace.ifBlank { it.name }}" to Tone.Neutral) }
      project.license?.let { license -> (license.nickname ?: license.name).takeIf { it.isNotBlank() }?.let { add(it to Tone.Neutral) } }
      if (project.visibility.isNotBlank()) add(project.visibility.replaceFirstChar { it.uppercase() } to Tone.Neutral)
    }
    if (badges.isNotEmpty() || project.topics.isNotEmpty()) {
      Spacer(Modifier.height(10.dp))
      FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        badges.forEach { (text, tone) -> GlBadge(text = text, tone = tone) }
        project.topics.forEach { GlBadge(text = it) }
      }
    }
  }
}

/** Project avatar, 48dp; the initial sits underneath so a private or failed image still reads. */
@Composable
private fun ProjectAvatar(project: GitLabProject) {
  val c = Gl.colors
  val shape = RoundedCornerShape(GlRadius.control)
  Box(
    modifier = Modifier
      .testTag(PERSONAL_IMAGE_TAG)
      .size(48.dp)
      .clip(shape)
      .background(c.surface2)
      .border(1.dp, c.imageOutline, shape),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = project.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
      style = Gl.type.sectionTitle,
      color = c.textSecondary,
    )
    if (!project.avatarUrl.isNullOrBlank()) {
      AsyncImage(
        model = rememberImageModel(project.avatarUrl),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}
