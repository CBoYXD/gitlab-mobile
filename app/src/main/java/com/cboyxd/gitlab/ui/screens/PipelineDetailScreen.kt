package com.cboyxd.gitlab.ui.screens

import androidx.compose.ui.semantics.heading
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.data.api.formatGitLabTime
import com.cboyxd.gitlab.data.model.GitLabJob
import com.cboyxd.gitlab.data.model.GitLabPipeline
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.FullScreenState
import com.cboyxd.gitlab.ui.components.GlBadge
import com.cboyxd.gitlab.ui.components.GlButton
import com.cboyxd.gitlab.ui.components.GlButtonKind
import com.cboyxd.gitlab.ui.components.GlCard
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlIconAction
import com.cboyxd.gitlab.ui.components.GlTopBar
import com.cboyxd.gitlab.ui.components.InlineError
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.components.MonoPill
import com.cboyxd.gitlab.ui.components.PipelineStatusBadge
import com.cboyxd.gitlab.ui.components.StateIcon
import com.cboyxd.gitlab.ui.components.formatDuration
import com.cboyxd.gitlab.ui.components.pipelineStatusStyle
import com.cboyxd.gitlab.ui.components.pressable
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.JobDetailState
import com.cboyxd.gitlab.ui.viewmodel.PipelineDetailState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ACTIVE_STATUSES = setOf("running", "pending", "created", "preparing", "waiting_for_resource", "scheduled", "canceling")

// ---------------------------------------------------------------------------------------------
// Pipeline detail
// ---------------------------------------------------------------------------------------------

@Composable
fun PipelineDetailScreen(
  state: PipelineDetailState,
  canWrite: Boolean,
  onBack: () -> Unit,
  onRetryLoad: () -> Unit,
  onRetryPipeline: (GitLabPipeline) -> Unit,
  onCancel: () -> Unit,
  onOpenJob: (GitLabJob) -> Unit,
  onOpenCommit: (String) -> Unit,
  onOpenInBrowser: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val pipeline = state.pipeline.data
  var confirmCancel by rememberSaveable { mutableStateOf(false) }

  val listState = rememberLazyListState()
  val scrolledPastTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
  Column(modifier = modifier.fillMaxSize().background(Gl.colors.bg)) {
    val url = pipeline?.webUrl?.takeIf { it.isNotBlank() }
    GlTopBar(
      title = "Pipeline #${state.pipelineId}",
      overline = pipeline?.ref?.takeIf { it.isNotBlank() },
      showTitle = scrolledPastTitle || pipeline == null,
      onBack = onBack,
    ) {
      DetailBarActions(
        webUrl = url,
        title = "Pipeline #${state.pipelineId}",
        menu = listOfNotNull(url?.let { DetailMenuItem("Open in browser", { onOpenInBrowser(it) }) }),
      )
    }
    if (scrolledPastTitle) GlDivider()

    when {
      pipeline == null && state.pipeline.error != null -> FullScreenState { ErrorState(state.pipeline.error, onRetryLoad) }
      pipeline == null -> FullScreenState { LoadingState(label = "Loading pipeline") }
      else -> {
        val jobsByStage = remember(state.jobs.data) { groupByStage(state.jobs.data) }
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = GlSpace.group),
        ) {
          item(key = "header") {
            PipelineHeader(
              pipeline = pipeline,
              canWrite = canWrite,
              busy = state.busy,
              onRetry = { onRetryPipeline(pipeline) },
              onCancel = { confirmCancel = true },
              onOpenCommit = onOpenCommit,
            )
          }
          if (state.pipeline.error != null) {
            item(key = "refresh-error") {
              InlineError(
                message = state.pipeline.error,
                onRetry = onRetryLoad,
                modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = GlSpace.tight),
              )
            }
          }
          when {
            state.jobs.initialLoading -> item(key = "jobs-loading") { LoadingState(label = "Loading jobs") }
            state.jobs.error != null && state.jobs.data.isEmpty() -> item(key = "jobs-error") {
              InlineError(
                message = state.jobs.error,
                onRetry = onRetryLoad,
                modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = GlSpace.tight),
              )
            }
            jobsByStage.isEmpty() -> item(key = "jobs-empty") {
              Text(
                text = "This pipeline has no jobs.",
                style = Gl.type.bodySmall,
                color = Gl.colors.textSecondary,
                modifier = Modifier.padding(GlSpace.margin),
              )
            }
            else -> jobsByStage.forEach { (stage, jobs) ->
              item(key = "stage-$stage") { StageHeader(stage, jobs) }
              item(key = "stage-jobs-$stage") {
                Column(Modifier.background(Gl.colors.bg)) {
                  jobs.forEachIndexed { index, job ->
                    if (index > 0) GlDivider(inset = 52.dp)
                    JobRow(job = job, onClick = { onOpenJob(job) })
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (confirmCancel && pipeline != null) {
    AlertDialog(
      onDismissRequest = { confirmCancel = false },
      title = { Text("Cancel pipeline #${pipeline.id}?") },
      text = { Text("Running jobs stop and pending jobs won't start. You can retry the pipeline later.") },
      confirmButton = {
        GlButton(
          text = "Cancel pipeline",
          kind = GlButtonKind.Danger,
          onClick = {
            confirmCancel = false
            onCancel()
          },
        )
      },
      dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep running", color = Gl.colors.textPrimary) } },
      containerColor = Gl.colors.surface,
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PipelineHeader(
  pipeline: GitLabPipeline,
  canWrite: Boolean,
  busy: Boolean,
  onRetry: () -> Unit,
  onCancel: () -> Unit,
  onOpenCommit: (String) -> Unit,
) {
  val c = Gl.colors
  val active = pipeline.status in ACTIVE_STATUSES
  Column(Modifier.fillMaxWidth()) {
    Column(Modifier.fillMaxWidth().padding(start = GlSpace.margin, end = GlSpace.margin, top = 8.dp, bottom = 20.dp)) {
      Text("Pipeline #${pipeline.id}", style = Gl.type.headline, color = c.textPrimary, modifier = Modifier.semantics { heading() })
      Spacer(Modifier.height(14.dp))
      FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailStatePill(pipelineStatusStyle(pipeline.status))
        if (pipeline.ref.isNotBlank()) DetailRefPill(pipeline.ref, Modifier.align(Alignment.CenterVertically))
        if (active) {
          CircularProgressIndicator(
            modifier = Modifier.size(16.dp).align(Alignment.CenterVertically).semantics { contentDescription = "Refreshes while running" },
            strokeWidth = 2.dp,
            color = c.textTertiary,
          )
        }
      }
      pipeline.yamlErrors?.takeIf { it.isNotBlank() }?.let { errors ->
        Spacer(Modifier.height(12.dp))
        InlineError(message = "Configuration error: $errors")
      }
      val canRetry = pipeline.status in setOf("failed", "canceled")
      val canCancel = pipeline.status in setOf("running", "pending", "created", "preparing", "waiting_for_resource", "scheduled")
      if (canWrite && (canRetry || canCancel)) {
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(GlSpace.tight)) {
          if (canRetry) GlButton(text = "Retry", onClick = onRetry, kind = GlButtonKind.Primary, loading = busy, icon = Icons.Outlined.Refresh)
          if (canCancel) GlButton(text = "Cancel", onClick = onCancel, kind = GlButtonKind.Secondary, loading = busy)
        }
      }
    }
    if (pipeline.sha.isNotBlank()) {
      GlDivider()
      DetailRow(
        icon = Icons.Outlined.Commit,
        title = "Commit ${pipeline.sha.take(8)}",
        onClick = { onOpenCommit(pipeline.sha) },
        onClickLabel = "Open commit",
      ) {
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = c.textTertiary)
      }
    }
    val trigger = listOfNotNull(
      pipeline.source?.let { "Triggered by ${humanSource(it)}" },
      pipeline.user?.username?.takeIf { it.isNotBlank() }?.let { "@$it" },
    ).joinToString(" · ")
    if (trigger.isNotBlank() || pipeline.createdAt.isNotBlank()) {
      GlDivider()
      DetailRow(icon = Icons.Outlined.PlayCircleOutline, title = trigger.ifBlank { "Started" }, subtitle = pipeline.createdAt.takeIf { it.isNotBlank() })
    }
    val timing = listOfNotNull(
      pipeline.duration?.toLong()?.takeIf { it > 0 }?.let { "Ran for ${formatDuration(it)}" },
      pipeline.queuedDuration?.toLong()?.takeIf { it > 0 }?.let { "queued ${formatDuration(it)}" },
    )
    if (timing.isNotEmpty()) {
      GlDivider()
      DetailRow(icon = Icons.Outlined.Timer, title = timing.joinToString(" · "))
    }
  }
}

@Composable
private fun StageHeader(stage: String, jobs: List<GitLabJob>) {
  val style = pipelineStatusStyle(stageStatusOf(jobs))
  DetailSectionHeader(
    title = stage.replaceFirstChar { it.uppercase() },
    modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Stage $stage, ${style.label}" },
  ) {
    Text(
      "${jobs.size} ${if (jobs.size == 1) "job" else "jobs"}",
      style = Gl.type.bodySmall,
      color = Gl.colors.textSecondary,
    )
    Spacer(Modifier.width(8.dp))
    StateIcon(style)
    Spacer(Modifier.width(12.dp))
  }
}

@Composable
private fun JobRow(job: GitLabJob, onClick: () -> Unit) {
  val style = pipelineStatusStyle(job.status)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open job")
      .defaultMinSize(minHeight = 56.dp)
      .padding(start = GlSpace.margin, end = 8.dp, top = 10.dp, bottom = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    StateIcon(style)
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(job.name, style = Gl.type.rowTitle, color = Gl.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
      val meta = listOfNotNull(
        style.label,
        job.duration?.toLong()?.takeIf { it > 0 }?.let { formatDuration(it) },
      ).joinToString(" · ")
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(meta, style = Gl.type.meta.copy(fontFeatureSettings = "tnum"), color = Gl.colors.textSecondary)
        if (job.allowFailure && job.status == "failed") {
          Spacer(Modifier.width(6.dp))
          GlBadge("Allowed to fail")
        }
      }
    }
    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Gl.colors.textTertiary)
  }
}

private fun groupByStage(jobs: List<GitLabJob>): List<Pair<String, List<GitLabJob>>> {
  val grouped = LinkedHashMap<String, MutableList<GitLabJob>>()
  jobs.forEach { job -> grouped.getOrPut(job.stage.ifBlank { "other" }) { mutableListOf() }.add(job) }
  return grouped.map { it.key to it.value }
}

private fun stageStatusOf(jobs: List<GitLabJob>): String {
  val statuses = jobs.map { it.status }
  return when {
    statuses.any { it == "failed" } && jobs.any { it.status == "failed" && !it.allowFailure } -> "failed"
    statuses.any { it == "running" } -> "running"
    statuses.any { it in setOf("pending", "created", "preparing", "scheduled", "waiting_for_resource") } -> "pending"
    statuses.all { it == "skipped" } -> "skipped"
    statuses.any { it == "canceled" } -> "canceled"
    statuses.any { it == "manual" } && statuses.none { it == "success" } -> "manual"
    statuses.any { it == "success" || it == "failed" } -> "success"
    else -> statuses.firstOrNull().orEmpty()
  }
}

private fun humanSource(source: String): String = when (source) {
  "push" -> "a push"
  "merge_request_event" -> "a merge request"
  "schedule" -> "a schedule"
  "web" -> "the web interface"
  "api" -> "the API"
  "trigger" -> "a trigger token"
  "pipeline" -> "another pipeline"
  "parent_pipeline" -> "a parent pipeline"
  "chat" -> "chat"
  "external" -> "an external service"
  else -> source.replace('_', ' ')
}

// ---------------------------------------------------------------------------------------------
// Job detail
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JobDetailScreen(
  state: JobDetailState,
  canWrite: Boolean,
  onBack: () -> Unit,
  onRetryLoad: () -> Unit,
  onRetryJob: () -> Unit,
  onOpenInBrowser: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val job = state.job.data
  val c = Gl.colors
  Column(modifier = modifier.fillMaxSize().background(c.bg)) {
    GlTopBar(
      title = job?.name ?: "Job #${state.jobId}",
      subtitle = listOfNotNull("Job #${state.jobId}", job?.stage?.takeIf { it.isNotBlank() }).joinToString(" · "),
      onBack = onBack,
    ) {
      val url = job?.webUrl?.takeIf { it.isNotBlank() }
      if (url != null) GlIconAction(Icons.AutoMirrored.Outlined.OpenInNew, "Open in browser", onClick = { onOpenInBrowser(url) })
    }
    GlDivider()
    when {
      job == null && state.job.error != null -> FullScreenState { ErrorState(state.job.error, onRetryLoad) }
      job == null -> FullScreenState { LoadingState(label = "Loading job") }
      else -> {
        Column(modifier = Modifier.padding(GlSpace.margin)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            PipelineStatusBadge(job.status)
            job.duration?.toLong()?.takeIf { it > 0 }?.let {
              Spacer(Modifier.width(GlSpace.tight))
              Text(formatDuration(it), style = Gl.type.numeric, color = c.textSecondary)
            }
            if (job.allowFailure) {
              Spacer(Modifier.width(GlSpace.tight))
              GlBadge("Allowed to fail")
            }
            Spacer(Modifier.weight(1f))
            if (job.status in ACTIVE_STATUSES) {
              CircularProgressIndicator(
                modifier = Modifier.size(14.dp).semantics { contentDescription = "Refreshes while running" },
                strokeWidth = 1.5.dp,
                color = c.textTertiary,
              )
            }
          }
          val facts = listOfNotNull(
            job.runner?.description?.takeIf { it.isNotBlank() }?.let { "Runner $it" },
            job.startedAt?.takeIf { it.isNotBlank() }?.let { "started ${formatGitLabTime(it)}" },
            job.finishedAt?.takeIf { it.isNotBlank() }?.let { "finished ${formatGitLabTime(it)}" },
            job.coverage?.let { "coverage ${"%.1f".format(it)}%" },
          )
          if (facts.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(facts.joinToString(" · "), style = Gl.type.meta, color = c.textSecondary)
          }
          if (job.status == "failed" && !job.failureReason.isNullOrBlank()) {
            Spacer(Modifier.height(GlSpace.tight))
            InlineError(message = humanFailure(job.failureReason))
          }
          if (canWrite && job.status in setOf("failed", "canceled", "success")) {
            Spacer(Modifier.height(GlSpace.tight))
            GlButton(text = "Retry job", onClick = onRetryJob, kind = GlButtonKind.Secondary, loading = state.busy, icon = Icons.Outlined.Refresh)
          }
        }
        GlDivider()
        JobLog(state = state, onRetryLoad = onRetryLoad, modifier = Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun JobLog(state: JobDetailState, onRetryLoad: () -> Unit, modifier: Modifier = Modifier) {
  val c = Gl.colors
  val trace = state.trace.data
  when {
    trace == null && state.trace.error != null -> Box(modifier.fillMaxWidth()) { ErrorState(state.trace.error, onRetryLoad) }
    trace == null -> Box(modifier.fillMaxWidth()) { LoadingState(label = "Loading log") }
    trace.text.isBlank() -> Box(modifier.fillMaxWidth().padding(GlSpace.margin)) {
      Text("This job hasn't written any output yet.", style = Gl.type.bodySmall, color = c.textSecondary)
    }
    else -> {
      val lines by produceState<List<JobTraceLine>?>(null, state.jobId, trace.text) {
        value = withContext(Dispatchers.Default) { parseJobTrace(trace.text) }
      }
      val parsed = lines
      if (parsed == null) {
        Box(modifier.fillMaxWidth()) { LoadingState(label = "Preparing log") }
        return
      }
      var overrides by remember(state.jobId) { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
      val collapsed = remember(parsed, overrides) {
        parsed.filter { it.sectionKey != null && (overrides[it.sectionKey] ?: it.initiallyCollapsed) }.mapNotNull { it.sectionKey }.toSet()
      }
      val visible = remember(parsed, collapsed) { parsed.filter { line -> line.parents.none { it in collapsed } } }
      var timestamps by rememberSaveable(state.jobId) { mutableStateOf(false) }
      var wrap by rememberSaveable(state.jobId) { mutableStateOf(true) }
      val listState = rememberLazyListState()
      val scope = rememberCoroutineScope()
      val total = visible.size + if (trace.truncated) 1 else 0
      var scrolledInitially by remember(state.jobId) { mutableStateOf(false) }
      LaunchedEffect(state.jobId, total) {
        if (!scrolledInitially && total > 0) {
          listState.scrollToItem(total - 1)
          scrolledInitially = true
        }
      }
      val style = Gl.type.monoSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp)
      val measurer = rememberTextMeasurer()
      val charWidth = remember(style) { measurer.measure("0", style).size.width.toFloat() }
      val longest = remember(parsed) { (parsed.maxOfOrNull { it.text.length } ?: 0).coerceAtMost(600) }
      val contentWidth = with(LocalDensity.current) { (charWidth * (longest + 8 + if (timestamps) 14 else 0)).toDp() } + 32.dp
      val numberWidth = with(LocalDensity.current) { (charWidth * ((parsed.lastOrNull()?.number ?: 0).toString().length + 1)).toDp() }
      Column(modifier.fillMaxWidth().background(JobConsoleBackground)) {
        Row(
          Modifier.fillMaxWidth().background(Color(0xFF242329)).padding(start = 16.dp, end = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Job log", style = Gl.type.label, color = JobConsoleForeground)
          Spacer(Modifier.weight(1f))
          TextButton(onClick = { timestamps = !timestamps }, modifier = Modifier.semantics { contentDescription = if (timestamps) "Hide timestamps" else "Show timestamps" }) {
            Text("Time", style = Gl.type.meta, color = if (timestamps) Color(0xFF5797FF) else JobConsoleMuted)
          }
          TextButton(onClick = { wrap = !wrap }, modifier = Modifier.semantics { contentDescription = if (wrap) "Disable line wrapping" else "Wrap lines" }) {
            Text("Wrap", style = Gl.type.meta, color = if (wrap) Color(0xFF5797FF) else JobConsoleMuted)
          }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
          val viewportWidth = maxWidth
          val horizontal = rememberScrollState()
          Box(Modifier.fillMaxSize().then(if (wrap) Modifier else Modifier.horizontalScroll(horizontal))) {
            LazyColumn(
              state = listState,
              modifier = Modifier.width(if (wrap) viewportWidth else contentWidth.coerceAtLeast(viewportWidth)).fillMaxHeight(),
              contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp),
            ) {
              if (trace.truncated) {
                item(key = "truncated") {
                  Text("Showing the last 256 KB. Open in the browser for the full log.", style = Gl.type.meta, color = JobConsoleMuted, modifier = Modifier.padding(16.dp))
                }
              }
              itemsIndexed(visible, key = { _, line -> line.number }) { _, line ->
                val section = line.sectionKey
                val isCollapsed = section in collapsed
                Row(
                  modifier = Modifier.fillMaxWidth()
                    .then(if (section != null) Modifier.background(Color(0xFF242329)).clickable {
                      overrides = overrides + (section to !isCollapsed)
                    }.semantics { contentDescription = if (isCollapsed) "Expand log section" else "Collapse log section" } else Modifier)
                    .padding(horizontal = 12.dp, vertical = if (section != null) 6.dp else 1.dp),
                  verticalAlignment = Alignment.Top,
                ) {
                  Text(line.number.toString(), style = style, color = JobConsoleMuted, textAlign = TextAlign.End, modifier = Modifier.width(numberWidth))
                  Spacer(Modifier.width(12.dp))
                  if (timestamps) {
                    Text(line.timestamp.orEmpty(), style = style, color = JobConsoleMuted, modifier = Modifier.width(100.dp))
                    Spacer(Modifier.width(8.dp))
                  }
                  if (section != null) {
                    Text(if (isCollapsed) "▸ " else "▾ ", style = style, color = JobConsoleMuted)
                  }
                  SelectionContainer(Modifier.weight(1f)) {
                    Text(
                      text = if (line.text.isEmpty()) AnnotatedString(" ") else line.text,
                      style = if (section != null) style.copy(fontWeight = FontWeight.Bold) else style,
                      color = JobConsoleForeground,
                      softWrap = wrap,
                    )
                  }
                }
              }
            }
          }
          val atEnd by remember { derivedStateOf { !listState.canScrollForward } }
          if (!atEnd) GlButton(
            text = "Jump to end",
            icon = Icons.Outlined.KeyboardDoubleArrowDown,
            kind = GlButtonKind.Secondary,
            onClick = { scope.launch { if (total > 0) listState.animateScrollToItem(total - 1) } },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(GlSpace.margin),
          )
        }
      }
    }
  }
}

private fun humanFailure(reason: String): String = when (reason) {
  "script_failure" -> "The job script exited with an error. Check the log below."
  "runner_system_failure" -> "The runner had a system failure. Retry the job."
  "stuck_or_timeout_failure" -> "The job timed out or got stuck. Retry it or raise the timeout."
  "api_failure" -> "GitLab had an API failure while running the job. Retry it."
  "missing_dependency_failure" -> "A job this one depends on has expired artifacts. Rerun the earlier job first."
  "runner_unsupported" -> "The runner doesn't support this job. Check the runner's version."
  "scheduler_failure" -> "The job couldn't be scheduled. Retry it."
  "data_integrity_failure" -> "GitLab found a data integrity problem. Retry the job."
  "unmet_prerequisites" -> "The job's prerequisites weren't met. Check its configuration."
  "job_execution_timeout" -> "The job ran longer than its timeout."
  else -> reason.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
