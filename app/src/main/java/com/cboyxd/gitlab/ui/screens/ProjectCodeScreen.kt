package com.cboyxd.gitlab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import com.cboyxd.gitlab.ui.components.GlChip
import com.cboyxd.gitlab.ui.viewmodel.Loadable
import com.cboyxd.gitlab.ui.components.IconTile
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.filled.CheckCircle
import com.cboyxd.gitlab.ui.components.FileTypeIcon
import androidx.compose.material.icons.outlined.SubdirectoryArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.data.model.GitLabTreeItem
import com.cboyxd.gitlab.ui.components.EmptyState
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.FullScreenState
import com.cboyxd.gitlab.ui.components.GlBadge
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlIconAction
import com.cboyxd.gitlab.ui.components.GlTopBar
import com.cboyxd.gitlab.ui.components.InlineError
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlRadius
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.ProjectDetailState
import kotlinx.coroutines.launch

/** Repository browser: ref picker, breadcrumbs, folders first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCodeScreen(
  state: ProjectDetailState,
  onBack: () -> Unit,
  onOpenPath: (String) -> Unit,
  onOpenFile: (String) -> Unit,
  onChangeRef: (String?) -> Unit,
  onLoadRefs: () -> Unit,
  onFilterRefs: (String) -> Unit,
  onOpenCommits: () -> Unit,
  onOpenInBrowser: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val c = Gl.colors
  val project = state.project.data
  val ref = state.effectiveRef
  var showRefs by rememberSaveable { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxSize().background(c.bg)) {
    val context = androidx.compose.ui.platform.LocalContext.current
    GlTopBar(
      title = state.treePath.substringAfterLast('/').ifEmpty { "Files" },
      overline = project?.pathWithNamespace,
      onBack = onBack,
      actions = {
        GlIconAction(Icons.Outlined.History, "Commits", onClick = onOpenCommits, enabled = project != null)
        val web = state.webUrl
        if (web != null && ref != null) {
          val suffix = if (state.treePath.isEmpty()) "" else "/" + state.treePath
          val url = "$web/-/tree/$ref$suffix"
          GlIconAction(Icons.Outlined.Share, "Share", onClick = { shareUrl(context, project?.name ?: "Files", url) })
          GlIconAction(Icons.AutoMirrored.Outlined.OpenInNew, "Open in browser", onClick = { onOpenInBrowser(url) })
        }
      },
    )

    if (project == null) {
      FullScreenState {
        val error = state.project.error
        if (error != null) ErrorState(message = error, onRetry = { onOpenPath(state.treePath) }) else LoadingState(label = "Loading project")
      }
      return@Column
    }

    if (ref == null || project.emptyRepo) {
      FullScreenState {
        EmptyState(
          icon = Icons.Outlined.FolderOpen,
          title = "Empty repository",
          body = "Push a commit to see files here.",
        )
      }
      return@Column
    }

    RefPickerRow(ref = ref, isDefault = state.ref == null, onClick = { showRefs = true })
    if (state.treePath.isNotEmpty()) Breadcrumbs(path = state.treePath, onOpenPath = onOpenPath)
    GlDivider()

    val tree = state.tree
    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 2.dp)) {
      if (tree.loading) {
        LinearProgressIndicator(
          modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Loading folder" },
          color = c.textSecondary,
          trackColor = c.surface2,
        )
      }
    }

    val treeError = tree.error
    when {
      treeError != null && tree.data.isEmpty() -> FullScreenState {
        ErrorState(message = treeError, onRetry = { onOpenPath(state.treePath) })
      }
      tree.initialLoading -> FullScreenState { LoadingState(label = "Loading files") }
      tree.loaded && tree.data.isEmpty() && !tree.loading -> FullScreenState {
        EmptyState(
          icon = Icons.Outlined.FolderOpen,
          title = "This folder is empty",
          body = "Go up a level or switch to another branch.",
        )
      }
      else -> LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = GlSpace.group),
      ) {
        if (treeError != null) {
          item(key = "error") {
            InlineError(
              message = treeError,
              onRetry = { onOpenPath(state.treePath) },
              modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = 4.dp),
            )
          }
        }
        items(tree.data, key = { it.path }) { item ->
          TreeRow(
            item = item,
            onClick = { if (item.type == "tree") onOpenPath(item.path) else onOpenFile(item.path) },
          )
        }
        item(key = "bottom") { Spacer(Modifier.navigationBarsPadding()) }
      }
    }
  }

  if (showRefs) {
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
        onChangeRef(selected.takeIf { it != project?.defaultBranch })
      },
    )
  }
}

@Composable
private fun RefPickerRow(ref: String, isDefault: Boolean, onClick: () -> Unit) {
  val c = Gl.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = "Switch branch or tag")
      .defaultMinSize(minHeight = 52.dp)
      .padding(horizontal = GlSpace.margin),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(Icons.Outlined.AccountTree, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(12.dp))
    BranchPill(ref, Modifier.weight(1f, fill = false))
    if (isDefault) {
      Spacer(Modifier.width(8.dp))
      DefaultBadge()
    }
    Spacer(Modifier.weight(1f))
    Text("Change", style = Gl.type.label.copy(fontWeight = FontWeight.SemiBold), color = c.accent)
  }
}

/** Branch or tag name as GitHub shows it: monospace, blue, on a tinted rounded pill. */
@Composable
internal fun BranchPill(name: String, modifier: Modifier = Modifier) {
  val c = Gl.colors
  Text(
    text = name,
    style = Gl.type.mono.copy(fontSize = Gl.type.body.fontSize, lineHeight = Gl.type.body.lineHeight),
    color = c.accent,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(c.accentBg)
      .border(1.dp, c.accent.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
      .padding(horizontal = 10.dp, vertical = 4.dp),
  )
}

@Composable
private fun DefaultBadge(text: String = "Default") {
  val c = Gl.colors
  Text(
    text,
    style = Gl.type.bodySmall,
    color = c.textSecondary,
    modifier = Modifier
      .clip(RoundedCornerShape(50))
      .background(c.sunken)
      .padding(horizontal = 10.dp, vertical = 3.dp),
  )
}

@Composable
private fun Breadcrumbs(path: String, onOpenPath: (String) -> Unit) {
  val c = Gl.colors
  val segments = if (path.isEmpty()) emptyList() else path.split('/')
  val scroll = rememberScrollState()
  LaunchedEffect(path) { scroll.scrollTo(scroll.maxValue) }
  Row(
    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      modifier = Modifier.weight(1f).horizontalScroll(scroll),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Crumb(label = "root", current = segments.isEmpty(), onClick = { onOpenPath("") })
      segments.forEachIndexed { index, segment ->
        Text("/", style = Gl.type.mono, color = c.textTertiary)
        val prefix = segments.take(index + 1).joinToString("/")
        Crumb(label = segment, current = index == segments.lastIndex, onClick = { onOpenPath(prefix) })
      }
    }
    if (segments.isNotEmpty()) {
      GlIconAction(
        icon = Icons.Outlined.SubdirectoryArrowLeft,
        contentDescription = "Up one level",
        onClick = { onOpenPath(path.substringBeforeLast('/', "")) },
        tint = c.textSecondary,
      )
    }
  }
}

@Composable
private fun Crumb(label: String, current: Boolean, onClick: () -> Unit) {
  val c = Gl.colors
  Box(
    modifier = Modifier
      .defaultMinSize(minHeight = GlSpace.touch)
      .clickable(enabled = !current, onClick = onClick, role = Role.Button, onClickLabel = "Open $label")
      .padding(horizontal = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = if (current) Gl.type.mono.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) else Gl.type.mono,
      color = if (current) c.textPrimary else c.textSecondary,
      maxLines = 1,
      modifier = if (current) Modifier.semantics { heading() } else Modifier,
    )
  }
}

@Composable
private fun TreeRow(item: GitLabTreeItem, onClick: () -> Unit) {
  val c = Gl.colors
  val folder = item.type == "tree"
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = if (folder) "Open folder" else "Open file")
      .semantics(mergeDescendants = true) { contentDescription = (if (folder) "Folder " else "File ") + item.name }
      .defaultMinSize(minHeight = 56.dp)
      .padding(horizontal = GlSpace.margin, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    FileTypeIcon(name = item.name, folder = folder, size = 26.dp)
    Spacer(Modifier.width(18.dp))
    Text(
      text = item.name,
      style = Gl.type.rowTitle,
      color = c.textPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
  }
}

private enum class RefKind(val title: String) { BRANCHES("Branches"), TAGS("Tags") }

/**
 * Branch and tag switcher in the manner of GitHub's: a search field, a Branches/Tags switch,
 * the current ref pinned on top, and rows that say when each ref last changed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RefPickerSheet(
  state: ProjectDetailState,
  onLoadRefs: () -> Unit,
  onFilter: (String) -> Unit,
  onDismiss: () -> Unit,
  onSelect: (String) -> Unit,
) {
  val c = Gl.colors
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()
  var query by rememberSaveable { mutableStateOf("") }
  var kind by rememberSaveable { mutableStateOf(RefKind.BRANCHES) }
  LaunchedEffect(Unit) { onLoadRefs() }
  val current = state.effectiveRef
  val defaultBranch = state.project.data?.defaultBranch
  val pick: (String) -> Unit = { name -> scope.launch { sheetState.hide() }.invokeOnCompletion { onSelect(name) } }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.surface) {
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f).imePadding()) {
      Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = GlSpace.margin), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }) {
          Icon(Icons.Outlined.Close, contentDescription = "Close", tint = c.textPrimary)
        }
        Spacer(Modifier.width(8.dp))
        Text(
          if (kind == RefKind.BRANCHES) "Choose branch" else "Choose tag",
          style = Gl.type.topBar,
          color = c.textPrimary,
          modifier = Modifier.semantics { heading() },
        )
      }
      Spacer(Modifier.height(8.dp))
      SearchPill(
        value = query,
        onValueChange = {
          query = it
          onFilter(it)
        },
        placeholder = if (kind == RefKind.BRANCHES) "Find a branch" else "Find a tag",
      )
      Spacer(Modifier.height(12.dp))
      Row(modifier = Modifier.padding(horizontal = GlSpace.margin), horizontalArrangement = Arrangement.spacedBy(GlSpace.tight)) {
        RefKind.entries.forEach { option ->
          val feed = if (option == RefKind.BRANCHES) state.branches else state.tags
          val count = feed.data.size.takeIf { feed.loaded && !feed.loading }
          GlChip(
            // A full page means there are more on the server.
            text = option.title + (count?.let { n -> val page = if (option == RefKind.BRANCHES) 100 else 50; " " + if (n >= page) "$page+" else n.toString() } ?: ""),
            selected = kind == option,
            onClick = { kind = option },
          )
        }
      }
      Spacer(Modifier.height(GlSpace.tight))
      GlDivider()

      LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = GlSpace.group)) {
        // The ref in use stays reachable even when the loaded page or the filter doesn't include it.
        if (query.isBlank() && current != null) {
          item(key = "current-header") { SheetHeader("Current") }
          item(key = "current") {
            val branch = state.branches.data.firstOrNull { it.name == current }
            val tag = state.tags.data.firstOrNull { it.name == current }
            RefRow(
              icon = if (tag != null && branch == null) Icons.Outlined.LocalOffer else Icons.Outlined.AccountTree,
              name = current,
              detail = (branch?.commit ?: tag?.commit)?.let { commitLine(it.title, it.committedDate ?: it.createdAt) },
              badges = listOfNotNull("Default".takeIf { current == defaultBranch }, "Protected".takeIf { branch?.protected == true }),
              selected = true,
              onClick = { pick(current) },
            )
          }
          item(key = "all-header") { SheetHeader(if (kind == RefKind.BRANCHES) "Recently updated" else "Latest tags") }
        }
        when (kind) {
          RefKind.BRANCHES -> refItems(
            feed = state.branches,
            query = query,
            noun = "branches",
            onRetry = onLoadRefs,
            rows = state.branches.data.filter { query.isNotBlank() || it.name != current },
          ) { branch ->
            RefRow(
              icon = Icons.Outlined.AccountTree,
              name = branch.name,
              detail = branch.commit?.let { commitLine(it.title, it.committedDate ?: it.createdAt) },
              badges = listOfNotNull("Default".takeIf { branch.default }, "Protected".takeIf { branch.protected }, "Merged".takeIf { branch.merged }),
              selected = branch.name == current,
              onClick = { pick(branch.name) },
            )
          }
          RefKind.TAGS -> refItems(
            feed = state.tags,
            query = query,
            noun = "tags",
            onRetry = onLoadRefs,
            rows = state.tags.data.filter { query.isNotBlank() || it.name != current },
          ) { tag ->
            RefRow(
              icon = Icons.Outlined.LocalOffer,
              name = tag.name,
              detail = tag.commit?.let { commitLine(it.title, it.committedDate ?: it.createdAt) },
              badges = listOfNotNull("Release".takeIf { tag.release != null }, "Protected".takeIf { tag.protected }),
              selected = tag.name == current,
              onClick = { pick(tag.name) },
            )
          }
        }
      }
    }
  }
}

/** "Updated 3h ago · last commit title": the date leads so truncation never hides it. */
private fun commitLine(title: String, time: String?): String =
  listOfNotNull(time?.takeIf { it.isNotBlank() }?.let { "Updated $it" }, title.takeIf { it.isNotBlank() }).joinToString(" · ")

private fun <T> androidx.compose.foundation.lazy.LazyListScope.refItems(
  feed: Loadable<List<T>>,
  query: String,
  noun: String,
  onRetry: () -> Unit,
  rows: List<T>,
  row: @Composable (T) -> Unit,
) {
  val error = feed.error
  when {
    feed.loading && rows.isEmpty() -> item(key = "$noun-loading") { LoadingState(label = "Loading $noun") }
    error != null && rows.isEmpty() -> item(key = "$noun-error") {
      InlineError(error, Modifier.padding(horizontal = GlSpace.margin, vertical = GlSpace.tight), onRetry = onRetry)
    }
    rows.isEmpty() -> item(key = "$noun-empty") {
      SheetEmpty(if (query.isBlank()) "No other $noun" else "No $noun match “${query.trim()}”")
    }
    else -> itemsIndexed(rows, key = { index, _ -> "$noun:$index" }) { index, value ->
      row(value)
    }
  }
}

/** Rounded filled search field; the icon and placeholder carry the purpose, the semantics name it. */
@Composable
private fun SearchPill(value: String, onValueChange: (String) -> Unit, placeholder: String) {
  val c = Gl.colors
  TextField(
    value = value,
    onValueChange = onValueChange,
    placeholder = { Text(placeholder, style = Gl.type.body, color = c.textTertiary) },
    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = c.textSecondary) },
    trailingIcon = {
      if (value.isNotEmpty()) {
        IconButton(onClick = { onValueChange("") }) {
          Icon(Icons.Outlined.Close, contentDescription = "Clear filter", tint = c.textSecondary)
        }
      }
    },
    singleLine = true,
    textStyle = Gl.type.body,
    shape = RoundedCornerShape(50),
    colors = TextFieldDefaults.colors(
      focusedContainerColor = c.surface2,
      unfocusedContainerColor = c.surface2,
      focusedIndicatorColor = Color.Transparent,
      unfocusedIndicatorColor = Color.Transparent,
      cursorColor = c.textPrimary,
      focusedTextColor = c.textPrimary,
      unfocusedTextColor = c.textPrimary,
    ),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = GlSpace.margin)
      .semantics { contentDescription = placeholder },
  )
}

@Composable
private fun SheetHeader(text: String) {
  Text(
    text,
    style = Gl.type.label,
    color = Gl.colors.textSecondary,
    modifier = Modifier.padding(start = GlSpace.margin, end = GlSpace.margin, top = 20.dp, bottom = 4.dp).semantics { heading() },
  )
}

@Composable
private fun SheetEmpty(text: String) {
  Text(text, style = Gl.type.bodySmall, color = Gl.colors.textSecondary, modifier = Modifier.padding(horizontal = GlSpace.margin, vertical = 16.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RefRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  name: String,
  detail: String?,
  badges: List<String>,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val c = Gl.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick, role = Role.Button, onClickLabel = if (selected) null else "Switch to $name")
      .semantics { if (selected) stateDescription = "Current" }
      .defaultMinSize(minHeight = 60.dp)
      .padding(horizontal = GlSpace.margin, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BranchPill(name, Modifier.align(Alignment.CenterVertically))
        badges.forEach { DefaultBadge(it) }
      }
      if (!detail.isNullOrBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(detail, style = Gl.type.meta, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
    if (selected) {
      Spacer(Modifier.width(8.dp))
      Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = c.accent, modifier = Modifier.size(26.dp))
    }
  }
}
