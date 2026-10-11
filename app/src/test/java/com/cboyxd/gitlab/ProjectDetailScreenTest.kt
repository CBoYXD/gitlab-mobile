package com.cboyxd.gitlab

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.cboyxd.gitlab.ui.components.MarkdownContext
import com.cboyxd.gitlab.ui.screens.ProjectDetailScreen
import com.cboyxd.gitlab.ui.theme.GitLabTheme
import com.cboyxd.gitlab.ui.viewmodel.ProjectDetailState
import com.cboyxd.gitlab.ui.viewmodel.ProjectSection
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ProjectDetailScreenTest {

  @get:Rule val rule = createComposeRule()

  private val sections = mutableListOf<ProjectSection>()
  private val releases = mutableListOf<String>()
  private var codeOpened = 0

  private fun show(state: ProjectDetailState = Fakes.projectState(), canWrite: Boolean = false) {
    rule.setContent {
      GitLabTheme(darkTheme = true, reducedMotion = true) {
        ProjectDetailScreen(
          state = state,
          pendingStar = false,
          canWrite = canWrite,
          markdown = MarkdownContext(),
          onBack = {},
          onRetry = {},
          onToggleStar = {},
          onOpenSection = { sections += it },
          onOpenCode = { codeOpened++ },
          onOpenRelease = { releases += it },
          onOpenInBrowser = {},
        )
      }
    }
  }

  private fun scrollTo(text: String) {
    rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
  }

  @Test
  fun `header shows the project and its public stats`() {
    show()
    // The name is the page heading; the top bar holds a hidden copy until the heading scrolls away.
    assertTrue(rule.onAllNodesWithText("rocket").fetchSemanticsNodes().isNotEmpty())
    rule.onNodeWithText("Public", substring = true).assertExists()
    rule.onNodeWithText("1.2k", substring = true).assertExists()
  }

  @Test
  fun `github-style rows are present and watchers are not`() {
    show()
    listOf("Issues", "Merge requests", "Pipelines", "Releases", "Contributors").forEach {
      scrollTo(it)
      rule.onNodeWithText(it).assertIsDisplayed()
    }
    rule.onNodeWithText("Watchers").assertDoesNotExist()
  }

  @Test
  fun `rows open their sections`() {
    show()
    rule.onNodeWithText("Issues").performClick()
    rule.onNodeWithText("Merge requests").performClick()
    rule.onNodeWithText("Pipelines").performClick()
    scrollTo("Contributors")
    rule.onNodeWithText("Contributors").performClick()
    scrollTo("Commits")
    rule.onNodeWithText("Commits").performClick()
    assertEquals(
      listOf(ProjectSection.ISSUES, ProjectSection.MERGE_REQUESTS, ProjectSection.PIPELINES, ProjectSection.CONTRIBUTORS, ProjectSection.COMMITS),
      sections,
    )
  }

  @Test
  fun `latest release opens its page`() {
    show()
    scrollTo("Rocket v1.2.0")
    rule.onNodeWithText("Rocket v1.2.0", substring = true).performClick()
    assertEquals(listOf("v1.2.0"), releases)
  }

  @Test
  fun `code row and current branch are shown`() {
    show()
    scrollTo("Code")
    rule.onNodeWithText("main").assertExists()
    rule.onNodeWithText("Code").performClick()
    assertEquals(1, codeOpened)
  }

  @Test
  fun `readme renders below the lists`() {
    show()
    scrollTo("README.md")
    rule.onNodeWithText("README.md").assertExists()
  }

  @Test
  fun `a project without issues hides the issues row`() {
    val state = Fakes.projectState().let { s -> s.copy(project = s.project.copy(data = s.project.data!!.copy(issuesEnabled = false))) }
    show(state)
    rule.onNodeWithText("Issues").assertDoesNotExist()
    rule.onNodeWithText("Merge requests").assertExists()
  }

  @Test
  fun `exact counts replace the cached project counter`() {
    val state = Fakes.projectState().copy(
      counts = com.cboyxd.gitlab.ui.viewmodel.Loadable(com.cboyxd.gitlab.data.model.ProjectCounts(openIssues = 21, openMergeRequests = 3), loaded = true),
    )
    show(state)
    rule.onNodeWithText("21", useUnmergedTree = true).assertExists()
    rule.onNodeWithText("3", useUnmergedTree = true).assertExists()
    // The project's cached open_issues_count (12) is gone once exact counts are in.
    rule.onNodeWithText("12", useUnmergedTree = true).assertDoesNotExist()
  }

  @Test
  fun `the cached counter stands in until counts load`() {
    val state = Fakes.projectState().copy(counts = com.cboyxd.gitlab.ui.viewmodel.Loadable(null, loading = true))
    show(state)
    rule.onNodeWithText("12", useUnmergedTree = true).assertExists()
    rule.onNodeWithContentDescription("Loading", useUnmergedTree = true).assertExists()
  }
}
