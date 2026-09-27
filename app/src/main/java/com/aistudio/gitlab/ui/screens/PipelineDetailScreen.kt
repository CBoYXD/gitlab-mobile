package com.aistudio.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.aistudio.gitlab.data.api.formatGitLabTime
import com.aistudio.gitlab.data.model.GitLabJob
import com.aistudio.gitlab.data.model.GitLabPipeline
import com.aistudio.gitlab.ui.components.ErrorState
import com.aistudio.gitlab.ui.components.FullScreenState
import com.aistudio.gitlab.ui.components.GlBadge
import com.aistudio.gitlab.ui.components.GlButton
import com.aistudio.gitlab.ui.components.GlButtonKind
import com.aistudio.gitlab.ui.components.GlCard
import com.aistudio.gitlab.ui.components.GlDivider
import com.aistudio.gitlab.ui.components.GlIconAction
import com.aistudio.gitlab.ui.components.GlTopBar
import com.aistudio.gitlab.ui.components.InlineError
import com.aistudio.gitlab.ui.components.LoadingState
import com.aistudio.gitlab.ui.components.MonoPill
import com.aistudio.gitlab.ui.components.PipelineStatusBadge
import com.aistudio.gitlab.ui.components.StateIcon
import com.aistudio.gitlab.ui.components.formatDuration
import com.aistudio.gitlab.ui.components.pipelineStatusStyle
import com.aistudio.gitlab.ui.components.pressable
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlColors
import com.aistudio.gitlab.ui.theme.GlSpace
import com.aistudio.gitlab.ui.viewmodel.JobDetailState
import com.aistudio.gitlab.ui.viewmodel.PipelineDetailState
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

  Column(modifier = modifier.fillMaxSize().background(Gl.colors.bg)) {
    GlTopBar(
      title = "Pipeline #${state.pipelineId}",
      subtitle = pipeline?.ref?.takeIf { it.isNotBlank() },
      onBack = onBack,
    ) {
      val url = pipeline?.webUrl?.takeIf { it.isNotBlank() }
      if (url != null) {
        GlIconAction(Icons.AutoMirrored.Outlined.OpenInNew, "Open in browser", onClick = { onOpenInBrowser(url) })
      }
    }
    GlDivider()

    when {
      pipeline == null && state.pipeline.error != null -> FullScreenState { ErrorState(state.pipeline.error, onRetryLoad) }
      pipeline == null -> FullScreenState { LoadingState(label = "Loading pipeline") }
      else -> {
        val jobsByStage = remember(state.jobs.data) { groupByStage(state.jobs.data) }
        LazyColumn(
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
                GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin)) {
                  jobs.forEachIndexed { index, job ->
                    if (index > 0) GlDivider(inset = 48.dp)
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
  GlCard(
    modifier = Modifier.padding(start = GlSpace.margin, end = GlSpace.margin, top = GlSpace.margin),
    contentPadding = PaddingValues(GlSpace.margin),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      PipelineStatusBadge(pipeline.status)
      Spacer(Modifier.width(GlSpace.tight))
      if (pipeline.ref.isNotBlank()) MonoPill(pipeline.ref, Modifier.weight(1f, fill = false))
    }
    if (pipeline.sha.isNotBlank()) {
      Spacer(Modifier.height(GlSpace.tight))
      Row(
        modifier = Modifier
          .defaultMinSize(minHeight = GlSpace.touch)
          .pressable(onClick = { onOpenCommit(pipeline.sha) }, scaleOnPress = false, onClickLabel = "Open commit"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Commit", style = Gl.type.meta, color = c.textSecondary)
        Spacer(Modifier.width(GlSpace.tight))
        Text(pipeline.sha.take(8), style = Gl.type.mono, color = c.link)
      }
    }
    Spacer(Modifier.height(GlSpace.tight))
    val facts = listOfNotNull(
      pipeline.source?.let { "Triggered by ${humanSource(it)}" },
      pipeline.user?.username?.takeIf { it.isNotBlank() }?.let { "@$it" },
      pipeline.createdAt.takeIf { it.isNotBlank() },
    )
    if (facts.isNotEmpty()) {
      Text(facts.joinToString(" · "), style = Gl.type.meta, color = c.textSecondary)
    }
    val timing = listOfNotNull(
      pipeline.duration?.toLong()?.takeIf { it > 0 }?.let { "Ran for ${formatDuration(it)}" },
      pipeline.queuedDuration?.toLong()?.takeIf { it > 0 }?.let { "queued ${formatDuration(it)}" },
    )
    if (timing.isNotEmpty()) {
      Spacer(Modifier.height(2.dp))
      Text(timing.joinToString(" · "), style = Gl.type.meta.copy(fontFeatureSettings = "tnum"), color = c.textSecondary)
    }
    if (active) {
      Spacer(Modifier.height(GlSpace.tight))
      Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = c.textTertiary)
        Spacer(Modifier.width(6.dp))
        Text("Refreshes while running", style = Gl.type.meta, color = c.textTertiary)
      }
    }
    pipeline.yamlErrors?.takeIf { it.isNotBlank() }?.let { errors ->
      Spacer(Modifier.height(GlSpace.tight))
      InlineError(message = "Configuration error: $errors")
    }
    val canRetry = pipeline.status in setOf("failed", "canceled")
    val canCancel = pipeline.status in setOf("running", "pending", "created", "preparing", "waiting_for_resource", "scheduled")
    if (canWrite && (canRetry || canCancel)) {
      Spacer(Modifier.height(12.dp))
      FlowRow(horizontalArrangement = Arrangement.spacedBy(GlSpace.tight)) {
        if (canRetry) GlButton(text = "Retry", onClick = onRetry, kind = GlButtonKind.Secondary, loading = busy, icon = Icons.Outlined.Refresh)
        if (canCancel) GlButton(text = "Cancel", onClick = onCancel, kind = GlButtonKind.Secondary, loading = busy)
      }
    }
  }
}

@Composable
private fun StageHeader(stage: String, jobs: List<GitLabJob>) {
  val style = pipelineStatusStyle(stageStatusOf(jobs))
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = GlSpace.margin, end = GlSpace.margin, top = GlSpace.group, bottom = GlSpace.tight)
      .semantics(mergeDescendants = true) { contentDescription = "Stage $stage, ${style.label}" },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    StateIcon(style)
    Spacer(Modifier.width(GlSpace.tight))
    Text(stage.replaceFirstChar { it.uppercase() }, style = Gl.type.sectionTitle, color = Gl.colors.textPrimary)
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
      val lines by produceState<List<AnnotatedString>?>(null, trace.text, c) {
        value = withContext(Dispatchers.Default) { parseTrace(trace.text, c) }
      }
      val parsed = lines
      if (parsed == null) {
        Box(modifier.fillMaxWidth()) { LoadingState(label = "Preparing log") }
        return
      }
      val listState = rememberLazyListState()
      val scope = rememberCoroutineScope()
      val total = parsed.size + if (trace.truncated) 1 else 0
      var scrolledInitially by remember(state.jobId) { mutableStateOf(false) }
      LaunchedEffect(state.jobId, total) {
        if (!scrolledInitially && total > 0) {
          listState.scrollToItem(total - 1)
          scrolledInitially = true
        }
      }
      val longest = remember(parsed) { (parsed.maxOfOrNull { it.length } ?: 0).coerceAtMost(600) }
      val measurer = rememberTextMeasurer()
      val style = Gl.type.monoSmall
      val charWidth = remember(style) { measurer.measure("0", style).size.width.toFloat() }
      val density = LocalDensity.current
      val contentWidth = with(density) { (charWidth * (longest + 2)).toDp() } + GlSpace.margin * 2
      val horizontal = rememberScrollState()
      Box(modifier = modifier.fillMaxWidth().background(c.codeBg)) {
        Box(modifier = Modifier.fillMaxSize().horizontalScroll(horizontal)) {
          LazyColumn(
            state = listState,
            modifier = Modifier.width(contentWidth.coerceAtLeast(360.dp)).fillMaxHeight(),
            contentPadding = PaddingValues(vertical = 12.dp),
          ) {
            if (trace.truncated) {
              item(key = "truncated") {
                Text(
                  "Showing the last 256 KB. Open the job in the browser for the full log.",
                  style = Gl.type.meta,
                  color = c.textTertiary,
                  modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = 6.dp),
                )
              }
            }
            itemsIndexed(parsed) { _, line ->
              Text(
                text = if (line.isEmpty()) AnnotatedString(" ") else line,
                style = style,
                color = c.textPrimary,
                softWrap = false,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = GlSpace.margin),
              )
            }
          }
        }
        GlButton(
          text = "Jump to end",
          icon = Icons.Outlined.KeyboardDoubleArrowDown,
          kind = GlButtonKind.Secondary,
          onClick = { scope.launch { if (total > 0) listState.animateScrollToItem(total - 1) } },
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(GlSpace.margin),
        )
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

// ---------------------------------------------------------------------------------------------
// Log parsing
// ---------------------------------------------------------------------------------------------

private val SECTION_START = Regex("""section_start:\d+:([A-Za-z0-9_.\-]+)(\[[^\]]*])?\r?(\u001B\[0K)?""")
private val SECTION_END = Regex("""section_end:\d+:[A-Za-z0-9_.\-]+\r?(\u001B\[0K)?""")
private val ESCAPE = Regex("""\u001B\[([0-9;]*)([A-Za-z])""")

/** Turns a GitLab job trace into styled lines: ANSI colors applied, section markers readable. */
private fun parseTrace(raw: String, colors: GlColors): List<AnnotatedString> {
  val out = ArrayList<AnnotatedString>()
  raw.split('\n').forEach { rawLine ->
    var line = rawLine
    var header: String? = null
    SECTION_START.find(line)?.let { match ->
      header = match.groupValues[1]
      line = line.removeRange(match.range)
    }
    line = SECTION_END.replace(line, "")
    // A carriage return redraws the line; keep what was drawn last.
    if ('\r' in line) {
      val parts = line.split('\r').filter { it.isNotEmpty() }
      line = parts.lastOrNull().orEmpty()
    }
    val heading = header
    if (heading != null && line.isBlank()) {
      out += buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.textSecondary)) { append("▸ ${humanSection(heading)}") }
      }
      return@forEach
    }
    if (rawLine.isNotEmpty() && line.isEmpty() && SECTION_END.containsMatchIn(rawLine)) return@forEach
    out += ansiLine(line, colors)
  }
  while (out.isNotEmpty() && out.last().isEmpty()) out.removeAt(out.lastIndex)
  return out
}

private fun humanSection(name: String): String = name
  .removePrefix("step_")
  .replace('_', ' ')
  .replace('-', ' ')
  .replaceFirstChar { it.uppercase() }

private fun ansiLine(line: String, colors: GlColors): AnnotatedString = buildAnnotatedString {
  var color: Color? = null
  var bold = false
  var last = 0
  fun appendChunk(text: String) {
    if (text.isEmpty()) return
    val style = SpanStyle(color = color ?: Color.Unspecified, fontWeight = if (bold) FontWeight.SemiBold else null)
    withStyle(style) { append(text) }
  }
  ESCAPE.findAll(line).forEach { match ->
    appendChunk(line.substring(last, match.range.first))
    last = match.range.last + 1
    if (match.groupValues[2] != "m") return@forEach
    val codes = match.groupValues[1].split(';').mapNotNull { it.toIntOrNull() }.ifEmpty { listOf(0) }
    codes.forEach { code ->
      when (code) {
        0 -> { color = null; bold = false }
        1 -> bold = true
        22 -> bold = false
        39 -> color = null
        30, 90 -> color = colors.textTertiary
        31, 91 -> color = colors.danger
        32, 92 -> color = colors.success
        33, 93 -> color = colors.warning
        34, 94 -> color = colors.info
        35, 95 -> color = colors.syntaxKeyword
        36, 96 -> color = colors.syntaxLiteral
        37, 97 -> color = colors.textPrimary
      }
    }
  }
  appendChunk(line.substring(last).replace("\u001B", ""))
}
