package com.cboyxd.gitlab.ui.components

import android.content.Context
import android.graphics.Picture
import android.graphics.RectF
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cboyxd.gitlab.ui.theme.Gl
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * File and folder icons from Material Icon Theme (assets/file-icons, refreshed by
 * tools/update_file_icons.py). Lookup follows the VS Code extension: an exact file name wins, then
 * the longest matching extension ("spec.ts" before "ts"); folders match by name. The light theme
 * swaps in the theme's light variants where it has them.
 */
object FileIcons {
  private const val DIR = "file-icons"

  private class Tables(
    val file: String,
    val folder: String,
    val fileNames: Map<String, String>,
    val fileExtensions: Map<String, String>,
    val folderNames: Map<String, String>,
    val lightFileNames: Map<String, String>,
    val lightFileExtensions: Map<String, String>,
    val lightFolderNames: Map<String, String>,
  )

  @Volatile private var tables: Tables? = null
  private val pictures = LruCache<String, Picture>(160)

  private fun tables(context: Context): Tables = tables ?: synchronized(this) {
    tables ?: load(context).also { tables = it }
  }

  private fun load(context: Context): Tables {
    val json = JSONObject(context.assets.open("$DIR/icons.json").bufferedReader().use { it.readText() })
    fun JSONObject.map(key: String): Map<String, String> {
      val obj = optJSONObject(key) ?: return emptyMap()
      val out = HashMap<String, String>(obj.length() * 2)
      obj.keys().forEach { out[it] = obj.getString(it) }
      return out
    }
    val light = json.optJSONObject("light") ?: JSONObject()
    return Tables(
      file = json.getString("file"),
      folder = json.getString("folder"),
      fileNames = json.map("fileNames"),
      fileExtensions = json.map("fileExtensions"),
      folderNames = json.map("folderNames"),
      lightFileNames = light.map("fileNames"),
      lightFileExtensions = light.map("fileExtensions"),
      lightFolderNames = light.map("folderNames"),
    )
  }

  /** Icon name for a file or folder called [name] (the last path segment). */
  fun iconName(context: Context, name: String, folder: Boolean, dark: Boolean): String {
    val t = tables(context)
    val key = name.lowercase()
    if (folder) {
      return (if (dark) null else t.lightFolderNames[key]) ?: t.folderNames[key] ?: t.folder
    }
    if (!dark) t.lightFileNames[key]?.let { return it }
    t.fileNames[key]?.let { return it }
    // "a.test.ts" tries "test.ts", then "ts".
    var dot = key.indexOf('.')
    while (dot >= 0 && dot < key.lastIndex) {
      val extension = key.substring(dot + 1)
      if (!dark) t.lightFileExtensions[extension]?.let { return it }
      t.fileExtensions[extension]?.let { return it }
      dot = key.indexOf('.', dot + 1)
    }
    return t.file
  }

  fun cached(icon: String): Picture? = pictures.get(icon)

  /** The icon if everything it needs is already in memory, so a recycled row draws on its first frame. */
  fun peek(context: Context, name: String, folder: Boolean, dark: Boolean): Picture? {
    if (tables == null) return null
    return pictures.get(iconName(context, name, folder, dark))
  }

  /** Parses and records the icon; call off the main thread. Null when the asset can't be read. */
  fun picture(context: Context, icon: String): Picture? {
    pictures.get(icon)?.let { return it }
    val picture = runCatching {
      SVG.getFromAsset(context.assets, "$DIR/$icon.svg").renderToPicture(96, 96)
    }.getOrNull() ?: return null
    pictures.put(icon, picture)
    return picture
  }
}

/** Material Icon Theme icon for a repository entry; parsing happens off the main thread once. */
@Composable
fun FileTypeIcon(name: String, folder: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp) {
  val context = LocalContext.current.applicationContext
  val dark = Gl.colors.isDark
  // Keyed so a recycled row never shows the previous entry's icon, even for a frame.
  val state = remember(name, folder, dark) { mutableStateOf(FileIcons.peek(context, name, folder, dark)) }
  LaunchedEffect(name, folder, dark) {
    if (state.value == null) {
      state.value = withContext(Dispatchers.Default) {
        val icon = FileIcons.iconName(context, name, folder, dark)
        FileIcons.cached(icon) ?: FileIcons.picture(context, icon)
      }
    }
  }
  val picture = state.value
  Canvas(modifier.size(size)) {
    val p = picture ?: return@Canvas
    drawIntoCanvas { it.nativeCanvas.drawPicture(p, RectF(0f, 0f, this.size.width, this.size.height)) }
  }
}
