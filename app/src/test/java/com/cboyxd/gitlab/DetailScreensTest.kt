package com.cboyxd.gitlab

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.cboyxd.gitlab.ui.screens.IssueDetailScreen
import com.cboyxd.gitlab.ui.screens.MergeRequestDetailScreen
import com.cboyxd.gitlab.ui.theme.GitLabTheme
import com.cboyxd.gitlab.ui.viewmodel.IssueDetailState
import com.cboyxd.gitlab.ui.viewmodel.MergeRequestDetailState
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class DetailScreensTest {

  @get:Rule val rule = createComposeRule()

  private fun showIssue(state: IssueDetailState, canWrite: Boolean) = rule.setContent {
    GitLabTheme(darkTheme = true, reducedMotion = true) {
      IssueDetailScreen(
        state = state,
        labels = Fakes.labels,
        canWrite = canWrite,
        onLink = { false },
        onRetry = {},
        onComment = { _, _ -> },
        onSetClosed = {},
        onEdit = { _, _, _ -> },
        onOpenUser = {},
        onOpenInBrowser = {},
        onBack = {},
      )
    }
  }

  private fun showMr(state: MergeRequestDetailState = Fakes.mergeRequestDetail(), canWrite: Boolean = true) = rule.setContent {
    GitLabTheme(darkTheme = true, reducedMotion = true) {
      MergeRequestDetailScreen(
        state = state,
        labels = Fakes.labels,
        canWrite = canWrite,
        onLink = { false },
        onRetry = {},
        onMerge = {},
        onSetClosed = {},
        onComment = { _, _ -> },
        onOpenPipeline = {},
        onOpenCommit = {},
        onOpenUser = {},
        onOpenInBrowser = {},
        onBack = {},
      )
    }
  }

  private fun scrollTo(text: String) {
    rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text, substring = true))
  }

  @Test
  fun `issue shows reference, title and the open pill`() {
    showIssue(Fakes.issueDetail(), canWrite = true)
    assertTrue(rule.onAllNodesWithText("example-org / rocket #7", substring = true).fetchSemanticsNodes().isNotEmpty())
    rule.onAllNodesWithText("Launch button does nothing on tablets").onFirst().assertExists()
    rule.onNodeWithText("Open").assertIsDisplayed()
  }

  @Test
  fun `signed-in viewers get the comment button and see comments`() {
    showIssue(Fakes.issueDetail(), canWrite = true)
    rule.onNodeWithText("COMMENT").assertIsDisplayed()
    scrollTo("I can reproduce this.")
    rule.onNodeWithText("I can reproduce this.", substring = true).assertExists()
  }

  @Test
  fun `guests see a sign-in hint instead of an error when comments are hidden`() {
    showIssue(Fakes.issueDetail(notesFailed = true), canWrite = false)
    rule.onNodeWithText("COMMENT").assertDoesNotExist()
    scrollTo("Sign in to see comments and activity.")
    rule.onNodeWithText("Sign in to see comments and activity.").assertExists()
    rule.onNodeWithText("403 Forbidden", substring = true).assertDoesNotExist()
    rule.onNodeWithText("Unable to load").assertDoesNotExist()
  }

  @Test
  fun `signed-in viewers still see the real error when comments fail`() {
    showIssue(Fakes.issueDetail(notesFailed = true), canWrite = true)
    rule.onNodeWithText("Sign in to see comments and activity.").assertDoesNotExist()
  }

  @Test
  fun `merge request page has changes, status and conversation`() {
    showMr()
    rule.onNodeWithText("fix-launch").assertExists()
    listOf("Changes", "2 files changed", "1 commit", "Status", "Ready to merge", "Conversation").forEach {
      scrollTo(it)
      rule.onNodeWithText(it, substring = true).assertExists()
    }
    rule.onNodeWithText("MERGE").assertExists()
  }

  @Test
  fun `guests get no merge button`() {
    showMr(canWrite = false)
    scrollTo("Status")
    rule.onNodeWithText("MERGE").assertDoesNotExist()
    rule.onNodeWithText("COMMENT").assertDoesNotExist()
  }

  @Test
  fun `files changed opens the diff view`() {
    showMr()
    scrollTo("2 files changed")
    rule.onNodeWithText("2 files changed").performClick()
    rule.waitForIdle()

    rule.onNodeWithText("Files changed").assertIsDisplayed()
    rule.onNodeWithText("+2 −1", substring = true).assertExists()
    rule.onNodeWithText("src/launch.kt", substring = true).assertExists()
    rule.onNodeWithText("ignite()", substring = true).assertExists()
  }
}
