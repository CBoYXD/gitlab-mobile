package com.cboyxd.gitlab

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cboyxd.gitlab.ui.components.MarkdownContext
import com.cboyxd.gitlab.ui.screens.FileViewerScreen
import com.cboyxd.gitlab.ui.screens.ProjectCodeScreen
import com.cboyxd.gitlab.ui.theme.GitLabTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** File browser and file viewer, including line selection. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class CodeScreensTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  private val opened = mutableListOf<String>()
  private var backs = 0

  private fun showFile() = rule.setContent {
    GitLabTheme(darkTheme = true, reducedMotion = true) {
      FileViewerScreen(
        state = Fakes.file(),
        webUrl = "https://gitlab.com/example-org/rocket",
        markdown = MarkdownContext(),
        onRetry = {},
        onOpenInBrowser = {},
        onBack = { backs++ },
      )
    }
  }

  private fun line(n: Int) = rule.onNode(hasClickLabel("Select line $n"))

  @Test
  fun `the file name is the title`() {
    showFile()
    rule.onNodeWithContentDescription("launch.kt").assertIsDisplayed()
  }

  @Test
  fun `tapping a line selects it and a second tap extends the range`() {
    showFile()
    line(3).performClick()
    rule.onNodeWithText("Line 3").assertIsDisplayed()

    line(7).performClick()
    rule.onNodeWithText("Lines 3 to 7").assertIsDisplayed()
    rule.onNodeWithContentDescription("Copy lines 3 to 7").assertExists()
  }

  @Test
  fun `a range picked bottom-up reads top-down`() {
    showFile()
    line(9).performClick()
    line(4).performClick()
    rule.onNodeWithText("Lines 4 to 9").assertIsDisplayed()
  }

  @Test
  fun `tapping the selected line again clears it`() {
    showFile()
    line(5).performClick()
    line(5).performClick()
    rule.onNodeWithText("Line 5").assertDoesNotExist()
    rule.onNodeWithContentDescription("launch.kt").assertExists()
  }

  @Test
  fun `back clears the selection before leaving`() {
    showFile()
    line(2).performClick()
    rule.onNodeWithText("Line 2").assertExists()

    rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
    rule.waitForIdle()
    rule.onNodeWithText("Line 2").assertDoesNotExist()
    assertEquals(0, backs)

    rule.onNodeWithContentDescription("Back").performClick()
    assertEquals(1, backs)
  }

  @Test
  fun `the close button clears the selection`() {
    showFile()
    line(2).performClick()
    rule.onNodeWithContentDescription("Clear selection").performClick()
    rule.onNodeWithText("Line 2").assertDoesNotExist()
  }

  private fun showCode() = rule.setContent {
    GitLabTheme(darkTheme = true, reducedMotion = true) {
      ProjectCodeScreen(
        state = Fakes.codeState(),
        onBack = {},
        onOpenPath = { opened += "dir:$it" },
        onOpenFile = { opened += "file:$it" },
        onChangeRef = {},
        onLoadRefs = {},
        onFilterRefs = {},
        onOpenCommits = {},
        onOpenInBrowser = {},
      )
    }
  }

  @Test
  fun `the code browser lists folders and files by name`() {
    showCode()
    listOf("src", "docs", "README.md", "build.gradle.kts").forEach {
      rule.onNodeWithContentDescription(it, substring = true).assertExists()
    }
    rule.onNodeWithContentDescription("Folder src").assertExists()
    rule.onNodeWithContentDescription("File README.md").assertExists()
  }

  @Test
  fun `rows open folders and files`() {
    showCode()
    rule.onNodeWithContentDescription("Folder src").performClick()
    rule.onNodeWithContentDescription("File README.md").performClick()
    assertEquals(listOf("dir:src", "file:README.md"), opened)
  }

  @Test
  fun `the branch row shows the ref with a change action`() {
    showCode()
    rule.onNodeWithText("main", substring = true).assertExists()
    rule.onNodeWithText("Change").assertIsDisplayed()
  }
}
