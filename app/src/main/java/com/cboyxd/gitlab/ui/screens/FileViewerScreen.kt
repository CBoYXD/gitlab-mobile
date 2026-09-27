package com.cboyxd.gitlab.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.WrapText
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cboyxd.gitlab.ui.components.EmptyState
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.FullScreenState
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlIconAction
import com.cboyxd.gitlab.ui.components.LazyCodeView
import com.cboyxd.gitlab.ui.components.LoadingState
import com.cboyxd.gitlab.ui.components.MarkdownContext
import com.cboyxd.gitlab.ui.components.MarkdownText
import com.cboyxd.gitlab.ui.components.StartEllipsisText
import com.cboyxd.gitlab.ui.components.rememberImageModel
import com.cboyxd.gitlab.ui.components.shareText
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlRadius
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.viewmodel.FileState

// ---------------------------------------------------------------------------------------------
// File viewer
// ---------------------------------------------------------------------------------------------

private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "ico")
private val MARKDOWN_EXTENSIONS = setOf("md", "markdown", "mdown", "mkd")

/**
 * A file in the manner of GitHub Mobile: the name in the bar (trimmed from the start so the
 * extension stays visible), share and a menu, then numbered, wrapped, highlighted code. Tapping a
 * line selects it and a second tap extends the selection to a range; the bar then offers sharing a
 * link to those lines and copying them. Back clears the selection before leaving.
 */
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
  val context = LocalContext.current
  val name = state.path.substringAfterLast('/')
  val extension = name.substringAfterLast('.', "").lowercase()
  val isImage = extension in IMAGE_EXTENSIONS
  val isMarkdown = extension in MARKDOWN_EXTENSIONS
  val ref = state.ref ?: "HEAD"
  val content = state.content.data
  val blobUrl = webUrl?.let { "$it/-/blob/$ref/${state.path}" }
  var preview by rememberSaveable(state.path) { mutableStateOf(true) }
  var wrap by rememberSaveable { mutableStateOf(true) }
  // Selection as 1-based line numbers; start is the first tap, end the second.
  var selStart by rememberSaveable(state.path) { mutableStateOf(0) }
  var selEnd by rememberSaveable(state.path) { mutableStateOf(0) }
  val selection = if (selStart > 0) minOf(selStart, selEnd)..maxOf(selStart, selEnd) else null
  val showingCode = content != null && content.isNotEmpty() && !isImage && !(isMarkdown && preview)
  val clearSelection = { selStart = 0; selEnd = 0 }

  BackHandler(enabled = selection != null) { clearSelection() }

  Column(modifier = modifier.fillMaxSize().background(c.bg)) {
    if (selection != null && showingCode) {
      val lines = remember(content) { content.orEmpty().lines() }
      val label = if (selection.first == selection.last) "Line ${selection.first}" else "Lines ${selection.first} to ${selection.last}"
      FileBar(background = c.surface3) {
        IconButton(onClick = clearSelection) {
          Icon(Icons.Outlined.Close, contentDescription = "Clear selection", tint = c.textPrimary)
        }
        Spacer(Modifier.width(8.dp))
        Text(
          label,
          style = Gl.type.topBar,
          color = c.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (blobUrl != null) {
          val anchor = if (selection.first == selection.last) "#L${selection.first}" else "#L${selection.first}-${selection.last}"
          BarAction(Icons.Outlined.Share, "Share link to ${label.lowercase()}", tint = c.textPrimary) {
            shareText(context, blobUrl + anchor, title = name)
          }
        }
        BarAction(Icons.Outlined.ContentCopy, "Copy ${label.lowercase()}", tint = c.textPrimary) {
          val from = (selection.first - 1).coerceIn(0, lines.size)
          val to = selection.last.coerceIn(from, lines.size)
          clipboard.setText(AnnotatedString(lines.subList(from, to).joinToString("\n")))
          clearSelection()
        }
      }
    } else {
      FileBar(background = c.bg) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
        }
        Spacer(Modifier.width(8.dp))
        StartEllipsisText(
          text = name.ifBlank { "File" },
          style = Gl.type.topBar,
          color = c.textPrimary,
          modifier = Modifier
            .weight(1f)
            .semantics(mergeDescendants = true) {
              heading()
              contentDescription = name.ifBlank { "File" }
            },
        )
        if (isMarkdown && content != null) {
          GlIconAction(
            icon = if (preview) Icons.Outlined.Code else Icons.Outlined.Visibility,
            contentDescription = if (preview) "Show source" else "Show preview",
            onClick = {
              preview = !preview
              clearSelection()
            },
          )
        }
        if (blobUrl != null) {
          GlIconAction(Icons.Outlined.Share, "Share", onClick = { shareText(context, blobUrl, title = name) })
        }
        FileMenu(
          canOpen = webUrl != null,
          canCopy = !content.isNullOrEmpty() && !isImage,
          wrap = wrap,
          showWrap = showingCode,
          onOpenInBrowser = { blobUrl?.let(onOpenInBrowser) },
          onOpenRaw = { webUrl?.let { onOpenInBrowser("$it/-/raw/$ref/${state.path}") } },
          onCopyPath = { clipboard.setText(AnnotatedString(state.path)) },
          onCopyContents = { content?.let { clipboard.setText(AnnotatedString(it)) } },
          onToggleWrap = { wrap = !wrap },
        )
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
      else -> LazyCodeView(
        code = content,
        path = state.path,
        wrap = wrap,
        selection = selection,
        onLineClick = { line ->
          when {
            selStart == 0 -> { selStart = line; selEnd = line }
            selStart == selEnd && line == selStart -> clearSelection()
            selStart == selEnd -> selEnd = line
            else -> { selStart = line; selEnd = line }
          }
        },
      )
    }
  }
}

@Composable
private fun FileBar(background: Color, content: @Composable RowScope.() -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(background)
      .heightIn(min = 60.dp)
      .padding(start = 4.dp, end = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    content = content,
  )
}

@Composable
private fun BarAction(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
  IconButton(onClick = onClick) { Icon(icon, contentDescription = description, tint = tint) }
}

@Composable
private fun FileMenu(
  canOpen: Boolean,
  canCopy: Boolean,
  wrap: Boolean,
  showWrap: Boolean,
  onOpenInBrowser: () -> Unit,
  onOpenRaw: () -> Unit,
  onCopyPath: () -> Unit,
  onCopyContents: () -> Unit,
  onToggleWrap: () -> Unit,
) {
  val c = Gl.colors
  var open by remember { mutableStateOf(false) }
  Box {
    GlIconAction(Icons.Outlined.MoreVert, "More options", onClick = { open = true })
    DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.surface2) {
      val colors = MenuDefaults.itemColors(textColor = c.textPrimary, leadingIconColor = c.textSecondary, trailingIconColor = c.accent)
      @Composable
      fun item(text: String, icon: ImageVector, trailing: ImageVector? = null, action: () -> Unit) = DropdownMenuItem(
        text = { Text(text, style = Gl.type.body) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = trailing?.let { { Icon(it, contentDescription = "On") } },
        colors = colors,
        onClick = {
          open = false
          action()
        },
      )
      if (showWrap) item("Wrap lines", Icons.AutoMirrored.Outlined.WrapText, if (wrap) Icons.Outlined.Check else null, onToggleWrap)
      if (canOpen) {
        item("Open in browser", Icons.AutoMirrored.Outlined.OpenInNew, action = onOpenInBrowser)
        item("View raw", Icons.Outlined.Description, action = onOpenRaw)
      }
      item("Copy path", Icons.Outlined.FolderOpen, action = onCopyPath)
      if (canCopy) item("Copy file contents", Icons.Outlined.ContentCopy, action = onCopyContents)
    }
  }
}
