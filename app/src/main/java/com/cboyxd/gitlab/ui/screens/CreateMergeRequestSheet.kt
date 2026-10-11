package com.cboyxd.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cboyxd.gitlab.data.model.GitLabBranch
import com.cboyxd.gitlab.data.model.GitLabProject
import com.cboyxd.gitlab.ui.components.*
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace
import kotlinx.coroutines.delay

/** Shared by the dashboard and a project's merge request list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateMergeRequestSheet(
  projects: List<GitLabProject>,
  onLoadBranches: suspend (Long, String) -> Result<List<GitLabBranch>>,
  onCreate: (Long, String, String, String, String?, (String?) -> Unit) -> Unit,
  onDismiss: () -> Unit,
) {
  var projectId by rememberSaveable { mutableStateOf(projects.firstOrNull()?.id ?: 0L) }
  val project = projects.firstOrNull { it.id == projectId }
  var source by rememberSaveable { mutableStateOf("") }
  var target by rememberSaveable { mutableStateOf(project?.defaultBranch.orEmpty()) }
  var title by rememberSaveable { mutableStateOf("") }
  var description by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
  var draft by rememberSaveable { mutableStateOf(false) }
  var selecting by remember { mutableStateOf<String?>(null) }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  val busyState = rememberUpdatedState(busy)
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { !busyState.value })
  val ready = project != null && source.isNotBlank() && target.isNotBlank() && source != target && title.isNotBlank()

  ModalBottomSheet(
    onDismissRequest = { if (!busy) onDismiss() },
    sheetState = sheetState,
    containerColor = Gl.colors.surface,
  ) {
    Column(
      Modifier.fillMaxWidth().imePadding().navigationBarsPadding()
        .verticalScroll(rememberScrollState()).padding(horizontal = GlSpace.margin).padding(bottom = GlSpace.group),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("New merge request", style = Gl.type.title, color = Gl.colors.textPrimary)
      if (projects.size > 1) {
        ProjectPicker(projects = projects, selectedId = projectId, enabled = !busy, onSelect = {
          projectId = it.id
          source = ""
          target = it.defaultBranch.orEmpty()
          error = null
        })
      } else {
        Text(project?.nameWithNamespace ?: "Choose a project", style = Gl.type.bodySmall, color = Gl.colors.textSecondary)
      }
      Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BranchChoice("From · source branch", source, !busy && project != null) { selecting = "source" }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
          Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Merge into", tint = Gl.colors.textTertiary, modifier = Modifier.size(20.dp))
        }
        BranchChoice("Into · target branch", target, !busy && project != null) { selecting = "target" }
      }
      Text("Changes from the source branch will be proposed for the target branch.", style = Gl.type.meta, color = Gl.colors.textSecondary)
      OutlinedTextField(
        value = title, onValueChange = { title = it; error = null },
        label = { Text("Title") }, enabled = !busy,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(), maxLines = 3,
      )
      MarkdownEditor(value = description, onValueChange = { description = it }, label = "Description", minLines = 4, enabled = !busy)
      Row(
        Modifier.fillMaxWidth().clickable(enabled = !busy, role = Role.Checkbox) { draft = !draft }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Checkbox(checked = draft, onCheckedChange = { draft = it }, enabled = !busy)
        Column(Modifier.weight(1f)) {
          Text("Create as draft", style = Gl.type.bodySmall, color = Gl.colors.textPrimary)
          Text("Ask for feedback before it's ready to merge", style = Gl.type.meta, color = Gl.colors.textSecondary)
        }
      }
      if (source.isNotBlank() && source == target) InlineError("Source and target branches must be different.")
      error?.let { InlineError(it) }
      GlButton(
        text = if (draft) "Create draft merge request" else "Create merge request", loading = busy, enabled = ready && !busy,
        modifier = Modifier.fillMaxWidth(), onClick = {
          busy = true
          error = null
          val submittedTitle = if (draft && !title.trim().startsWith("Draft:", ignoreCase = true)) "Draft: ${title.trim()}" else title.trim()
          onCreate(projectId, submittedTitle, source, target, description.text.takeIf { it.isNotBlank() }) {
            busy = false
            if (it == null) onDismiss() else error = it
          }
        },
      )
      GlButton("Cancel", onClick = onDismiss, kind = GlButtonKind.Ghost, enabled = !busy, modifier = Modifier.fillMaxWidth())
    }
  }
  selecting?.let { side ->
    BranchPickerDialog(
      projectId = projectId,
      title = if (side == "source") "Source branch" else "Target branch",
      selected = if (side == "source") source else target,
      excluded = if (side == "source") target else source,
      onLoad = onLoadBranches,
      onSelect = {
        if (side == "source") {
          source = it.name
          if (title.isBlank()) title = it.name.substringAfterLast('/').replace('-', ' ').replace('_', ' ')
        } else target = it.name
        error = null
        selecting = null
      },
      onDismiss = { selecting = null },
    )
  }
}

@Composable
private fun BranchChoice(label: String, branch: String, enabled: Boolean, onClick: () -> Unit) {
  Surface(
    onClick = onClick, enabled = enabled, shape = MaterialTheme.shapes.medium,
    color = Gl.colors.sunken, modifier = Modifier.fillMaxWidth(),
  ) {
    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Icon(Icons.Outlined.AccountTree, null, tint = Gl.colors.accent, modifier = Modifier.size(20.dp))
      Column(Modifier.weight(1f)) {
        Text(label, style = Gl.type.meta, color = Gl.colors.textSecondary)
        Text(branch.ifBlank { "Choose branch" }, style = Gl.type.mono, color = Gl.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
      }
      Icon(Icons.Outlined.KeyboardArrowDown, null, tint = Gl.colors.textTertiary)
    }
  }
}

@Composable
private fun BranchPickerDialog(
  projectId: Long,
  title: String,
  selected: String,
  excluded: String,
  onLoad: suspend (Long, String) -> Result<List<GitLabBranch>>,
  onSelect: (GitLabBranch) -> Unit,
  onDismiss: () -> Unit,
) {
  var query by rememberSaveable(projectId) { mutableStateOf("") }
  var branches by remember(projectId) { mutableStateOf(emptyList<GitLabBranch>()) }
  var loading by remember { mutableStateOf(true) }
  var error by remember { mutableStateOf<String?>(null) }
  var retry by remember { mutableIntStateOf(0) }
  val loader by rememberUpdatedState(onLoad)
  LaunchedEffect(projectId, query, retry) {
    loading = true
    error = null
    if (query.isNotBlank()) delay(300)
    loader(projectId, query.trim()).fold(
      onSuccess = { branches = it; loading = false },
      onFailure = { branches = emptyList(); error = it.message ?: "Unable to load branches"; loading = false },
    )
  }
  Dialog(onDismissRequest = onDismiss) {
    Surface(shape = MaterialTheme.shapes.large, color = Gl.colors.surface) {
      Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = Gl.type.title, color = Gl.colors.textPrimary)
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search branches") }, singleLine = true,
          keyboardOptions = KeyboardOptions(autoCorrectEnabled = false), modifier = Modifier.fillMaxWidth())
        when {
          loading -> LoadingState(label = "Loading branches", modifier = Modifier.height(100.dp))
          error != null -> InlineError(error!!, onRetry = { retry++ })
          branches.isEmpty() -> Text("No branches found", style = Gl.type.bodySmall, color = Gl.colors.textSecondary)
          else -> LazyColumn(Modifier.weight(1f, fill = false)) {
            items(branches, key = { it.name }) { branch ->
              val enabled = branch.name != excluded
              Row(
                Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button) { onSelect(branch) }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                RadioButton(selected = branch.name == selected, onClick = null, enabled = enabled)
                Column(Modifier.weight(1f)) {
                  Text(branch.name, style = Gl.type.mono, color = if (enabled) Gl.colors.textPrimary else Gl.colors.textTertiary)
                  if (!enabled) Text("Already selected on the other side", style = Gl.type.meta, color = Gl.colors.textTertiary)
                }
                if (branch.default) GlBadge("Default", tone = Tone.Info)
              }
              GlDivider()
            }
          }
        }
        GlButton("Cancel", onClick = onDismiss, kind = GlButtonKind.Ghost, modifier = Modifier.fillMaxWidth())
      }
    }
  }
}
