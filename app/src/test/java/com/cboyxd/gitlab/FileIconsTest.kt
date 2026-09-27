package com.cboyxd.gitlab

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.cboyxd.gitlab.ui.components.FileIcons
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** File and folder icon lookup against the bundled Material Icon Theme tables. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class FileIconsTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  private fun file(name: String, dark: Boolean = true) = FileIcons.iconName(context, name, folder = false, dark = dark)
  private fun folder(name: String, dark: Boolean = true) = FileIcons.iconName(context, name, folder = true, dark = dark)

  @Test
  fun `exact file names win over extensions`() {
    assertEquals("docker", file("Dockerfile"))
    assertEquals("git", file(".gitignore"))
    assertEquals("nodejs", file("package.json"))
    assertEquals("readme", file("README.md"))
  }

  @Test
  fun `the longest matching extension wins`() {
    assertEquals("test-ts", file("button.spec.ts"))
    assertEquals("typescript", file("button.ts"))
    assertEquals("typescript-def", file("index.d.ts"))
    assertEquals("gitlab", file(".gitlab-ci.yml"))
  }

  @Test
  fun `common languages get their icons`() {
    assertEquals("kotlin", file("MainActivity.kt"))
    assertEquals("kotlin", file("build.gradle.kts"))
    assertEquals("rust", file("candle_parsing.rs"))
    assertEquals("python", file("app.py"))
    assertEquals("markdown", file("release-notes.md"))
    assertEquals("react_ts", file("App.tsx"))
    assertEquals("ruby", file("app.rb"))
    assertEquals("json", file("data.json"))
    assertEquals("toml", file("Cargo.toml"))
  }

  @Test
  fun `lookup ignores case`() {
    assertEquals(file("dockerfile"), file("DOCKERFILE"))
    assertEquals("python", file("SETUP.PY"))
    assertEquals("folder-src", folder("SRC"))
  }

  @Test
  fun `unknown names fall back to the default icons`() {
    assertEquals("file", file("mystery.xyzzy"))
    assertEquals("file", file("noextension"))
    assertEquals("file", file("trailingdot."))
    assertEquals("folder", folder("some-unknown-folder-name"))
  }

  @Test
  fun `types outside the bundled popular set fall back to the plain icons`() {
    assertEquals("file", file("brew.coffee"))
    assertEquals("folder", folder("package.json"))
  }

  @Test
  fun `known folders get their icons`() {
    assertEquals("folder-src", folder("src"))
    assertEquals("folder-test", folder("test"))
    assertEquals("folder-test", folder("tests"))
    assertEquals("folder-docs", folder("docs"))
    assertEquals("folder-github", folder(".github"))
    assertEquals("folder-gitlab", folder(".gitlab"))
    assertEquals("folder-node", folder("node_modules"))
  }

  @Test
  fun `a folder named like a file is still a folder`() {
    assertTrue(folder("src.kt").startsWith("folder"))
    assertTrue(!file("src").startsWith("folder"))
  }

  @Test
  fun `the light theme uses light variants where the theme has them`() {
    assertEquals("toml", file("Cargo.toml", dark = true))
    assertEquals("toml_light", file("Cargo.toml", dark = false))
    assertEquals("pnpm_light", file("pnpm-lock.yaml", dark = false))
    assertEquals("folder-intellij", folder(".idea", dark = true))
    assertEquals("folder-intellij_light", folder(".idea", dark = false))
    // Icons without a light variant stay the same.
    assertEquals(file("app.py", dark = true), file("app.py", dark = false))
    assertNotEquals(file("Cargo.toml", dark = true), file("Cargo.toml", dark = false))
  }

  @Test
  fun `every icon the tables reference is bundled and renders`() {
    val json = JSONObject(context.assets.open("file-icons/icons.json").bufferedReader().use { it.readText() })
    val names = sortedSetOf(json.getString("file"), json.getString("folder"))
    fun collect(obj: JSONObject?) {
      obj ?: return
      obj.keys().forEach { names += obj.getString(it) }
    }
    listOf("fileNames", "fileExtensions", "folderNames").forEach { key ->
      collect(json.optJSONObject(key))
      collect(json.optJSONObject("light")?.optJSONObject(key))
    }
    // The app ships a curated popular subset (tools/update_file_icons.py); guard against an empty export.
    assertTrue("expected over a hundred icons, got ${names.size}", names.size > 100)

    val bundled = context.assets.list("file-icons").orEmpty().toSet()
    val missing = names.filterNot { "$it.svg" in bundled }
    assertTrue("icons referenced but not bundled: $missing", missing.isEmpty())

    val broken = names.filter { FileIcons.picture(context, it) == null }
    assertTrue("icons that fail to parse: $broken", broken.isEmpty())
  }

  @Test
  fun `rendered icons are cached`() {
    val icon = file("main.rs")
    val first = FileIcons.picture(context, icon)
    assertNotNull(first)
    assertTrue(FileIcons.cached(icon) === first)
    assertTrue(FileIcons.peek(context, "main.rs", folder = false, dark = true) === first)
  }
}
