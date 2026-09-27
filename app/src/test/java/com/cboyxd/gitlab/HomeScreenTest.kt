package com.cboyxd.gitlab

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.cboyxd.gitlab.ui.screens.HomeScreen
import com.cboyxd.gitlab.ui.theme.GitLabTheme
import com.cboyxd.gitlab.ui.viewmodel.BottomTab
import com.cboyxd.gitlab.ui.viewmodel.GitLabUiState
import com.cboyxd.gitlab.ui.viewmodel.ScreenDestination
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
class HomeScreenTest {

  @get:Rule val rule = createComposeRule()

  private val navigated = mutableListOf<ScreenDestination>()
  private val tabs = mutableListOf<BottomTab>()
  private var signInClicks = 0
  private var profileClicks = 0

  private fun show(state: GitLabUiState) {
    rule.setContent {
      GitLabTheme(darkTheme = true, reducedMotion = true) {
        HomeScreen(
          state = state,
          onRefresh = {},
          onOpenProfile = { profileClicks++ },
          onOpenSearch = {},
          onAddServer = {},
          onSignInAgain = { signInClicks++ },
          onNavigate = { navigated += it },
          onSwitchTab = { tabs += it },
          onToggleStar = {},
        )
      }
    }
  }

  private fun scrollTo(text: String) {
    rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
  }

  @Test
  fun `guest sees the sign-in banner and popular projects, not my work`() {
    show(Fakes.guestState())

    rule.onNodeWithText("browsing as a guest", substring = true).assertIsDisplayed()
    rule.onNodeWithText("Sign in", ignoreCase = true).performClick()
    assertEquals(1, signInClicks)

    rule.onNodeWithText("My work").assertDoesNotExist()
    rule.onNodeWithText("Popular projects").assertIsDisplayed()
    scrollTo("rocket")
    rule.onNodeWithText("rocket").assertIsDisplayed()
  }

  @Test
  fun `guest avatar opens the account entry point`() {
    show(Fakes.guestState())
    rule.onNodeWithContentDescription("Account").performClick()
    assertEquals(1, profileClicks)
  }

  @Test
  fun `signed-in home lists my work in order`() {
    show(Fakes.signedInState())

    rule.onNodeWithText("My work").assertIsDisplayed()
    listOf("Issues", "Merge requests", "Pipelines", "Your projects", "Groups").forEach {
      rule.onNodeWithText(it).assertIsDisplayed()
    }
    rule.onNodeWithText("browsing as a guest", substring = true).assertDoesNotExist()
  }

  @Test
  fun `items the user removed are gone`() {
    show(Fakes.signedInState())
    listOf("Discussions", "Projects", "Starred", "Top repositories", "Organizations").forEach {
      rule.onNodeWithText(it).assertDoesNotExist()
    }
  }

  @Test
  fun `my work rows open their screens`() {
    show(Fakes.signedInState())

    rule.onNodeWithText("Issues").performClick()
    rule.onNodeWithText("Merge requests").performClick()
    rule.onNodeWithText("Pipelines").performClick()
    rule.onNodeWithText("Groups").performClick()
    rule.onNodeWithText("Your projects").performClick()

    assertEquals(
      listOf(ScreenDestination.IssuesList, ScreenDestination.MergeRequestsList, ScreenDestination.PipelinesList, ScreenDestination.GroupsList),
      navigated,
    )
    assertEquals(listOf(BottomTab.PROJECTS), tabs)
  }

  @Test
  fun `favorites show only starred projects and open them`() {
    show(Fakes.signedInState())

    scrollTo("Favorites")
    rule.onNodeWithText("Favorites").assertIsDisplayed()
    scrollTo("comet")
    rule.onNodeWithText("rocket").assertExists()
    rule.onNodeWithText("comet").assertExists()
    rule.onNodeWithText("asteroid").assertDoesNotExist()

    rule.onNodeWithText("comet").performClick()
    assertTrue(navigated.contains(ScreenDestination.ProjectDetail(2)))
  }

  @Test
  fun `favorites explain how to fill them when nothing is starred`() {
    val state = Fakes.signedInState().let { it.copy(projects = it.projects.copy(data = it.projects.data.map { p -> p.copy(isStarred = false) })) }
    show(state)

    scrollTo("Browse projects")
    rule.onNodeWithText("Browse projects").performClick()
    assertEquals(listOf(BottomTab.PROJECTS), tabs)
  }
}
