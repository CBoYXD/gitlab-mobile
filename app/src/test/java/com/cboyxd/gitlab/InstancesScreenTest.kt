package com.cboyxd.gitlab

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cboyxd.gitlab.data.auth.GitLabAuth
import com.cboyxd.gitlab.data.model.GitLabInstance
import com.cboyxd.gitlab.ui.screens.InstancesScreen
import com.cboyxd.gitlab.ui.theme.GitLabTheme
import com.cboyxd.gitlab.ui.viewmodel.LoginState
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Sign-in: guest browsing is the default, never a sign-in method. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class InstancesScreenTest {

  @get:Rule val rule = createComposeRule()

  private fun show(active: GitLabInstance, saved: List<GitLabInstance>, openAdd: Boolean) {
    rule.setContent {
      GitLabTheme(darkTheme = true, reducedMotion = true) {
        InstancesScreen(
          activeInstance = active,
          savedInstances = saved,
          loginState = LoginState(),
          savedClientId = "",
          openAddOnStart = openAdd,
          onAddOpened = {},
          onSwitchInstance = {},
          onDeleteInstance = {},
          onSignInWithPassword = { _, _, _, _ -> },
          onSubmitTwoFactor = {},
          onSignInWithOAuth = { _, _, _ -> },
          onSignInWithToken = { _, _, _ -> },
          onClearLogin = {},
          onBack = {},
        )
      }
    }
  }

  @Test
  fun `guest without accounts is invited to sign in`() {
    show(GitLabAuth.GUEST, emptyList(), openAdd = false)
    rule.onNodeWithText("browsing as a guest", substring = true).assertIsDisplayed()
    // The guest is not a saved server.
    rule.onNodeWithText("Signed-in accounts").assertDoesNotExist()
    rule.onNodeWithText("Sign in").performClick()
    rule.waitForIdle()
    rule.onNodeWithText("Sign-in method").assertExists()
  }

  @Test
  fun `sign-in methods are token, oauth and password only`() {
    show(GitLabAuth.GUEST, emptyList(), openAdd = true)
    rule.waitForIdle()

    rule.onNodeWithText("Sign-in method").assertExists()
    assertTrue(rule.onAllNodesWithText("Access token").fetchSemanticsNodes().isNotEmpty())
    rule.onNodeWithText("OAuth").assertExists()
    rule.onNodeWithText("Password").assertExists()
    rule.onNodeWithText("No sign-in").assertDoesNotExist()
    rule.onNodeWithText("Browse without signing in").assertDoesNotExist()
  }

  @Test
  fun `a signed-in account is listed and guest is not offered`() {
    show(Fakes.signedIn, listOf(Fakes.signedIn), openAdd = false)
    rule.onNodeWithText("Signed-in accounts").assertExists()
    assertTrue(rule.onAllNodesWithText("@tanuki", substring = true).fetchSemanticsNodes().isNotEmpty())
    rule.onNodeWithText("No sign-in").assertDoesNotExist()
    rule.onNodeWithText("Guest", substring = true).assertDoesNotExist()
  }
}
