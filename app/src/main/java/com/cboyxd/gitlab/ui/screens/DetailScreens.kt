package com.cboyxd.gitlab.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.CallMerge
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.data.model.GitLabCommit
import com.cboyxd.gitlab.data.model.GitLabIssue
import com.cboyxd.gitlab.data.model.GitLabLabel
import com.cboyxd.gitlab.data.model.GitLabMergeRequest
import com.cboyxd.gitlab.data.model.GitLabNote
import com.cboyxd.gitlab.data.model.GitLabPipeline
import com.cboyxd.gitlab.data.model.GitLabUser
import com.cboyxd.gitlab.ui.components.Avatar
import com.cboyxd.gitlab.ui.components.ColoredLabel
import com.cboyxd.gitlab.ui.components.EmptyState
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.FullScreenState
import com.cboyxd.gitlab.ui.components.GlBadge
import com.cboyxd.gitlab.ui.components.GlButton
import com.cboyxd.gitlab.ui.components.GlButtonKind
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlTopBar
import com.cboyxd.gitlab.ui.components.InlineError
import com.cboyxd.gitlab.ui.components.LabelRow
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.components.MarkdownContext
import com.cboyxd.gitlab.ui.components.MarkdownEditor
import com.cboyxd.gitlab.ui.components.MarkdownText
import com.cboyxd.gitlab.ui.components.PipelineCard
import com.cboyxd.gitlab.ui.components.PipelineStatusBadge
import com.cboyxd.gitlab.ui.components.Tone
import com.cboyxd.gitlab.ui.components.diffFileItems
import com.cboyxd.gitlab.ui.components.diffStats
import com.cboyxd.gitlab.ui.components.issueStateStyle
import com.cboyxd.gitlab.ui.components.mergeRequestStateStyle
import com.cboyxd.gitlab.ui.components.pipelineStatusStyle
import com.cboyxd.gitlab.ui.components.pressable
import com.cboyxd.gitlab.ui.components.rememberPreparedDiffs
import com.cboyxd.gitlab.ui.components.toneColors
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlRadius
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.IssueDetailState
import com.cboyxd.gitlab.ui.viewmodel.Loadable
import com.cboyxd.gitlab.ui.viewmodel.MergeRequestDetailState
import java.util.Locale

/** Room under the list for the floating COMMENT and details buttons. */
private val FloatingActionsClearance = 96.dp

// ---------------------------------------------------------------------------------------------
// Issue detail
// ---------------------------------------------------------------------------------------------

/**
 * An issue laid out like GitHub Mobile's: path and number, a big title with a filled state pill,
 * the author's post, then the timeline. Comment and details float at the bottom right; edit and
 * close live in the overflow menu.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
  var composing by rememberSaveable { mutableStateOf(false) }
  var showInfo by rememberSaveable { mutableStateOf(false) }
  val clipboard = LocalClipboardManager.current
  val context = remember(issue?.webUrl, onLink) {
    MarkdownContext(webBase = issue?.webUrl?.substringBefore("/-/")?.takeIf { it.isNotBlank() }, rawBase = null, onLink = onLink)
  }
  val listState = rememberLazyListState()
  val scrolledPastTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
  val reference = issue?.let { reference(it.references?.full, it.webUrl, "#", it.iid) } ?: "#${state.iid}"

  Column(modifier = modifier.fillMaxSize().background(c.bg)) {
    val menu = buildList {
      if (canWrite && issue != null) {
        add(DetailMenuItem("Edit", { editing = true }))
        val open = issue.state == "opened"
        add(DetailMenuItem(if (open) "Close issue" else "Reopen issue", { onSetClosed(open) }, enabled = !state.busy))
      }
      issue?.webUrl?.takeIf { it.isNotBlank() }?.let { web ->
        add(DetailMenuItem("Open in browser", { onOpenInBrowser(web) }))
        add(DetailMenuItem("Copy link", { clipboard.setText(AnnotatedString(web)) }))
      }
    }
    GlTopBar(
      title = issue?.title ?: "Issue",
      overline = reference,
      showTitle = scrolledPastTitle || issue == null,
      onBack = onBack,
    ) {
      DetailBarActions(webUrl = issue?.webUrl, title = issue?.title.orEmpty(), menu = menu)
    }
    if (scrolledPastTitle) GlDivider()
    when {
      issue == null && state.issue.error != null -> FullScreenState { ErrorState(state.issue.error, onRetry) }
      issue == null -> FullScreenState { LoadingState(label = "Loading issue") }
      else -> Box(Modifier.weight(1f).fillMaxWidth()) {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = FloatingActionsClearance),
        ) {
          item(key = "header") {
            DetailHeader(reference = reference, title = issue.title) {
              DetailStatePill(issueStateStyle(issue.state))
              if (issue.confidential) GlBadge("Confidential", tone = Tone.Warning, icon = Icons.Outlined.Lock)
              if (issue.discussionLocked == true) GlBadge("Locked", icon = Icons.Outlined.Lock)
              if (state.busy) BusyDot()
            }
          }
          if (state.issue.error != null) {
            item(key = "issue-error") {
              InlineError(state.issue.error, Modifier.padding(horizontal = GlSpace.margin, vertical = 4.dp), onRetry = onRetry)
            }
          }
          item(key = "post") {
            AuthorPost(
              author = issue.author,
              age = issue.createdAt,
              body = issue.description,
              context = context,
              onOpenUser = onOpenUser,
            ) {
              val tasks = issue.taskStatus
              if (tasks != null && tasks.count > 0) TaskProgress(tasks.completedCount, tasks.count)
              Reactions(issue.upvotes, issue.downvotes)
            }
          }
          item(key = "timeline-header") { DetailSectionHeader("Activity") }
          timeline(
            notes = state.notes,
            labels = labels,
            opener = issue.author,
            context = context,
            onOpenUser = onOpenUser,
            onRetry = onRetry,
            guest = !canWrite,
          )
        }
        DetailFloatingActions(
          onComment = if (canWrite && issue.discussionLocked != true) ({ composing = true }) else null,
          onInfo = { showInfo = true },
        )
      }
    }
  }

  if (editing && issue != null) {
    EditIssueDialog(issue, context, onDismiss = { editing = false }, onSave = onEdit)
  }
  if (composing) {
    CommentSheet(
      posting = state.posting,
      previewContext = context,
      onDismiss = { composing = false },
      onSubmit = onComment,
    )
  }
  if (showInfo && issue != null) {
    InfoSheet(title = "Details", onDismiss = { showInfo = false }) {
      issueMetaRows(issue, labels, onOpenUser).forEach { (label, content) -> MetaRow(label, content) }
    }
  }
}

private fun issueMetaRows(
  issue: GitLabIssue,
  labels: Map<String, GitLabLabel>,
  onOpenUser: (GitLabUser) -> Unit,
): List<Pair<String, @Composable () -> Unit>> = buildList {
  add("Assignees" to { if (issue.assignees.isEmpty()) MetaValue("No one") else PeopleList(issue.assignees, onOpenUser) })
  add("Labels" to { if (issue.labels.isEmpty()) MetaValue("None") else LabelRow(issue.labels, labels) })
  add("Milestone" to { MetaValue(issue.milestone?.title?.takeIf { it.isNotBlank() } ?: "None") })
  issue.dueDate?.takeIf { it.isNotBlank() }?.let { add("Due date" to { MetaValue(it) }) }
  issue.weight?.let { add("Weight" to { MetaValue(it.toString()) }) }
  val time = issue.timeStats
  val timeText = listOfNotNull(time?.spent?.let { "$it spent" }, time?.estimate?.let { "$it estimated" }).joinToString(" · ")
  if (timeText.isNotBlank()) add("Time tracking" to { MetaValue(timeText) })
  if (issue.mergeRequestsCount > 0) {
    add("Merge requests" to { MetaValue(if (issue.mergeRequestsCount == 1) "1 related" else "${issue.mergeRequestsCount} related") })
  }
  add("Opened" to { MetaValue(listOf(issue.author.name.ifBlank { issue.author.username }, issue.createdAt).filter { it.isNotBlank() }.joinToString(" · ")) })
  if (issue.state != "opened") {
    val by = issue.closedBy?.let { it.name.ifBlank { it.username } }
    val text = listOfNotNull(by, issue.closedAt).joinToString(" · ")
    if (text.isNotBlank()) add("Closed" to { MetaValue(text) })
  }
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

/** Pages a merge request opens over itself; back returns to the overview. */
private enum class MrPage { OVERVIEW, FILES, COMMITS, PIPELINES }

/**
 * A merge request laid out like a GitHub Mobile pull request: header with state and branches, the
 * description (collapsed when long), then Changes, Status and Conversation sections. Files,
 * commits and pipelines open as full pages inside the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
  val mr = state.mergeRequest.data
  var page by rememberSaveable { mutableStateOf(MrPage.OVERVIEW) }
  var confirmMerge by rememberSaveable { mutableStateOf(false) }
  var composing by rememberSaveable { mutableStateOf(false) }
  var showInfo by rememberSaveable { mutableStateOf(false) }
  val context = remember(mr?.webUrl, onLink) {
    MarkdownContext(webBase = mr?.webUrl?.substringBefore("/-/")?.takeIf { it.isNotBlank() }, rawBase = null, onLink = onLink)
  }
  BackHandler(enabled = page != MrPage.OVERVIEW) { page = MrPage.OVERVIEW }
  val reference = mr?.let { reference(it.references?.full, it.webUrl, "!", it.iid) } ?: "!${state.iid}"

  when (page) {
    MrPage.OVERVIEW -> MergeRequestOverview(
      state = state,
      reference = reference,
      labels = labels,
      context = context,
      canWrite = canWrite,
      onRetry = onRetry,
      onRequestMerge = { confirmMerge = true },
      onSetClosed = onSetClosed,
      onOpenPage = { page = it },
      onOpenPipeline = onOpenPipeline,
      onOpenUser = onOpenUser,
      onOpenInBrowser = onOpenInBrowser,
      onComment = { composing = true },
      onInfo = { showInfo = true },
      onBack = onBack,
      modifier = modifier,
    )
    MrPage.FILES -> FilesChangedPage(state, reference, onRetry, onBack = { page = MrPage.OVERVIEW }, modifier = modifier)
    MrPage.COMMITS -> SubPage(title = "Commits", overline = reference, onBack = { page = MrPage.OVERVIEW }, modifier = modifier) {
      loadableList(
        feed = state.commits,
        key = "commits",
        loadingLabel = "Loading commits",
        emptyTitle = "No commits",
        emptyBody = "This merge request has no commits yet.",
        onRetry = onRetry,
      ) { commits ->
        items(commits, key = { it.id }) { commit -> DetailCommitRow(commit, onClick = { onOpenCommit(commit.id) }) }
      }
    }
    MrPage.PIPELINES -> SubPage(title = "Pipelines", overline = reference, onBack = { page = MrPage.OVERVIEW }, modifier = modifier) {
      loadableList(
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
    }
  }

  if (confirmMerge && mr != null) {
    val c = Gl.colors
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
        MergeButton(label = "Merge", onClick = {
          confirmMerge = false
          onMerge()
        })
      },
      dismissButton = { GlButton(text = "Cancel", onClick = { confirmMerge = false }, kind = GlButtonKind.Ghost) },
    )
  }
  if (composing) {
    CommentSheet(
      posting = state.posting,
      previewContext = context,
      onDismiss = { composing = false },
      onSubmit = onComment,
    )
  }
  if (showInfo && mr != null) {
    InfoSheet(title = "Details", onDismiss = { showInfo = false }) {
      MetaRow("Assignees") { if (mr.assignees.isEmpty()) MetaValue("No one") else PeopleList(mr.assignees, onOpenUser) }
      MetaRow("Reviewers") { if (mr.reviewers.isEmpty()) MetaValue("None requested") else PeopleList(mr.reviewers, onOpenUser) }
      MetaRow("Labels") { if (mr.labels.isEmpty()) MetaValue("None") else LabelRow(mr.labels, labels) }
      MetaRow("Milestone") { MetaValue(mr.milestone?.title?.takeIf { it.isNotBlank() } ?: "None") }
      MetaRow("Branches") { MetaValue("${mr.sourceBranch} → ${mr.targetBranch}") }
      MetaRow("Opened") { MetaValue(listOf(mr.author.name.ifBlank { mr.author.username }, mr.createdAt).filter { it.isNotBlank() }.joinToString(" · ")) }
      if (mr.squash == true) MetaRow("Merge options") { MetaValue("Squash commits when merging") }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MergeRequestOverview(
  state: MergeRequestDetailState,
  reference: String,
  labels: Map<String, GitLabLabel>,
  context: MarkdownContext,
  canWrite: Boolean,
  onRetry: () -> Unit,
  onRequestMerge: () -> Unit,
  onSetClosed: (Boolean) -> Unit,
  onOpenPage: (MrPage) -> Unit,
  onOpenPipeline: (GitLabPipeline) -> Unit,
  onOpenUser: (GitLabUser) -> Unit,
  onOpenInBrowser: (String) -> Unit,
  onComment: () -> Unit,
  onInfo: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier,
) {
  val c = Gl.colors
  val mr = state.mergeRequest.data
  val clipboard = LocalClipboardManager.current
  val listState = rememberLazyListState()
  val scrolledPastTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

  Column(modifier = modifier.fillMaxSize().background(c.bg)) {
    val menu = buildList {
      if (canWrite && mr != null) {
        when (mr.state) {
          "opened" -> add(DetailMenuItem("Close merge request", { onSetClosed(true) }, enabled = !state.busy))
          "closed" -> add(DetailMenuItem("Reopen merge request", { onSetClosed(false) }, enabled = !state.busy))
        }
      }
      mr?.webUrl?.takeIf { it.isNotBlank() }?.let { web ->
        add(DetailMenuItem("Open in browser", { onOpenInBrowser(web) }))
        add(DetailMenuItem("Copy link", { clipboard.setText(AnnotatedString(web)) }))
      }
    }
    GlTopBar(
      title = mr?.title ?: "Merge request",
      overline = reference,
      showTitle = scrolledPastTitle || mr == null,
      onBack = onBack,
    ) {
      DetailBarActions(webUrl = mr?.webUrl, title = mr?.title.orEmpty(), menu = menu)
    }
    if (scrolledPastTitle) GlDivider()
    when {
      mr == null && state.mergeRequest.error != null -> FullScreenState { ErrorState(state.mergeRequest.error, onRetry) }
      mr == null -> FullScreenState { LoadingState(label = "Loading merge request") }
      else -> Box(Modifier.weight(1f).fillMaxWidth()) {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = FloatingActionsClearance),
        ) {
          item(key = "header") {
            DetailHeader(reference = reference, title = mr.title) {
              DetailStatePill(mergeRequestStateStyle(mr.state, mr.isDraft))
              DetailRefPill(mr.sourceBranch)
              Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "into",
                tint = c.textSecondary,
                modifier = Modifier.size(18.dp).align(Alignment.CenterVertically),
              )
              DetailRefPill(mr.targetBranch)
              if (state.busy) BusyDot()
            }
          }
          if (state.mergeRequest.error != null) {
            item(key = "mr-error") {
              InlineError(state.mergeRequest.error, Modifier.padding(horizontal = GlSpace.margin, vertical = 4.dp), onRetry = onRetry)
            }
          }
          item(key = "post") {
            AuthorPost(
              author = mr.author,
              age = mr.createdAt,
              body = mr.description,
              context = context,
              onOpenUser = onOpenUser,
              collapsible = true,
            ) {
              Reactions(mr.upvotes, mr.downvotes)
            }
          }

          item(key = "changes-header") { DetailSectionHeader("Changes") }
          item(key = "changes") { ChangesRows(state, onOpenPage) }

          item(key = "status-header") { DetailSectionHeader("Status") }
          item(key = "status") {
            StatusRows(
              mr = mr,
              state = state,
              canWrite = canWrite,
              onRequestMerge = onRequestMerge,
              onSetClosed = onSetClosed,
              onOpenPipeline = onOpenPipeline,
              onOpenUser = onOpenUser,
            )
          }

          item(key = "timeline-header") { DetailSectionHeader("Conversation") }
          timeline(
            notes = state.notes,
            labels = labels,
            opener = mr.author,
            context = context,
            onOpenUser = onOpenUser,
            onRetry = onRetry,
            guest = !canWrite,
          )
        }
        DetailFloatingActions(onComment = if (canWrite) onComment else null, onInfo = onInfo)
      }
    }
  }
}

/** "534 files changed +12,881 −6,776", commits and pipelines; each opens its own page. */
@Composable
private fun ChangesRows(state: MergeRequestDetailState, onOpenPage: (MrPage) -> Unit) {
  val c = Gl.colors
  val diffs = state.diffs
  val stats = remember(diffs.data) { diffStats(diffs.data) }
  Column {
    DetailRow(
      icon = Icons.Outlined.Difference,
      title = when {
        diffs.loaded && diffs.error == null -> plural(diffs.data.size, "file changed", "files changed")
        state.mergeRequest.data?.changesCount != null -> "${state.mergeRequest.data?.changesCount} files changed"
        else -> "Files changed"
      },
      onClick = { onOpenPage(MrPage.FILES) },
      onClickLabel = "Show changed files",
    ) {
      when {
        diffs.initialLoading -> SmallSpinner()
        diffs.error != null && diffs.data.isEmpty() -> Icon(Icons.Outlined.ErrorOutline, "Couldn't load", tint = c.danger, modifier = Modifier.size(18.dp))
        else -> DiffCounts(stats.added, stats.removed)
      }
    }
    GlDivider()
    val commits = state.commits
    DetailRow(
      icon = Icons.Outlined.Commit,
      title = if (commits.loaded && commits.error == null) plural(commits.data.size, "commit", "commits") else "Commits",
      onClick = { onOpenPage(MrPage.COMMITS) },
      onClickLabel = "Show commits",
    ) {
      when {
        commits.initialLoading -> SmallSpinner()
        else -> commits.data.firstOrNull()?.createdAt?.takeIf { it.isNotBlank() }?.let {
          Text(it, style = Gl.type.bodySmall, color = c.textSecondary)
        }
      }
    }
    val pipelines = state.pipelines
    if (pipelines.data.isNotEmpty() || pipelines.initialLoading) {
      GlDivider()
      DetailRow(
        icon = Icons.Outlined.PlayCircleOutline,
        title = if (pipelines.loaded) plural(pipelines.data.size, "pipeline", "pipelines") else "Pipelines",
        onClick = { onOpenPage(MrPage.PIPELINES) },
        onClickLabel = "Show pipelines",
      ) {
        if (pipelines.initialLoading) SmallSpinner()
        else pipelines.data.firstOrNull()?.let { PipelineStatusBadge(it.status) }
      }
    }
  }
}

@Composable
private fun StatusRows(
  mr: GitLabMergeRequest,
  state: MergeRequestDetailState,
  canWrite: Boolean,
  onRequestMerge: () -> Unit,
  onSetClosed: (Boolean) -> Unit,
  onOpenPipeline: (GitLabPipeline) -> Unit,
  onOpenUser: (GitLabUser) -> Unit,
) {
  val c = Gl.colors
  var reviewsOpen by rememberSaveable { mutableStateOf(false) }
  val approvals = state.approvals.data
  val approvers = approvals?.approvedBy?.map { it.user }.orEmpty()
  Column {
    val reviewSummary = when {
      approvers.isNotEmpty() -> "Approved by ${approvers.joinToString { it.name.ifBlank { it.username } }}"
      (approvals?.approvalsLeft ?: 0) > 0 -> plural(approvals!!.approvalsLeft!!, "approval required", "approvals required")
      mr.reviewers.isNotEmpty() -> plural(mr.reviewers.size, "reviewer", "reviewers")
      else -> "None requested"
    }
    DetailRow(
      icon = Icons.Outlined.Person,
      title = "Reviews · $reviewSummary",
      onClick = { reviewsOpen = !reviewsOpen },
      onClickLabel = if (reviewsOpen) "Hide reviewers" else "Show reviewers",
    ) {
      Icon(if (reviewsOpen) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = c.textSecondary)
    }
    if (reviewsOpen) {
      Column(Modifier.fillMaxWidth().background(c.bg).padding(start = 56.dp, end = GlSpace.margin, bottom = 12.dp)) {
        MetaRowCompact("Reviewers") { if (mr.reviewers.isEmpty()) MetaValue("None requested") else PeopleList(mr.reviewers, onOpenUser) }
        MetaRowCompact("Approved by") { if (approvers.isEmpty()) MetaValue("No one yet") else PeopleList(approvers, onOpenUser) }
      }
    }
    mr.headPipeline?.let { pipeline ->
      GlDivider()
      val style = pipelineStatusStyle(pipeline.status)
      DetailRow(
        icon = style.icon,
        iconTint = toneColors(style.tone).first,
        title = "Pipeline #${pipeline.id} ${style.label.lowercase(Locale.ROOT)}",
        subtitle = listOfNotNull(pipeline.ref.takeIf { it.isNotBlank() }, pipeline.sha.take(8).takeIf { it.isNotBlank() }).joinToString(" · "),
        onClick = { onOpenPipeline(pipeline) },
        onClickLabel = "Open pipeline",
      ) {
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = c.textTertiary)
      }
    }
    GlDivider()
    when (mr.state) {
      "opened" -> {
        val (headline, detail, tone) = mergeReadiness(mr)
        val (fg, _) = toneColors(tone)
        DetailRow(
          icon = when (tone) {
            Tone.Success -> Icons.Outlined.CheckCircle
            Tone.Danger -> Icons.Outlined.ErrorOutline
            Tone.Warning -> Icons.Outlined.Schedule
            else -> Icons.Outlined.RadioButtonUnchecked
          },
          iconTint = fg,
          title = headline,
          subtitle = detail,
        )
        if (canWrite) {
          val conflicts = mr.hasConflicts == true || mr.detailedMergeStatus == "conflict"
          val blockedReason = when {
            conflicts -> "Resolve conflicts first"
            mr.isDraft -> "Mark as ready first"
            else -> null
          }
          Column(Modifier.fillMaxWidth().background(c.bg).padding(start = GlSpace.margin, end = GlSpace.margin, bottom = GlSpace.margin)) {
            MergeSplitButton(
              enabled = blockedReason == null && !state.busy,
              loading = state.busy,
              onMerge = onRequestMerge,
              onClose = { onSetClosed(true) },
            )
            if (blockedReason != null) {
              Spacer(Modifier.height(6.dp))
              Text(blockedReason, style = Gl.type.meta, color = c.textSecondary)
            }
          }
        }
      }
      "merged" -> {
        val by = (mr.mergedBy ?: mr.mergeUser)?.let { it.name.ifBlank { it.username } }
        DetailRow(
          icon = Icons.Outlined.CallMerge,
          iconTint = c.merged,
          title = "Merged",
          subtitle = listOfNotNull(by?.let { "by $it" }, mr.mergedAt).joinToString(" · ").ifBlank { null },
        )
      }
      "closed" -> {
        val by = mr.closedBy?.let { it.name.ifBlank { it.username } }
        DetailRow(
          icon = Icons.Outlined.Close,
          iconTint = c.danger,
          title = "Closed without merging",
          subtitle = listOfNotNull(by?.let { "by $it" }, mr.closedAt).joinToString(" · ").ifBlank { null },
        )
        if (canWrite) {
          GlButton(
            text = "Reopen merge request",
            onClick = { onSetClosed(false) },
            kind = GlButtonKind.Secondary,
            loading = state.busy,
            modifier = Modifier.fillMaxWidth().background(c.bg).padding(start = GlSpace.margin, end = GlSpace.margin, bottom = GlSpace.margin),
          )
        }
      }
    }
  }
}

/** Headline, explanation and tone for an open merge request, from detailed_merge_status. */
private fun mergeReadiness(mr: GitLabMergeRequest): Triple<String, String?, Tone> {
  if (mr.isDraft) return Triple("Draft", "Mark it as ready before merging.", Tone.Neutral)
  return when (mr.detailedMergeStatus ?: mr.mergeStatus) {
    "mergeable", "can_be_merged" -> Triple("Ready to merge", "Merging can be performed automatically", Tone.Success)
    "conflict", "broken_status", "cannot_be_merged" -> Triple("Merge conflicts", "Resolve the conflicts on GitLab before merging.", Tone.Danger)
    "ci_must_pass", "ci_still_running" -> Triple("Waiting for the pipeline", "The pipeline must succeed before merging.", Tone.Warning)
    "discussions_not_resolved" -> Triple("Unresolved threads", "Resolve every thread before merging.", Tone.Warning)
    "not_approved" -> Triple("Approval required", "This merge request needs approval before merging.", Tone.Warning)
    "checking", "unchecked", "preparing" -> Triple("Checking mergeability", "GitLab is checking whether this can be merged.", Tone.Neutral)
    "need_rebase" -> Triple("Rebase required", "The source branch is behind the target branch.", Tone.Warning)
    "blocked_status" -> Triple("Blocked", "Another merge request must be merged first.", Tone.Warning)
    null -> Triple("Mergeability unknown", null, Tone.Neutral)
    else -> Triple((mr.detailedMergeStatus ?: "").replace('_', ' ').replaceFirstChar { it.uppercase() }, null, Tone.Neutral)
  }
}

/** Green MERGE button with a caret for the other ways to finish a merge request. */
@Composable
private fun MergeSplitButton(enabled: Boolean, loading: Boolean, onMerge: () -> Unit, onClose: () -> Unit) {
  val c = Gl.colors
  var menu by remember { mutableStateOf(false) }
  val shape = RoundedCornerShape(GlRadius.chip)
  val bg = if (enabled) c.successSolid else c.surface3
  val fg = if (enabled) Color.White else c.textTertiary
  Row(
    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(shape).background(bg),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .weight(1f)
        .heightIn(min = 52.dp)
        .pressable(onClick = onMerge, enabled = enabled && !loading, scaleOnPress = false),
      contentAlignment = Alignment.Center,
    ) {
      if (loading) {
        androidx.compose.material3.CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = fg)
      } else {
        Text("MERGE", style = Gl.type.label.copy(fontWeight = FontWeight.SemiBold), color = fg)
      }
    }
    Box(Modifier.width(1.dp).fillMaxHeight().background(Color.Black.copy(alpha = 0.2f)))
    Box {
      Box(
        modifier = Modifier
          .size(width = 60.dp, height = 52.dp)
          .pressable(onClick = { menu = true }, enabled = !loading, scaleOnPress = false, onClickLabel = "More merge options"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "More merge options", tint = fg)
      }
      DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = c.surface2) {
        DropdownMenuItem(
          text = { Text("Merge", style = Gl.type.body, color = if (enabled) c.textPrimary else c.textTertiary) },
          enabled = enabled,
          onClick = { menu = false; onMerge() },
        )
        DropdownMenuItem(
          text = { Text("Close merge request", style = Gl.type.body, color = c.danger) },
          onClick = { menu = false; onClose() },
        )
      }
    }
  }
}

@Composable
private fun MergeButton(label: String, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .heightIn(min = GlSpace.touch)
      .clip(RoundedCornerShape(GlRadius.control))
      .background(Gl.colors.successSolid)
      .pressable(onClick = onClick)
      .padding(horizontal = 18.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(label, style = Gl.type.label, color = Color.White)
  }
}

/** Every changed file with sticky headers, a gutter and red/green lines. */
@Composable
private fun FilesChangedPage(state: MergeRequestDetailState, reference: String, onRetry: () -> Unit, onBack: () -> Unit, modifier: Modifier) {
  val c = Gl.colors
  val diffs = state.diffs
  val stats = remember(diffs.data) { diffStats(diffs.data) }
  val prepared = rememberPreparedDiffs(diffs.data)
  var collapsed by remember(diffs.data) { mutableStateOf(emptySet<Int>()) }
  var viewed by remember(diffs.data) { mutableStateOf(emptySet<Int>()) }
  Column(modifier = modifier.fillMaxSize().background(c.bg)) {
    GlTopBar(
      title = "Files changed",
      subtitle = if (diffs.loaded) "+${grouped(stats.added)} −${grouped(stats.removed)} · ${plural(diffs.data.size, "file", "files")}" else reference,
      onBack = onBack,
    )
    GlDivider()
    val error = diffs.error
    when {
      diffs.initialLoading -> FullScreenState { LoadingState(label = "Loading changes") }
      error != null && diffs.data.isEmpty() -> FullScreenState { ErrorState(error, onRetry) }
      diffs.data.isEmpty() -> FullScreenState { EmptyState(Icons.Outlined.Difference, "No changes", "This merge request doesn't change any files.") }
      else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = GlSpace.group)) {
        diffFileItems(
          diffs = diffs.data,
          parsed = prepared.parsed,
          prepared = prepared.texts,
          collapsed = collapsed,
          viewed = viewed,
          onToggleCollapsed = { i -> collapsed = if (i in collapsed) collapsed - i else collapsed + i },
          onToggleViewed = { i ->
            // A file marked viewed folds away, as on GitHub; unmarking opens it again.
            if (i in viewed) {
              viewed = viewed - i
              collapsed = collapsed - i
            } else {
              viewed = viewed + i
              collapsed = collapsed + i
            }
          },
        )
        item(key = "bottom") { Spacer(Modifier.navigationBarsPadding()) }
      }
    }
  }
}

@Composable
private fun SubPage(title: String, overline: String, onBack: () -> Unit, modifier: Modifier, content: LazyListScope.() -> Unit) {
  Column(modifier = modifier.fillMaxSize().background(Gl.colors.bg)) {
    GlTopBar(title = title, overline = overline, onBack = onBack)
    GlDivider()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = GlSpace.group), content = content)
  }
}

/** Commit in the GitHub manner: title and age on top, the author beneath. */
@Composable
internal fun DetailCommitRow(commit: GitLabCommit, onClick: () -> Unit) {
  val c = Gl.colors
  val author = commit.authorName.ifBlank { "Someone" }
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(role = Role.Button, onClickLabel = "Open commit", onClick = onClick)
      .padding(horizontal = GlSpace.margin, vertical = 12.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        commit.title,
        style = Gl.type.rowTitle.copy(fontWeight = FontWeight.Medium),
        color = c.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Spacer(Modifier.width(12.dp))
      Text(shortAge(commit.createdAt), style = Gl.type.bodySmall, color = c.textSecondary)
    }
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Avatar(null, author, 22.dp)
      Spacer(Modifier.width(8.dp))
      Text(
        text = androidx.compose.ui.text.buildAnnotatedString {
          pushStyle(androidx.compose.ui.text.SpanStyle(color = c.textPrimary, fontWeight = FontWeight.Medium))
          append(author)
          pop()
          append(" authored")
        },
        style = Gl.type.bodySmall,
        color = c.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Text(commit.shortId, style = Gl.type.monoSmall, color = c.textTertiary)
    }
  }
}

// ---------------------------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------------------------

/** "group / project #12" from the API's full reference, or from the web URL when it is missing. */
private fun reference(full: String?, webUrl: String, sigil: String, iid: Long): String {
  val path = full?.substringBeforeLast(sigil)?.takeIf { it.isNotBlank() }
    ?: webUrl.substringAfter("://", "").substringAfter('/', "").substringBefore("/-/").takeIf { it.isNotBlank() }
  return if (path == null) "$sigil$iid" else path.replace("/", " / ") + " $sigil$iid"
}

/** Path above a big bold title, then a row of pills (state, branches). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHeader(reference: String, title: String, pills: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit) {
  val c = Gl.colors
  Column(Modifier.fillMaxWidth().padding(start = GlSpace.margin, end = GlSpace.margin, top = 8.dp, bottom = 20.dp)) {
    Text(reference, style = Gl.type.bodySmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(10.dp))
    Text(
      title,
      style = Gl.type.headline,
      color = c.textPrimary,
      modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(14.dp))
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      content = pills,
    )
  }
}

/** The opening post: author with age, the Markdown body (optionally folded), then [footer]. */
@Composable
private fun AuthorPost(
  author: GitLabUser,
  age: String,
  body: String?,
  context: MarkdownContext,
  onOpenUser: (GitLabUser) -> Unit,
  collapsible: Boolean = false,
  footer: @Composable () -> Unit = {},
) {
  val c = Gl.colors
  Column(Modifier.fillMaxWidth().padding(bottom = GlSpace.margin)) {
    AuthorLine(author, age, badge = null, onOpenUser = onOpenUser)
    Box(Modifier.padding(horizontal = GlSpace.margin)) {
      if (body.isNullOrBlank()) {
        Text("No description provided.", style = Gl.type.body, color = c.textTertiary)
      } else if (collapsible && isLong(body)) {
        CollapsibleMarkdown(body, context)
      } else {
        MarkdownText(body, context = context)
      }
    }
    Column(Modifier.padding(horizontal = GlSpace.margin), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Spacer(Modifier.height(4.dp))
      footer()
    }
  }
}

private fun isLong(text: String) = text.length > 700 || text.count { it == '\n' } > 14

/** Long descriptions fold to a preview with a fade and a "Read more" pill. */
@Composable
private fun CollapsibleMarkdown(body: String, context: MarkdownContext) {
  val c = Gl.colors
  var expanded by rememberSaveable(body.hashCode()) { mutableStateOf(false) }
  if (expanded) {
    Column {
      MarkdownText(body, context = context)
      Spacer(Modifier.height(8.dp))
      ReadMorePill("Show less", Icons.Outlined.ExpandLess, Modifier.align(Alignment.CenterHorizontally)) { expanded = false }
    }
    return
  }
  Box {
    Box(Modifier.heightIn(max = 260.dp).clip(RoundedCornerShape(0.dp))) {
      MarkdownText(body, context = context)
    }
    Box(
      Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .height(96.dp)
        .background(Brush.verticalGradient(listOf(Color.Transparent, c.bg))),
    )
    ReadMorePill("Read more", Icons.Outlined.KeyboardArrowDown, Modifier.align(Alignment.BottomCenter)) { expanded = true }
  }
}

@Composable
private fun ReadMorePill(text: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
  val c = Gl.colors
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(50))
      .background(c.surface2)
      .border(1.dp, c.borderStrong, RoundedCornerShape(50))
      .pressable(onClick = onClick)
      .heightIn(min = 44.dp)
      .padding(horizontal = 20.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(icon, contentDescription = null, tint = c.textPrimary, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(text, style = Gl.type.label, color = c.textPrimary)
  }
}

/** Avatar, bold name, "· 3h", and an optional role badge ("Author"). */
@Composable
private fun AuthorLine(author: GitLabUser, age: String, badge: String?, onOpenUser: (GitLabUser) -> Unit, avatar: androidx.compose.ui.unit.Dp = 44.dp) {
  val c = Gl.colors
  val name = author.name.ifBlank { author.username }.ifBlank { "Someone" }
  val openable = author.username.isNotBlank() || author.id > 0
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .then(if (openable) Modifier.clickable(role = Role.Button, onClickLabel = "Open profile of $name") { onOpenUser(author) } else Modifier)
      .padding(horizontal = GlSpace.margin, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Avatar(author.avatarUrl, name, avatar)
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          author.username.ifBlank { name },
          style = Gl.type.rowTitle.copy(fontWeight = FontWeight.SemiBold),
          color = c.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
        )
        if (age.isNotBlank()) {
          Text(" · ${shortAge(age)}", style = Gl.type.bodySmall, color = c.textSecondary, maxLines = 1)
        }
      }
      if (badge != null) {
        Spacer(Modifier.height(4.dp))
        RoleBadge(badge)
      }
    }
  }
}

@Composable
private fun RoleBadge(text: String) {
  val c = Gl.colors
  Text(
    text,
    style = Gl.type.badge,
    color = c.textSecondary,
    modifier = Modifier
      .clip(RoundedCornerShape(50))
      .background(c.surface2)
      .border(1.dp, c.border, RoundedCornerShape(50))
      .padding(horizontal = 10.dp, vertical = 3.dp),
  )
}

@Composable
private fun Reactions(up: Int, down: Int) {
  if (up <= 0 && down <= 0) return
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    if (up > 0) DetailReactionChip("👍", up, plural(up, "thumbs up", "thumbs up"))
    if (down > 0) DetailReactionChip("👎", down, plural(down, "thumbs down", "thumbs down"))
  }
}

@Composable
private fun TaskProgress(done: Int, total: Int) {
  val c = Gl.colors
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text("$done of $total tasks completed", style = Gl.type.meta, color = c.textSecondary)
    LinearProgressIndicator(
      progress = { done.toFloat() / total },
      modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
      color = c.success,
      trackColor = c.surface3,
    )
  }
}

@Composable
private fun BusyDot() {
  androidx.compose.material3.CircularProgressIndicator(
    modifier = Modifier.size(16.dp).semantics { contentDescription = "Updating" },
    strokeWidth = 2.dp,
    color = Gl.colors.textSecondary,
  )
}

@Composable
private fun SmallSpinner() {
  androidx.compose.material3.CircularProgressIndicator(
    modifier = Modifier.size(16.dp).semantics { contentDescription = "Loading" },
    strokeWidth = 2.dp,
    color = Gl.colors.textTertiary,
  )
}

@Composable
private fun DiffCounts(added: Int, removed: Int) {
  val c = Gl.colors
  Row(Modifier.semantics(mergeDescendants = true) { contentDescription = "$added lines added, $removed removed" }) {
    Text("+${grouped(added)}", style = Gl.type.numeric.copy(fontSize = Gl.type.rowTitle.fontSize), color = c.success)
    Spacer(Modifier.width(6.dp))
    Text("−${grouped(removed)}", style = Gl.type.numeric.copy(fontSize = Gl.type.rowTitle.fontSize), color = c.danger)
  }
}

private fun grouped(value: Int): String = String.format(Locale.US, "%,d", value)

private fun plural(n: Int, one: String, many: String) = if (n == 1) "1 $one" else "${grouped(n)} $many"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoSheet(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
  val c = Gl.colors
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = c.surface,
  ) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = GlSpace.margin)) {
      Text(
        title,
        style = Gl.type.title,
        color = c.textPrimary,
        modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = 8.dp).semantics { heading() },
      )
      content()
    }
  }
}

@Composable
private fun MetaRow(label: String, content: @Composable () -> Unit) {
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
  GlDivider(inset = GlSpace.margin)
}

@Composable
private fun MetaRowCompact(label: String, content: @Composable () -> Unit) {
  Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(label, style = Gl.type.meta, color = Gl.colors.textSecondary)
    content()
  }
}

@Composable
private fun MetaValue(text: String) {
  Text(text, style = Gl.type.body, color = Gl.colors.textPrimary)
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

// ---------------------------------------------------------------------------------------------
// Timeline
// ---------------------------------------------------------------------------------------------

/** Left edge of the timeline rail, centred on the event icons. */
private val RailX = 40.dp

/**
 * Comments and events in order on a darker band, joined by a vertical rail like GitHub's
 * timeline: events are one line with an icon on the rail, comments are full posts.
 */
private fun LazyListScope.timeline(
  notes: Loadable<List<GitLabNote>>,
  labels: Map<String, GitLabLabel>,
  opener: GitLabUser,
  context: MarkdownContext,
  onOpenUser: (GitLabUser) -> Unit,
  onRetry: () -> Unit,
  guest: Boolean = false,
) {
  val error = notes.error
  when {
    notes.initialLoading -> item(key = "notes-loading") { Box(Modifier.background(Gl.colors.sunken)) { LoadingState(label = "Loading activity") } }
    // GitLab.com hides comments from anonymous requests; for a guest that's expected, not an error.
    guest && error != null && notes.data.isEmpty() -> item(key = "notes-guest") {
      Row(
        modifier = Modifier.fillMaxWidth().background(Gl.colors.sunken).padding(horizontal = GlSpace.margin, vertical = GlSpace.margin),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Gl.colors.textTertiary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Sign in to see comments and activity.", style = Gl.type.bodySmall, color = Gl.colors.textSecondary)
      }
    }
    error != null && notes.data.isEmpty() -> item(key = "notes-error") {
      Box(Modifier.fillMaxWidth().background(Gl.colors.sunken).padding(GlSpace.margin)) { InlineError(error, onRetry = onRetry) }
    }
    notes.data.isEmpty() -> item(key = "notes-empty") {
      Row(
        modifier = Modifier.fillMaxWidth().background(Gl.colors.sunken).padding(horizontal = GlSpace.margin, vertical = GlSpace.margin),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Outlined.ChatBubbleOutline, null, tint = Gl.colors.textTertiary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("No comments yet", style = Gl.type.bodySmall, color = Gl.colors.textSecondary)
      }
    }
    else -> {
      if (error != null) {
        item(key = "notes-inline-error") {
          Box(Modifier.fillMaxWidth().background(Gl.colors.sunken).padding(GlSpace.margin)) { InlineError(error, onRetry = onRetry) }
        }
      }
      item(key = "rail-top") { RailGap() }
      items(notes.data, key = { "note-${it.id}" }) { note ->
        if (note.system) SystemEvent(note, labels) else {
          CommentPost(note, isOpener = note.author.id != 0L && note.author.id == opener.id, context = context, onOpenUser = onOpenUser)
          RailGap()
        }
      }
      item(key = "rail-end") { Box(Modifier.fillMaxWidth().height(GlSpace.margin).background(Gl.colors.sunken)) }
    }
  }
}

@Composable
private fun RailGap() {
  Box(Modifier.fillMaxWidth().height(16.dp).background(Gl.colors.sunken)) {
    Box(Modifier.padding(start = RailX).width(2.dp).fillMaxHeight().background(Gl.colors.border))
  }
}

@Composable
private fun CommentPost(note: GitLabNote, isOpener: Boolean, context: MarkdownContext, onOpenUser: (GitLabUser) -> Unit) {
  val c = Gl.colors
  Column(Modifier.fillMaxWidth().background(c.bg)) {
    GlDivider()
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.weight(1f)) {
        AuthorLine(note.author, note.createdAt, badge = if (isOpener) "Author" else null, onOpenUser = onOpenUser, avatar = 36.dp)
      }
      Row(Modifier.padding(end = GlSpace.margin), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (note.internal == true) GlBadge("Internal", tone = Tone.Warning, icon = Icons.Outlined.Lock)
        if (note.type == "DiffNote") GlBadge("On the diff")
        if (note.resolved == true) GlBadge("Resolved", tone = Tone.Success)
      }
    }
    Box(Modifier.padding(start = GlSpace.margin, end = GlSpace.margin, bottom = GlSpace.margin)) {
      MarkdownText(note.body, context = context)
    }
    GlDivider()
  }
}

private val LABEL_TOKEN = Regex("""~"[^"]+"|~[^\s,]+""")

/** "CBoYXD added the [bug] label · 3h" with the event's icon on the rail. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SystemEvent(note: GitLabNote, labels: Map<String, GitLabLabel>) {
  val c = Gl.colors
  val name = note.author.username.ifBlank { note.author.name }
  val line = note.body.lineSequence().firstOrNull().orEmpty().replace("**", "").replace("`", "")
  val icon = systemIcon(line)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(IntrinsicSize.Min)
      .background(c.sunken)
      .semantics(mergeDescendants = true) { contentDescription = "$name $line, ${note.createdAt}" },
  ) {
    Box(Modifier.width(RailX * 2 + 2.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
      Box(Modifier.align(Alignment.TopStart).padding(start = RailX).width(2.dp).fillMaxHeight().background(c.border))
      Box(
        Modifier.size(32.dp).clip(CircleShape).background(c.sunken),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
      }
    }
    FlowRow(
      modifier = Modifier.weight(1f).padding(end = GlSpace.margin, top = 10.dp, bottom = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(name, style = Gl.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.textPrimary)
      var rest = line
      var shown = 0
      while (rest.isNotEmpty() && shown < 40) {
        val match = LABEL_TOKEN.find(rest)
        val before = if (match == null) rest else rest.substring(0, match.range.first)
        before.split(' ').filter { it.isNotBlank() }.forEach { word ->
          Text(word, style = Gl.type.body, color = c.textSecondary)
          shown++
        }
        if (match == null) break
        val token = match.value.removePrefix("~").trim('"')
        val label = labels[token] ?: token.toLongOrNull()?.let { id -> labels.values.firstOrNull { it.id == id } }
        if (label != null) ColoredLabel(label.name, label) else if (token.toLongOrNull() == null) ColoredLabel(token, null)
        shown++
        rest = rest.substring(match.range.last + 1)
      }
      Text("· ${shortAge(note.createdAt)}", style = Gl.type.body, color = c.textTertiary)
    }
  }
}

private fun systemIcon(body: String): ImageVector {
  val text = body.lowercase(Locale.ROOT)
  return when {
    "label" in text -> Icons.Outlined.LocalOffer
    "assigned" in text || "review" in text -> Icons.Outlined.Person
    "reopened" in text -> Icons.Outlined.RadioButtonUnchecked
    "closed" in text -> Icons.Outlined.TaskAlt
    "merged" in text -> Icons.Outlined.CallMerge
    "milestone" in text -> Icons.Outlined.Flag
    "commit" in text -> Icons.Outlined.Commit
    "branch" in text -> Icons.Outlined.AccountTree
    "mentioned" in text || "marked" in text && "related" in text -> Icons.Outlined.Link
    "changed" in text || "edited" in text || "title" in text -> Icons.Outlined.Edit
    "locked" in text || "confidential" in text -> Icons.Outlined.Lock
    else -> Icons.Outlined.History
  }
}

/** Comment editor in a sheet over the page; it closes itself once the comment is posted. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentSheet(
  posting: Boolean,
  previewContext: MarkdownContext,
  onDismiss: () -> Unit,
  onSubmit: (String, (String?) -> Unit) -> Unit,
) {
  val c = Gl.colors
  var draft by remember { mutableStateOf(TextFieldValue("")) }
  var error by remember { mutableStateOf<String?>(null) }
  var emptyError by remember { mutableStateOf(false) }
  ModalBottomSheet(
    onDismissRequest = { if (!posting) onDismiss() },
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { !posting }),
    containerColor = c.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .imePadding()
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = GlSpace.margin, vertical = GlSpace.tight),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Comment", style = Gl.type.title, color = c.textPrimary, modifier = Modifier.semantics { heading() })
      MarkdownEditor(
        value = draft,
        onValueChange = { draft = it; emptyError = false },
        label = "Add a comment",
        minLines = 4,
        maxLines = 12,
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
              if (result == null) {
                draft = TextFieldValue("")
                onDismiss()
              } else {
                error = result
              }
            }
          }
        },
      )
    }
  }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
  focusedBorderColor = Gl.colors.accent,
  unfocusedBorderColor = Gl.colors.borderStrong,
  focusedLabelColor = Gl.colors.accent,
  unfocusedLabelColor = Gl.colors.textSecondary,
  cursorColor = Gl.colors.accent,
  errorBorderColor = Gl.colors.danger,
  errorLabelColor = Gl.colors.danger,
  errorSupportingTextColor = Gl.colors.danger,
)
