package com.aistudio.gitlab.ui.screens

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aistudio.gitlab.data.model.GitLabFileDiff
import com.aistudio.gitlab.data.model.GitLabIssue
import com.aistudio.gitlab.data.model.GitLabLabel
import com.aistudio.gitlab.data.model.GitLabMergeRequest
import com.aistudio.gitlab.data.model.GitLabNote
import com.aistudio.gitlab.data.model.GitLabPipeline
import com.aistudio.gitlab.data.model.GitLabUser
import com.aistudio.gitlab.ui.components.Avatar
import com.aistudio.gitlab.ui.components.CommitRow
import com.aistudio.gitlab.ui.components.DiffViewer
import com.aistudio.gitlab.ui.components.EmptyState
import com.aistudio.gitlab.ui.components.ErrorState
import com.aistudio.gitlab.ui.components.FullScreenState
import com.aistudio.gitlab.ui.components.GlBadge
import com.aistudio.gitlab.ui.components.GlButton
import com.aistudio.gitlab.ui.components.GlButtonKind
import com.aistudio.gitlab.ui.components.GlCard
import com.aistudio.gitlab.ui.components.GlChip
import com.aistudio.gitlab.ui.components.GlDivider
import com.aistudio.gitlab.ui.components.GlFilterRow
import com.aistudio.gitlab.ui.components.GlIconAction
import com.aistudio.gitlab.ui.components.GlSectionHeader
import com.aistudio.gitlab.ui.components.GlTopBar
import com.aistudio.gitlab.ui.components.InlineError
import com.aistudio.gitlab.ui.components.LabelRow
import com.aistudio.gitlab.ui.components.LazyCodeView
import com.aistudio.gitlab.ui.components.LoadingState
import com.aistudio.gitlab.ui.components.MarkdownContext
import com.aistudio.gitlab.ui.components.MarkdownEditor
import com.aistudio.gitlab.ui.components.MarkdownText
import com.aistudio.gitlab.ui.components.MonoPill
import com.aistudio.gitlab.ui.components.PipelineCard
import com.aistudio.gitlab.ui.components.PipelineStatusBadge
import com.aistudio.gitlab.ui.components.StateBadge
import com.aistudio.gitlab.ui.components.Tone
import com.aistudio.gitlab.ui.components.issueStateStyle
import com.aistudio.gitlab.ui.components.mergeRequestStateStyle
import com.aistudio.gitlab.ui.components.rememberImageModel
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlRadius
import com.aistudio.gitlab.ui.theme.GlSpace
import com.aistudio.gitlab.ui.viewmodel.FileState
import com.aistudio.gitlab.ui.viewmodel.IssueDetailState
import com.aistudio.gitlab.ui.viewmodel.Loadable
import com.aistudio.gitlab.ui.viewmodel.MergeRequestDetailState

// ---------------------------------------------------------------------------------------------
// File viewer
// ---------------------------------------------------------------------------------------------

private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "ico")
private val MARKDOWN_EXTENSIONS = setOf("md", "markdown", "mdown", "mkd")

@Composable
fun FileViewerScreen(
  state: FileState,
  webUrl: String?,
  markdown: MarkdownContext,
  onRetry: () -> Unit,
  onOpenInBrowser: (String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val c = Gl.colors
  val clipboard = LocalClipboardManager.current
  val name = state.path.substringAfterLast('/')
  val extension = name.substringAfterLast('.', "").lowercase()
  val isImage = extension in IMAGE_EXTENSIONS
  val isMarkdown = extension in MARKDOWN_EXTENSIONS
  val ref = state.ref ?: "HEAD"
  val content = state.content.data
  var preview by rememberSaveable(state.path) { mutableStateOf(true) }

  Column(modifier = modifier.fillMaxSize().background(c.bg)) {
    GlTopBar(title = name.ifBlank { "File" }, subtitle = state.path, onBack = onBack) {
      GlIconAction(
        icon = Icons.Outlined.ContentCopy,
        contentDescription = "Copy file contents",
        enabled = !content.isNullOrEmpty() && !isImage,
        onClick = { content?.let { clipboard.setText(AnnotatedString(it)) } },
      )
      if (webUrl != null) {
        GlIconAction(
          icon = Icons.AutoMirrored.Outlined.OpenInNew,
          contentDescription = "Open in browser",
          onClick = { onOpenInBrowser("$webUrl/-/blob/$ref/${state.path}") },
        )
      }
    }
    if (state.ref != null || isMarkdown) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = GlSpace.margin, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GlSpace.tight),
      ) {
        state.ref?.let { MonoPill(it, Modifier.weight(1f, fill = false)) }
        if (isMarkdown) {
          Spacer(Modifier.weight(1f))
          GlChip("Preview", selected = preview, onClick = { preview = true })
          GlChip("Code", selected = !preview, onClick = { preview = false })
        }
      }
    }
    GlDivider()
    when {
      isImage && webUrl != null -> Box(
        modifier = Modifier.fillMaxSize().padding(GlSpace.margin),
        contentAlignment = Alignment.Center,
      ) {
        AsyncImage(
          model = rememberImageModel("$webUrl/-/raw/$ref/${state.path}"),
          contentDescription = name,
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlRadius.chip))
            .border(1.dp, c.imageOutline, RoundedCornerShape(GlRadius.chip)),
        )
      }
      state.content.loading && content == null -> FullScreenState { LoadingState(label = "Loading file") }
      state.content.error != null && content == null -> FullScreenState { ErrorState(state.content.error, onRetry) }
      content == null -> FullScreenState { LoadingState(label = "Loading file") }
      content.isEmpty() -> FullScreenState {
        EmptyState(Icons.Outlined.Difference, "Empty file", "This file has no content.")
      }
      isMarkdown && preview -> Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(GlSpace.margin),
      ) {
        MarkdownText(content, context = markdown)
        Spacer(Modifier.height(GlSpace.group))
      }
      else -> LazyCodeView(content, path = state.path)
    }
  }
}

// ---------------------------------------------------------------------------------------------
// Issue detail
// ---------------------------------------------------------------------------------------------

@Composable
fun IssueDetailScreen(
  state: IssueDetailState,
  labels: Map<String, GitLabLabel>,
  canWrite: Boolean,
  onLink: (String) -> Boolean,
  onRetry: () -> Unit,
  onComment: (String, (String?) -> Unit) -> Unit,
  onSetClosed: (Boolean) -> Unit,
  onEdit: (String, String, (String?) -> Unit) -> Unit,
  onOpenUser: (GitLabUser) -> Unit,
  onOpenInBrowser: (String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val c = Gl.colors
  val issue = state.issue.data
  var editing by rememberSaveable { mutableStateOf(false) }
  var showActivity by rememberSaveable { mutableStateOf(false) }
  val context = remember(issue?.webUrl, onLink) {
    MarkdownContext(webBase = issue?.webUrl?.substringBefore("/-/")?.takeIf { it.isNotBlank() }, rawBase = null, onLink = onLink)
  }

  Column(modifier = modifier.fillMaxSize().background(c.bg)) {
    GlTopBar(
      title = "#${issue?.iid ?: state.iid}",
      subtitle = issue?.let { it.references?.full?.takeIf { r -> r.isNotBlank() } ?: projectPathFromWebUrl(it.webUrl) },
      onBack = onBack,
    ) {
      if (canWrite && issue != null) {
        GlIconAction(Icons.Outlined.Edit, "Edit issue", onClick = { editing = true })
      }
      val web = issue?.webUrl
      if (!web.isNullOrBlank()) {
        GlIconAction(Icons.AutoMirrored.Outlined.OpenInNew, "Open in browser", onClick = { onOpenInBrowser(web) })
      }
    }
    GlDivider()
    when {
      issue == null && state.issue.error != null -> FullScreenState { ErrorState(state.issue.error, onRetry) }
      issue == null -> FullScreenState { LoadingState(label = "Loading issue") }
      else -> {
        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          contentPadding = PaddingValues(bottom = GlSpace.group),
        ) {
          item(key = "header") { IssueHeader(issue, context, onOpenUser) }
          if (state.issue.error != null) {
            item(key = "issue-error") {
              InlineError(state.issue.error, Modifier.padding(horizontal = GlSpace.margin, vertical = 4.dp), onRetry = onRetry)
            }
          }
          item(key = "meta") { IssueMeta(issue, labels, onOpenUser) }
          if (canWrite) {
            item(key = "actions") {
              val open = issue.state == "opened"
              GlButton(
                text = if (open) "Close issue" else "Reopen issue",
                onClick = { onSetClosed(open) },
                kind = GlButtonKind.Secondary,
                loading = state.busy,
                modifier = Modifier.fillMaxWidth().padding(horizontal = GlSpace.margin, vertical = GlSpace.tight),
              )
            }
          }
          notesSection(
            notes = state.notes,
            showActivity = showActivity,
            onToggleActivity = { showActivity = !showActivity },
            context = context,
            onOpenUser = onOpenUser,
            onRetry = onRetry,
          )
        }
        if (canWrite && issue.discussionLocked != true) {
          CommentComposer(posting = state.posting, previewContext = context, onSubmit = onComment)
        }
      }
    }
  }

  if (editing && issue != null) {
    EditIssueDialog(issue, context, onDismiss = { editing = false }, onSave = onEdit)
  }
}

@Composable
private fun IssueHeader(issue: GitLabIssue, context: MarkdownContext, onOpenUser: (GitLabUser) -> Unit) {
  val c = Gl.colors
  Column(
    modifier = Modifier.padding(start = GlSpace.margin, end = GlSpace.margin, top = GlSpace.margin),
    verticalArrangement = Arrangement.spacedBy(GlSpace.tight),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      StateBadge(issueStateStyle(issue.state))
      if (issue.confidential) GlBadge("Confidential", tone = Tone.Warning, icon = Icons.Outlined.Lock)
      if (issue.discussionLocked == true) GlBadge("Locked", icon = Icons.Outlined.Lock)
    }
    Text(issue.title, style = Gl.type.title, color = c.textPrimary)
    AuthorLine(issue.author, "opened ${issue.createdAt}", onOpenUser)
    val tasks = issue.taskStatus
    if (tasks != null && tasks.count > 0) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${tasks.completedCount} of ${tasks.count} tasks", style = Gl.type.meta, color = c.textSecondary)
        LinearProgressIndicator(
          progress = { tasks.completedCount.toFloat() / tasks.count },
          modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
          color = c.success,
          trackColor = c.surface3,
        )
      }
    }
    Spacer(Modifier.height(4.dp))
    val description = issue.description
    if (description.isNullOrBlank()) {
      Text("No description provided.", style = Gl.type.bodySmall, color = c.textTertiary)
    } else {
      MarkdownText(description, context = context)
    }
    Spacer(Modifier.height(GlSpace.tight))
  }
}

@Composable
private fun IssueMeta(issue: GitLabIssue, labels: Map<String, GitLabLabel>, onOpenUser: (GitLabUser) -> Unit) {
  val rows = buildList<Pair<String, @Composable () -> Unit>> {
    if (issue.assignees.isNotEmpty()) add("Assignees" to { PeopleList(issue.assignees, onOpenUser) })
    if (issue.labels.isNotEmpty()) add("Labels" to { LabelRow(issue.labels, labels) })
    issue.milestone?.title?.takeIf { it.isNotBlank() }?.let { add("Milestone" to { MetaValue(it) }) }
    issue.dueDate?.takeIf { it.isNotBlank() }?.let { add("Due date" to { MetaValue(it) }) }
    issue.weight?.let { add("Weight" to { MetaValue(it.toString()) }) }
    val time = issue.timeStats
    val timeText = listOfNotNull(time?.spent?.let { "$it spent" }, time?.estimate?.let { "$it estimated" }).joinToString(" · ")
    if (timeText.isNotBlank()) add("Time tracking" to { MetaValue(timeText) })
    if (issue.mergeRequestsCount > 0) {
      add("Merge requests" to { MetaValue(if (issue.mergeRequestsCount == 1) "1 related" else "${issue.mergeRequestsCount} related") })
    }
  }
  if (rows.isEmpty()) return
  MetaCard(rows)
}

@Composable
private fun EditIssueDialog(
  issue: GitLabIssue,
  context: MarkdownContext,
  onDismiss: () -> Unit,
  onSave: (String, String, (String?) -> Unit) -> Unit,
) {
  val c = Gl.colors
  var title by rememberSaveable(issue.id) { mutableStateOf(issue.title) }
  var description by remember(issue.id) { mutableStateOf(TextFieldValue(issue.description.orEmpty())) }
  var saving by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  var titleError by remember { mutableStateOf(false) }
  AlertDialog(
    onDismissRequest = { if (!saving) onDismiss() },
    containerColor = c.surface,
    title = { Text("Edit issue", style = Gl.type.title, color = c.textPrimary) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it; titleError = false },
          label = { Text("Title") },
          isError = titleError,
          supportingText = if (titleError) ({ Text("Enter a title") }) else null,
          singleLine = true,
          enabled = !saving,
          colors = fieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )
        MarkdownEditor(
          value = description,
          onValueChange = { description = it },
          label = "Description",
          minLines = 4,
          maxLines = 10,
          enabled = !saving,
          previewContext = context,
        )
        error?.let { InlineError(it) }
      }
    },
    confirmButton = {
      GlButton(
        text = "Save changes",
        loading = saving,
        onClick = {
          if (title.isBlank()) {
            titleError = true
          } else {
            saving = true
            error = null
            onSave(title, description.text) { result ->
              saving = false
              if (result == null) onDismiss() else error = result
            }
          }
        },
      )
    },
    dismissButton = { GlButton(text = "Cancel", onClick = onDismiss, kind = GlButtonKind.Ghost, enabled = !saving) },
  )
}

// ---------------------------------------------------------------------------------------------
// Merge request detail
// ---------------------------------------------------------------------------------------------

private enum class MrTab { OVERVIEW, COMMENTS, COMMITS, PIPELINES, CHANGES }

@Composable
fun MergeRequestDetailScreen(
  state: MergeRequestDetailState,
  labels: Map<String, GitLabLabel>,
  canWrite: Boolean,
  onLink: (String) -> Boolean,
  onRetry: () -> Unit,
  onMerge: () -> Unit,
  onSetClosed: (Boolean) -> Unit,
  onComment: (String, (String?) -> Unit) -> Unit,
  onOpenPipeline: (GitLabPipeline) -> Unit,
  onOpenCommit: (String) -> Unit,
  onOpenUser: (GitLabUser) -> Unit,
  onOpenInBrowser: (String) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val c = Gl.colors
  val mr = state.mergeRequest.data
  var tab by rememberSaveable { mutableStateOf(MrTab.OVERVIEW) }
  var showActivity by rememberSaveable { mutableStateOf(false) }
  var confirmMerge by rememberSaveable { mutableStateOf(false) }
  val context = remember(mr?.webUrl, onLink) {
    MarkdownContext(webBase = mr?.webUrl?.substringBefore("/-/")?.takeIf { it.isNotBlank() }, rawBase = null, onLink = onLink)
  }

  Column(modifier = modifier.fillMaxSize().background(c.bg)) {
    GlTopBar(
      title = "!${mr?.iid ?: state.iid}",
      subtitle = mr?.let { "${it.sourceBranch} → ${it.targetBranch}" },
      onBack = onBack,
    ) {
      val web = mr?.webUrl
      if (!web.isNullOrBlank()) {
        GlIconAction(Icons.AutoMirrored.Outlined.OpenInNew, "Open in browser", onClick = { onOpenInBrowser(web) })
      }
    }
    when {
      mr == null && state.mergeRequest.error != null -> {
        GlDivider()
        FullScreenState { ErrorState(state.mergeRequest.error, onRetry) }
      }
      mr == null -> {
        GlDivider()
        FullScreenState { LoadingState(label = "Loading merge request") }
      }
      else -> {
        GlFilterRow(
          options = MrTab.entries,
          selected = tab,
          label = { t -> tabLabel(t, state) },
          onSelect = { tab = it },
        )
        GlDivider()
        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          contentPadding = PaddingValues(bottom = GlSpace.group),
        ) {
          when (tab) {
            MrTab.OVERVIEW -> item(key = "overview") {
              MergeRequestOverview(
                mr = mr,
                state = state,
                labels = labels,
                context = context,
                canWrite = canWrite,
                onRetry = onRetry,
                onRequestMerge = { confirmMerge = true },
                onSetClosed = onSetClosed,
                onOpenPipeline = onOpenPipeline,
                onOpenUser = onOpenUser,
              )
            }
            MrTab.COMMENTS -> notesSection(
              notes = state.notes,
              showActivity = showActivity,
              onToggleActivity = { showActivity = !showActivity },
              context = context,
              onOpenUser = onOpenUser,
              onRetry = onRetry,
            )
            MrTab.COMMITS -> loadableList(
              feed = state.commits,
              key = "commits",
              loadingLabel = "Loading commits",
              emptyTitle = "No commits",
              emptyBody = "This merge request has no commits yet.",
              onRetry = onRetry,
            ) { commits ->
              itemsIndexed(commits, key = { _, commit -> commit.id }) { index, commit ->
                if (index > 0) GlDivider(inset = GlSpace.margin)
                CommitRow(commit, onClick = { onOpenCommit(commit.id) })
              }
            }
            MrTab.PIPELINES -> loadableList(
              feed = state.pipelines,
              key = "pipelines",
              loadingLabel = "Loading pipelines",
              emptyTitle = "No pipelines",
              emptyBody = "Pipelines that ran for this merge request appear here.",
              onRetry = onRetry,
            ) { pipelines ->
              items(pipelines, key = { it.id }) { pipeline ->
                PipelineCard(
                  pipeline = pipeline,
                  onRetry = null,
                  onClick = { onOpenPipeline(pipeline) },
                  modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = 6.dp),
                )
              }
            }
            MrTab.CHANGES -> loadableList(
              feed = state.diffs,
              key = "changes",
              loadingLabel = "Loading changes",
              emptyTitle = "No changes",
              emptyBody = "This merge request doesn't change any files.",
              onRetry = onRetry,
            ) { diffs ->
              itemsIndexed(diffs, key = { index, d -> "$index-${d.newPath}" }) { _, diff ->
                DetailDiffCard(diff, Modifier.padding(horizontal = GlSpace.margin, vertical = 6.dp))
              }
            }
          }
        }
        if (tab == MrTab.COMMENTS && canWrite) {
          CommentComposer(posting = state.posting, previewContext = context, onSubmit = onComment)
        }
      }
    }
  }

  if (confirmMerge && mr != null) {
    AlertDialog(
      onDismissRequest = { confirmMerge = false },
      containerColor = c.surface,
      title = { Text("Merge !${mr.iid} into ${mr.targetBranch}?", style = Gl.type.title, color = c.textPrimary) },
      text = {
        Text(
          "Merging adds the changes from ${mr.sourceBranch} to ${mr.targetBranch}. This can't be undone from the app.",
          style = Gl.type.bodySmall,
          color = c.textSecondary,
        )
      },
      confirmButton = {
        GlButton(text = "Merge", onClick = {
          confirmMerge = false
          onMerge()
        })
      },
      dismissButton = { GlButton(text = "Cancel", onClick = { confirmMerge = false }, kind = GlButtonKind.Ghost) },
    )
  }
}

private fun tabLabel(tab: MrTab, state: MergeRequestDetailState): String {
  fun count(feed: Loadable<out List<*>>) = if (feed.loaded && feed.error == null) " ${feed.data.size}" else ""
  return when (tab) {
    MrTab.OVERVIEW -> "Overview"
    MrTab.COMMENTS -> "Comments" + (if (state.notes.loaded && state.notes.error == null) " ${state.notes.data.count { !it.system }}" else "")
    MrTab.COMMITS -> "Commits" + count(state.commits)
    MrTab.PIPELINES -> "Pipelines" + count(state.pipelines)
    MrTab.CHANGES -> "Changes" + count(state.diffs)
  }
}

@Composable
private fun MergeRequestOverview(
  mr: GitLabMergeRequest,
  state: MergeRequestDetailState,
  labels: Map<String, GitLabLabel>,
  context: MarkdownContext,
  canWrite: Boolean,
  onRetry: () -> Unit,
  onRequestMerge: () -> Unit,
  onSetClosed: (Boolean) -> Unit,
  onOpenPipeline: (GitLabPipeline) -> Unit,
  onOpenUser: (GitLabUser) -> Unit,
) {
  val c = Gl.colors
  Column(
    modifier = Modifier.padding(top = GlSpace.margin),
    verticalArrangement = Arrangement.spacedBy(GlSpace.tight),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = GlSpace.margin),
      verticalArrangement = Arrangement.spacedBy(GlSpace.tight),
    ) {
      StateBadge(mergeRequestStateStyle(mr.state, mr.isDraft))
      Text(mr.title, style = Gl.type.title, color = c.textPrimary)
      AuthorLine(mr.author, "opened ${mr.createdAt}", onOpenUser)
      MonoPill("${mr.sourceBranch} → ${mr.targetBranch}")
    }
    if (state.mergeRequest.error != null) {
      InlineError(state.mergeRequest.error, Modifier.padding(horizontal = GlSpace.margin), onRetry = onRetry)
    }

    val rows = buildList<Pair<String, @Composable () -> Unit>> {
      mr.headPipeline?.let { pipeline ->
        add("Pipeline" to {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(GlRadius.chip))
              .clickable(role = Role.Button, onClickLabel = "Open pipeline") { onOpenPipeline(pipeline) }
              .defaultMinSize(minHeight = GlSpace.touch),
          ) {
            PipelineStatusBadge(pipeline.status)
            Spacer(Modifier.width(8.dp))
            Text("#${pipeline.id}", style = Gl.type.numeric, color = c.textSecondary)
          }
        })
      }
      if (mr.state == "opened") {
        mergeStatusText(mr)?.let { (text, tone) -> add("Merge status" to { GlBadge(text, tone = tone) }) }
      }
      state.approvals.data?.let { approvals ->
        val approvers = approvals.approvedBy.map { it.user }
        val left = approvals.approvalsLeft ?: 0
        when {
          approvers.isNotEmpty() -> add("Approved by" to { PeopleList(approvers, onOpenUser) })
          left > 0 -> add("Approvals" to { MetaValue(if (left == 1) "1 approval required" else "$left approvals required") })
        }
      }
      if (mr.reviewers.isNotEmpty()) add("Reviewers" to { PeopleList(mr.reviewers, onOpenUser) })
      if (mr.assignees.isNotEmpty()) add("Assignees" to { PeopleList(mr.assignees, onOpenUser) })
      if (mr.labels.isNotEmpty()) add("Labels" to { LabelRow(mr.labels, labels) })
      mr.milestone?.title?.takeIf { it.isNotBlank() }?.let { add("Milestone" to { MetaValue(it) }) }
      if (mr.state == "merged") {
        val by = (mr.mergedBy ?: mr.mergeUser)?.let { it.name.ifBlank { it.username } }
        val text = listOfNotNull(by?.let { "by $it" }, mr.mergedAt).joinToString(" · ")
        if (text.isNotBlank()) add("Merged" to { MetaValue(text) })
      }
      if (mr.upvotes > 0 || mr.downvotes > 0) {
        add("Reactions" to {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) {
              contentDescription = "${mr.upvotes} thumbs up, ${mr.downvotes} thumbs down"
            },
          ) {
            Icon(Icons.Outlined.ThumbUp, null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(mr.upvotes.toString(), style = Gl.type.numeric, color = c.textPrimary)
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Outlined.ThumbDown, null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(mr.downvotes.toString(), style = Gl.type.numeric, color = c.textPrimary)
          }
        })
      }
    }
    if (rows.isNotEmpty()) MetaCard(rows)

    Column(modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = GlSpace.tight)) {
      val description = mr.description
      if (description.isNullOrBlank()) {
        Text("No description provided.", style = Gl.type.bodySmall, color = c.textTertiary)
      } else {
        MarkdownText(description, context = context)
      }
    }

    if (canWrite) {
      Column(
        modifier = Modifier.padding(horizontal = GlSpace.margin),
        verticalArrangement = Arrangement.spacedBy(GlSpace.tight),
      ) {
        when (mr.state) {
          "opened" -> {
            val conflicts = mr.hasConflicts == true || mr.detailedMergeStatus == "conflict"
            val blockedReason = when {
              conflicts -> "Resolve conflicts first"
              mr.isDraft -> "Mark as ready first"
              else -> null
            }
            GlButton(
              text = "Merge",
              onClick = onRequestMerge,
              enabled = blockedReason == null,
              loading = state.busy,
              modifier = Modifier.fillMaxWidth(),
            )
            if (blockedReason != null) {
              Text(blockedReason, style = Gl.type.meta, color = c.textSecondary)
            }
            GlButton(
              text = "Close merge request",
              onClick = { onSetClosed(true) },
              kind = GlButtonKind.Secondary,
              enabled = !state.busy,
              modifier = Modifier.fillMaxWidth(),
            )
          }
          "closed" -> GlButton(
            text = "Reopen merge request",
            onClick = { onSetClosed(false) },
            kind = GlButtonKind.Secondary,
            loading = state.busy,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}

/** detailed_merge_status in plain words; null when there is nothing worth saying. */
private fun mergeStatusText(mr: GitLabMergeRequest): Pair<String, Tone>? {
  val status = mr.detailedMergeStatus ?: mr.mergeStatus ?: return null
  return when (status) {
    "mergeable", "can_be_merged" -> "Ready to merge" to Tone.Success
    "conflict", "broken_status", "cannot_be_merged" -> "Has conflicts" to Tone.Danger
    "draft_status" -> "Draft" to Tone.Neutral
    "ci_must_pass", "ci_still_running" -> "Waiting for pipeline" to Tone.Warning
    "discussions_not_resolved" -> "Unresolved threads" to Tone.Warning
    "not_approved" -> "Needs approval" to Tone.Warning
    "checking", "unchecked", "preparing" -> "Checking" to Tone.Neutral
    "need_rebase" -> "Needs rebase" to Tone.Warning
    "blocked_status" -> "Blocked by another merge request" to Tone.Warning
    "not_open" -> null
    else -> status.replace('_', ' ').replaceFirstChar { it.uppercase() } to Tone.Neutral
  }
}

// ---------------------------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------------------------

private fun projectPathFromWebUrl(webUrl: String): String? {
  val path = webUrl.substringAfter("://", "").substringAfter('/', "").substringBefore("/-/")
  return path.takeIf { it.isNotBlank() }
}

@Composable
private fun AuthorLine(author: GitLabUser, action: String, onOpenUser: (GitLabUser) -> Unit) {
  val name = author.name.ifBlank { author.username }.ifBlank { "Someone" }
  val openable = author.username.isNotBlank() || author.id > 0
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(GlRadius.chip))
      .then(if (openable) Modifier.clickable(role = Role.Button, onClickLabel = "Open profile") { onOpenUser(author) } else Modifier)
      .defaultMinSize(minHeight = 32.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Avatar(author.avatarUrl, name, 24.dp)
    Spacer(Modifier.width(8.dp))
    Text(
      text = "$name $action",
      style = Gl.type.meta,
      color = Gl.colors.textSecondary,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun MetaCard(rows: List<Pair<String, @Composable () -> Unit>>) {
  GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = GlSpace.tight)) {
    rows.forEachIndexed { index, (label, content) ->
      if (index > 0) GlDivider(inset = GlSpace.margin)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = GlSpace.touch)
          .padding(horizontal = GlSpace.margin, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(label, style = Gl.type.meta, color = Gl.colors.textSecondary)
        content()
      }
    }
  }
}

@Composable
private fun MetaValue(text: String) {
  Text(text, style = Gl.type.bodySmall, color = Gl.colors.textPrimary)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeopleList(users: List<GitLabUser>, onOpenUser: (GitLabUser) -> Unit) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    users.forEach { user ->
      val name = user.name.ifBlank { user.username }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(GlRadius.chip))
          .clickable(role = Role.Button, onClickLabel = "Open profile of $name") { onOpenUser(user) }
          .defaultMinSize(minHeight = 40.dp)
          .padding(end = 4.dp),
      ) {
        Avatar(user.avatarUrl, name, 24.dp)
        Spacer(Modifier.width(6.dp))
        Text(name, style = Gl.type.bodySmall, color = Gl.colors.textPrimary, maxLines = 1)
      }
    }
  }
}

/** Renders a Loadable list with its loading, error and empty states. */
private fun <T> LazyListScope.loadableList(
  feed: Loadable<List<T>>,
  key: String,
  loadingLabel: String,
  emptyTitle: String,
  emptyBody: String,
  onRetry: () -> Unit,
  content: LazyListScope.(List<T>) -> Unit,
) {
  val error = feed.error
  when {
    feed.initialLoading -> item(key = "$key-loading") { LoadingState(label = loadingLabel) }
    error != null && feed.data.isEmpty() -> item(key = "$key-error") { ErrorState(error, onRetry) }
    feed.data.isEmpty() -> item(key = "$key-empty") { EmptyState(Icons.Outlined.Commit, emptyTitle, emptyBody) }
    else -> {
      if (error != null) {
        item(key = "$key-inline-error") {
          InlineError(error, Modifier.padding(horizontal = GlSpace.margin, vertical = 4.dp), onRetry = onRetry)
        }
      }
      content(feed.data)
    }
  }
}

private fun LazyListScope.notesSection(
  notes: Loadable<List<GitLabNote>>,
  showActivity: Boolean,
  onToggleActivity: () -> Unit,
  context: MarkdownContext,
  onOpenUser: (GitLabUser) -> Unit,
  onRetry: () -> Unit,
) {
  val comments = notes.data.count { !it.system }
  val hasActivity = notes.data.any { it.system }
  val error = notes.error
  item(key = "notes-header") {
    GlSectionHeader(
      title = if (notes.loaded && error == null) "Comments ($comments)" else "Comments",
      trailing = if (hasActivity) {
        {
          GlButton(
            text = if (showActivity) "Hide activity" else "Show activity",
            onClick = onToggleActivity,
            kind = GlButtonKind.Ghost,
          )
        }
      } else null,
    )
  }
  val visible = notes.data.filter { showActivity || !it.system }
  when {
    notes.initialLoading -> item(key = "notes-loading") { LoadingState(label = "Loading comments") }
    error != null && notes.data.isEmpty() -> item(key = "notes-error") {
      InlineError(error, Modifier.padding(horizontal = GlSpace.margin), onRetry = onRetry)
    }
    visible.isEmpty() -> item(key = "notes-empty") {
      Row(
        modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = GlSpace.tight),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Gl.colors.textTertiary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("No comments yet", style = Gl.type.bodySmall, color = Gl.colors.textSecondary)
      }
    }
    else -> items(visible, key = { "note-${it.id}" }) { note ->
      if (note.system) SystemNote(note) else NoteItem(note, context, onOpenUser)
    }
  }
}

@Composable
private fun NoteItem(note: GitLabNote, context: MarkdownContext, onOpenUser: (GitLabUser) -> Unit) {
  val c = Gl.colors
  val name = note.author.name.ifBlank { note.author.username }.ifBlank { "Someone" }
  GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = 4.dp)) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(role = Role.Button, onClickLabel = "Open profile of $name") { onOpenUser(note.author) }
        .defaultMinSize(minHeight = GlSpace.touch)
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Avatar(note.author.avatarUrl, name, 28.dp)
      Spacer(Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(name, style = Gl.type.label, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          text = listOfNotNull(
            note.author.username.takeIf { it.isNotBlank() }?.let { "@$it" },
            note.createdAt.takeIf { it.isNotBlank() },
          ).joinToString(" · "),
          style = Gl.type.meta,
          color = c.textSecondary,
          maxLines = 1,
        )
      }
      if (note.type == "DiffNote") GlBadge("On the diff")
      if (note.resolved == true) {
        Spacer(Modifier.width(6.dp))
        GlBadge("Resolved", tone = Tone.Success)
      }
    }
    GlDivider()
    Box(modifier = Modifier.padding(12.dp)) {
      MarkdownText(note.body, context = context, textStyle = Gl.type.bodySmall)
    }
  }
}

@Composable
private fun SystemNote(note: GitLabNote) {
  val name = note.author.name.ifBlank { note.author.username }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = GlSpace.margin + 4.dp, vertical = 6.dp)
      .semantics(mergeDescendants = true) {},
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Avatar(note.author.avatarUrl, name, 20.dp)
    Spacer(Modifier.width(8.dp))
    Text(
      text = listOf(name, note.body.lineSequence().firstOrNull().orEmpty(), note.createdAt).filter { it.isNotBlank() }.joinToString(" · "),
      style = Gl.type.meta,
      color = Gl.colors.textSecondary,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/** Bottom composer: Markdown editor plus a Comment button. The draft survives a failed post. */
@Composable
private fun CommentComposer(
  posting: Boolean,
  previewContext: MarkdownContext,
  onSubmit: (String, (String?) -> Unit) -> Unit,
) {
  val c = Gl.colors
  var draft by remember { mutableStateOf(TextFieldValue("")) }
  var error by remember { mutableStateOf<String?>(null) }
  var emptyError by remember { mutableStateOf(false) }
  Surface(color = c.surface) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .imePadding()
        .navigationBarsPadding()
        .heightIn(max = 420.dp)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = GlSpace.margin, vertical = GlSpace.tight),
      verticalArrangement = Arrangement.spacedBy(GlSpace.tight),
    ) {
      MarkdownEditor(
        value = draft,
        onValueChange = { draft = it; emptyError = false },
        label = "Add a comment",
        minLines = 2,
        maxLines = 8,
        enabled = !posting,
        isError = emptyError,
        supportingText = if (emptyError) "Write a comment first" else null,
        previewContext = previewContext,
      )
      error?.let { InlineError(it) }
      GlButton(
        text = "Comment",
        loading = posting,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
          val body = draft.text.trim()
          if (body.isEmpty()) {
            emptyError = true
          } else {
            error = null
            onSubmit(body) { result ->
              if (result == null) draft = TextFieldValue("") else error = result
            }
          }
        },
      )
    }
  }
}

/** One changed file: path, what happened to it, and the diff. */
@Composable
internal fun DetailDiffCard(diff: GitLabFileDiff, modifier: Modifier = Modifier) {
  val c = Gl.colors
  val path = diff.newPath.ifEmpty { diff.oldPath }
  GlCard(modifier = modifier) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 44.dp)
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = if (diff.renamedFile && diff.oldPath != diff.newPath) "${diff.oldPath} → ${diff.newPath}" else path,
        style = Gl.type.monoSmall,
        color = c.textPrimary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      when {
        diff.newFile -> GlBadge("New", tone = Tone.Success)
        diff.deletedFile -> GlBadge("Deleted", tone = Tone.Danger)
        diff.renamedFile -> GlBadge("Renamed", tone = Tone.Info)
      }
    }
    GlDivider()
    if (diff.diff.isBlank()) {
      Text(
        "No text changes (binary or too large)",
        style = Gl.type.meta,
        color = c.textSecondary,
        modifier = Modifier.padding(12.dp),
      )
    } else {
      DiffViewer(diff.diff, path = path)
    }
  }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
  focusedBorderColor = Gl.colors.textPrimary,
  unfocusedBorderColor = Gl.colors.borderStrong,
  focusedLabelColor = Gl.colors.textPrimary,
  unfocusedLabelColor = Gl.colors.textSecondary,
  cursorColor = Gl.colors.textPrimary,
  errorBorderColor = Gl.colors.danger,
  errorLabelColor = Gl.colors.danger,
  errorSupportingTextColor = Gl.colors.danger,
)
