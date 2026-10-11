package com.cboyxd.gitlab

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cboyxd.gitlab.data.model.GitLabIssue
import com.cboyxd.gitlab.data.model.GitLabMergeRequest
import com.cboyxd.gitlab.ui.screens.IssuesScreen
import com.cboyxd.gitlab.ui.screens.MergeRequestsScreen
import com.cboyxd.gitlab.ui.theme.GitLabTheme
import com.cboyxd.gitlab.ui.viewmodel.Loadable
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Issues and merge requests lists: GitHub-style rows, labels and the state dropdown. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ListScreensTest {

  @get:Rule val rule = createComposeRule()

  private val openedIssues = mutableListOf<GitLabIssue>()
  private val openedMrs = mutableListOf<GitLabMergeRequest>()

  private fun showIssues() = rule.setContent {
    GitLabTheme(darkTheme = true, reducedMotion = true) {
      IssuesScreen(
        issues = Loadable(Fakes.issues, loaded = true),
        projects = Fakes.projects,
        onSelectIssue = { openedIssues += it },
        onCreateIssue = { _, _, _, _, _ -> },
        onRetry = {},
        onBack = {},
      )
    }
  }

  private fun showMergeRequests() = rule.setContent {
    GitLabTheme(darkTheme = true, reducedMotion = true) {
      MergeRequestsScreen(
        mergeRequests = Loadable(Fakes.mergeRequests, loaded = true),
        projects = Fakes.projects,
        onSelectMergeRequest = { openedMrs += it },
        onCreateMergeRequest = { _, _, _, _, _, _ -> },
        onLoadBranches = { _, _ -> Result.success(emptyList()) },
        onRetry = {},
        onBack = {},
      )
    }
  }

  @Test
  fun `issue rows show the project reference, title and labels`() {
    showIssues()
    rule.onNodeWithText("example-org / rocket #7", substring = true).assertExists()
    rule.onNodeWithText("Launch button does nothing on tablets").assertIsDisplayed()
    rule.onNodeWithContentDescription("Label bug", useUnmergedTree = true).assertExists()
    // Scoped labels are announced as key and value.
    rule.onNodeWithContentDescription("Label priority high", useUnmergedTree = true).assertExists()
  }

  @Test
  fun `issues start on open and the state dropdown switches to closed`() {
    showIssues()
    rule.onNodeWithText("Old crash on startup").assertDoesNotExist()

    rule.onNodeWithContentDescription("State: Open").performClick()
    rule.onNodeWithText("Closed").performClick()
    rule.waitForIdle()

    rule.onNodeWithText("Old crash on startup").assertExists()
    rule.onNodeWithText("Launch button does nothing on tablets").assertDoesNotExist()
    rule.onNodeWithContentDescription("State: Closed").assertExists()
  }

  @Test
  fun `tapping an issue row opens it`() {
    showIssues()
    rule.onNodeWithText("Add dark mode to the settings page").performClick()
    assertEquals(listOf(6L), openedIssues.map { it.iid })
  }

  @Test
  fun `merge request rows use the bang reference and show drafts`() {
    showMergeRequests()
    rule.onNodeWithText("example-org / rocket !12", substring = true).assertExists()
    rule.onNodeWithText("Fix the launch button on tablets").assertIsDisplayed()
    assertTrue(rule.onAllNodesWithContentDescription("Label bug", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    assertTrue(rule.onAllNodesWithContentDescription("Draft", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    // Merged ones are filtered out of the default Open view.
    rule.onNodeWithText("Bump the version").assertDoesNotExist()
  }

  @Test
  fun `tapping a merge request row opens it`() {
    showMergeRequests()
    rule.onNodeWithText("Fix the launch button on tablets").performClick()
    assertEquals(listOf(12L), openedMrs.map { it.iid })
  }
}
