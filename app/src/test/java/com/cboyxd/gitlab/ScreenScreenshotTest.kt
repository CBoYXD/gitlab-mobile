package com.cboyxd.gitlab

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.cboyxd.gitlab.ui.components.MarkdownContext
import com.cboyxd.gitlab.ui.screens.HomeScreen
import com.cboyxd.gitlab.ui.screens.ProjectDetailScreen
import com.cboyxd.gitlab.ui.theme.GitLabTheme
import com.cboyxd.gitlab.ui.viewmodel.GitLabUiState
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots of the main screens with invented data, in both themes. Record with
 * `./gradlew :app:recordRoborazziDebug`, compare with `:app:verifyRoborazziDebug`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ScreenScreenshotTest {

  @get:Rule val rule = createComposeRule()

  private fun capture(name: String, dark: Boolean, content: @Composable () -> Unit) {
    rule.setContent { GitLabTheme(darkTheme = dark, reducedMotion = true, content = content) }
    rule.waitForIdle()
    rule.onRoot().captureRoboImage(filePath = "src/test/screenshots/${name}_${if (dark) "dark" else "light"}.png")
  }

  @Composable
  private fun Home(state: GitLabUiState) = HomeScreen(
    state = state,
    onRefresh = {},
    onOpenProfile = {},
    onOpenSearch = {},
    onAddServer = {},
    onSignInAgain = {},
    onNavigate = {},
    onSwitchTab = {},
    onToggleStar = {},
  )

  @Composable
  private fun Project() = ProjectDetailScreen(
    state = Fakes.projectState(),
    pendingStar = false,
    canWrite = true,
    markdown = MarkdownContext(),
    onBack = {},
    onRetry = {},
    onToggleStar = {},
    onOpenSection = {},
    onOpenCode = {},
    onOpenRelease = {},
    onOpenInBrowser = {},
  )

  @Test fun home_signed_in_dark() = capture("home_signed_in", dark = true) { Home(Fakes.signedInState()) }
  @Test fun home_signed_in_light() = capture("home_signed_in", dark = false) { Home(Fakes.signedInState()) }
  @Test fun home_guest_dark() = capture("home_guest", dark = true) { Home(Fakes.guestState()) }
  @Test fun home_guest_light() = capture("home_guest", dark = false) { Home(Fakes.guestState()) }
  @Test fun project_dark() = capture("project", dark = true) { Project() }
  @Test fun project_light() = capture("project", dark = false) { Project() }
}
