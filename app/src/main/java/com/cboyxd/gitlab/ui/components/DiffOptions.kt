package com.cboyxd.gitlab.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.data.model.GitLabFileDiff
import com.cboyxd.gitlab.ui.theme.Gl
import kotlin.math.roundToInt

@Immutable
data class DiffDisplayOptions(
  val wrapLines: Boolean = true,
  val showLineNumbers: Boolean = true,
  val dualLineNumbers: Boolean = false,
  val scrollablePath: Boolean = true,
  val syntaxColors: Boolean = true,
  val fontSize: Int = 14,
)

val LocalDiffDisplayOptions = staticCompositionLocalOf { DiffDisplayOptions() }

/** Persistent display preferences for merge request changes. */
@Composable
fun rememberDiffDisplayOptions(): Pair<DiffDisplayOptions, (DiffDisplayOptions) -> Unit> {
  val context = LocalContext.current.applicationContext
  val prefs = remember(context) { context.getSharedPreferences("diff_display", 0) }
  var options by remember(prefs) {
    mutableStateOf(DiffDisplayOptions(
      wrapLines = prefs.getBoolean("wrap", true),
      showLineNumbers = prefs.getBoolean("numbers", true),
      dualLineNumbers = prefs.getBoolean("dual_numbers", false),
      scrollablePath = prefs.getBoolean("scroll_path", true),
      syntaxColors = prefs.getBoolean("syntax", true),
      fontSize = prefs.getInt("size", 14).coerceIn(11, 18),
    ))
  }
  return options to { updated ->
    options = updated
    prefs.edit().putBoolean("wrap", updated.wrapLines).putBoolean("numbers", updated.showLineNumbers)
      .putBoolean("dual_numbers", updated.dualLineNumbers).putBoolean("scroll_path", updated.scrollablePath)
      .putBoolean("syntax", updated.syntaxColors).putInt("size", updated.fontSize).apply()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffOptionsSheet(
  options: DiffDisplayOptions,
  onChange: (DiffDisplayOptions) -> Unit,
  onDismiss: () -> Unit,
  actions: @Composable () -> Unit = {},
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = Gl.colors.surface,
  ) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 16.dp)) {
      Text("Code options", style = Gl.type.title, color = Gl.colors.textPrimary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
      DiffOptionToggle("Wrap lines", options.wrapLines) { onChange(options.copy(wrapLines = it)) }
      DiffOptionToggle("Show line numbers", options.showLineNumbers) { onChange(options.copy(showLineNumbers = it)) }
      if (options.showLineNumbers) DiffOptionToggle("Show old and new numbers", options.dualLineNumbers) { onChange(options.copy(dualLineNumbers = it)) }
      DiffOptionToggle("Scrollable file path", options.scrollablePath) { onChange(options.copy(scrollablePath = it)) }
      DiffOptionToggle("Syntax highlighting", options.syntaxColors) { onChange(options.copy(syntaxColors = it)) }
      Column(Modifier.padding(horizontal = 16.dp)) {
        Text("Text size · ${options.fontSize} sp", style = Gl.type.bodySmall, color = Gl.colors.textPrimary)
        Slider(value = options.fontSize.toFloat(), onValueChange = { onChange(options.copy(fontSize = it.roundToInt())) }, valueRange = 11f..18f, steps = 6)
      }
      GlDivider()
      Text("Preview", style = Gl.type.sectionTitle, color = Gl.colors.textPrimary, modifier = Modifier.padding(16.dp))
      val diff = remember { GitLabFileDiff(newPath = "example.kt", diff = "@@ -1,3 +1,3 @@\n fun greeting(name: String): String {\n-    return name\n+    return \"Hello, \" + name // A readable preview with long lines\n }") }
      val prepared = rememberPreparedDiffs(listOf(diff))
      val scroll = rememberScrollState()
      CompositionLocalProvider(LocalDiffDisplayOptions provides options) {
        prepared.parsed[0]?.forEachIndexed { index, line ->
          DiffLineRow(line, prepared.texts[0]?.getOrNull(index), 28.dp,
            scrollState = if (options.wrapLines) null else scroll, codeWidth = 650.dp)
        }
      }
      GlDivider()
      Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { actions() }
      GlButton("Done", onClick = onDismiss, kind = GlButtonKind.Ghost, modifier = Modifier.fillMaxWidth())
    }
  }
}

@Composable
private fun DiffOptionToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
  Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(label, style = Gl.type.bodySmall, color = Gl.colors.textPrimary, modifier = Modifier.weight(1f))
    Switch(checked = checked, onCheckedChange = onChange)
  }
}
