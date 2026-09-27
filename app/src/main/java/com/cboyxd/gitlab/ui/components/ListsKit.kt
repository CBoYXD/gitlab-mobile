package com.cboyxd.gitlab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlSpace

// Building blocks for the GitHub Mobile style lists: dropdown filter pills, count pills, a filled
// search field and the "namespace / project #12" reference line.

/** Horizontally scrolling row of filter pills under a list's top bar. */
@Composable
fun ListsFilterBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = GlSpace.margin, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(GlSpace.tight),
    verticalAlignment = Alignment.CenterVertically,
    content = content,
  )
}

/**
 * Pill that shows the current choice with a chevron and opens a menu of the others, like
 * GitHub's "Open ⌄". [countOf] adds a count to each menu entry when given.
 */
@Composable
fun <T> ListsDropdownFilter(
  name: String,
  options: List<T>,
  selected: T,
  label: (T) -> String,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
  countOf: ((T) -> Int)? = null,
  active: Boolean = false,
) {
  val c = Gl.colors
  var open by remember { mutableStateOf(false) }
  val shape = RoundedCornerShape(50)
  Box(modifier) {
    Row(
      modifier = Modifier
        .heightIn(min = GlSpace.touch)
        .semantics(mergeDescendants = true) {
          role = Role.DropdownList
          contentDescription = "$name: ${label(selected)}"
        }
        .pressable(onClick = { open = true }, role = Role.DropdownList, onClickLabel = "Change $name"),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier
          .clip(shape)
          .background(if (active) c.accentBg else c.surface2)
          .border(1.dp, if (active) c.accent else c.border, shape)
          .padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(label(selected), style = Gl.type.label, color = if (active) c.accent else c.textPrimary, maxLines = 1)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = if (active) c.accent else c.textSecondary, modifier = Modifier.size(18.dp))
      }
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.surface) {
      options.forEach { option ->
        val isSelected = option == selected
        DropdownMenuItem(
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(label(option), style = Gl.type.body, color = c.textPrimary, modifier = Modifier.weight(1f, fill = false))
              countOf?.let {
                Spacer(Modifier.width(12.dp))
                Text(it(option).toString(), style = Gl.type.numeric, color = c.textSecondary)
              }
            }
          },
          leadingIcon = {
            Box(Modifier.size(20.dp)) {
              if (isSelected) Icon(Icons.Outlined.Check, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
            }
          },
          onClick = {
            open = false
            onSelect(option)
          },
          modifier = Modifier.semantics { if (isSelected) stateDescription = "Selected" },
        )
      }
    }
  }
}

/** Small rounded counter: a comment bubble with 6, a check with 2. */
@Composable
fun CountPill(icon: ImageVector, text: String, description: String, modifier: Modifier = Modifier, tint: Color = Gl.colors.textSecondary) {
  val c = Gl.colors
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(50))
      .background(c.surface2)
      .padding(horizontal = 9.dp, vertical = 3.dp)
      .semantics(mergeDescendants = true) { contentDescription = description },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
    Spacer(Modifier.width(5.dp))
    Text(text, style = Gl.type.numeric.copy(fontSize = Gl.type.meta.fontSize), color = c.textSecondary, maxLines = 1)
  }
}

/** Filled, fully rounded search field used at the top of Projects and Search. */
@Composable
fun ListsSearchField(
  query: String,
  onChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
) {
  val c = Gl.colors
  val focus = LocalFocusManager.current
  TextField(
    value = query,
    onValueChange = onChange,
    placeholder = { Text(placeholder, style = Gl.type.body, color = c.textTertiary) },
    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = c.textSecondary) },
    trailingIcon = {
      if (query.isNotEmpty()) {
        IconButton(onClick = { onChange("") }) {
          Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = c.textSecondary)
        }
      }
    },
    singleLine = true,
    textStyle = Gl.type.body,
    shape = RoundedCornerShape(50),
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
    colors = TextFieldDefaults.colors(
      focusedContainerColor = c.surface2,
      unfocusedContainerColor = c.surface2,
      focusedIndicatorColor = Color.Transparent,
      unfocusedIndicatorColor = Color.Transparent,
      cursorColor = c.accent,
      focusedTextColor = c.textPrimary,
      unfocusedTextColor = c.textPrimary,
    ),
    modifier = modifier.semantics { contentDescription = placeholder },
  )
}

/** "3h ago" → "3h", as GitHub's lists show ages; dates stay as they are. */
fun compactAge(display: String): String = when {
  display.isBlank() -> ""
  display == "just now" -> "now"
  else -> display.removeSuffix(" ago")
}

/**
 * "group / project" for an issue or merge request, from its reference ("group/project#12") or its
 * web URL. Nested namespaces keep their full path before the separator.
 */
fun projectLabel(fullReference: String?, webUrl: String): String? {
  val path = fullReference?.takeIf { it.isNotBlank() }?.substringBefore('#')?.substringBefore('!')?.takeIf { it.contains('/') }
    ?: webUrl.substringAfter("://", "").substringAfter('/', "").substringBefore("/-/").takeIf { it.contains('/') }
    ?: return null
  val namespace = path.substringBeforeLast('/')
  val project = path.substringAfterLast('/')
  return "$namespace / $project"
}

/** Blue "⋮" toolbar action with a menu of named actions. */
@Composable
fun ListsOverflowMenu(actions: List<Pair<String, () -> Unit>>, modifier: Modifier = Modifier) {
  if (actions.isEmpty()) return
  var open by remember { mutableStateOf(false) }
  Box(modifier) {
    GlIconAction(Icons.Outlined.MoreVert, "More options", onClick = { open = true })
    DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Gl.colors.surface) {
      actions.forEach { (label, action) ->
        DropdownMenuItem(
          text = { Text(label, style = Gl.type.body, color = Gl.colors.textPrimary) },
          onClick = {
            open = false
            action()
          },
        )
      }
    }
  }
}
