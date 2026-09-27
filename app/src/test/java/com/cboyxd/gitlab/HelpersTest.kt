package com.cboyxd.gitlab

import com.cboyxd.gitlab.data.api.formatGitLabTime
import com.cboyxd.gitlab.data.api.parseGitLabMillis
import com.cboyxd.gitlab.data.auth.GitLabAuth
import com.cboyxd.gitlab.ui.components.formatCompact
import com.cboyxd.gitlab.ui.components.formatCount
import com.cboyxd.gitlab.ui.components.formatDuration
import com.cboyxd.gitlab.ui.components.languageForFence
import com.cboyxd.gitlab.ui.components.languageForPath
import dev.snipme.highlights.model.SyntaxLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure helpers: guest account, counters, durations, times, language detection. */
class HelpersTest {

  @Test
  fun `guest browses gitlab com without credentials`() {
    val guest = GitLabAuth.GUEST
    assertTrue(GitLabAuth.isGitLabCom(guest.url))
    assertEquals("", guest.token)
    assertEquals(GitLabAuth.KIND_PUBLIC, guest.authKind)
    assertFalse(guest.isCustom)
    // A stable id, so per-instance caches survive recomposition and restarts.
    assertEquals(GitLabAuth.GUEST.id, GitLabAuth.GUEST.copy().id)
  }

  @Test
  fun `counts use thin-space grouping`() {
    assertEquals("0", formatCount(0))
    assertEquals("999", formatCount(999))
    assertEquals("12 881", formatCount(12_881))
    assertEquals("1 234 567", formatCount(1_234_567))
  }

  @Test
  fun `compact counts read like GitHub`() {
    assertEquals("999", formatCompact(999))
    assertEquals("1k", formatCompact(1_000))
    assertEquals("6.1k", formatCompact(6_100))
    assertEquals("12.5k", formatCompact(12_500))
    assertEquals("125k", formatCompact(125_000))
    assertEquals("1.2M", formatCompact(1_200_000))
  }

  @Test
  fun `durations pick the two largest units`() {
    assertEquals("0s", formatDuration(0))
    assertEquals("59s", formatDuration(59))
    assertEquals("1m 0s", formatDuration(60))
    assertEquals("12m 5s", formatDuration(725))
    assertEquals("2h 3m", formatDuration(2 * 3600 + 3 * 60 + 9))
  }

  @Test
  fun `relative times cover every range`() {
    val base = parseGitLabMillis("2026-09-21T12:00:00Z")!!
    assertEquals("just now", formatGitLabTime("2026-09-21T12:00:00Z", base + 30_000))
    assertEquals("5m ago", formatGitLabTime("2026-09-21T12:00:00Z", base + 5 * 60_000))
    assertEquals("3d ago", formatGitLabTime("2026-09-21T12:00:00Z", base + 3L * 86_400_000))
    // Clock skew never produces a negative age.
    assertEquals("just now", formatGitLabTime("2026-09-21T12:00:00Z", base - 60_000))
    assertEquals("", formatGitLabTime(""))
    assertEquals("not-a-dat", formatGitLabTime("not-a-date-at-all").take(9))
  }

  @Test
  fun `timestamps parse with offsets and fractions`() {
    val utc = parseGitLabMillis("2026-09-21T12:00:00Z")
    assertNotNull(utc)
    assertEquals(utc, parseGitLabMillis("2026-09-21T12:00:00.000Z"))
    assertEquals(utc, parseGitLabMillis("2026-09-21T15:00:00.000+03:00"))
    assertNull(parseGitLabMillis("yesterday"))
  }

  @Test
  fun `languages are detected from paths`() {
    assertEquals(SyntaxLanguage.KOTLIN, languageForPath("app/src/Main.kt"))
    assertEquals(SyntaxLanguage.KOTLIN, languageForPath("build.gradle.kts"))
    assertEquals(SyntaxLanguage.RUST, languageForPath("src/candle_parsing.rs"))
    assertEquals(SyntaxLanguage.PYTHON, languageForPath("tools/update_file_icons.py"))
    assertEquals(SyntaxLanguage.RUBY, languageForPath("Gemfile"))
    assertEquals(SyntaxLanguage.TYPESCRIPT, languageForPath("web/App.TSX"))
    assertEquals(SyntaxLanguage.DEFAULT, languageForPath("notes.txt"))
    assertEquals(SyntaxLanguage.DEFAULT, languageForPath("LICENSE"))
  }

  @Test
  fun `languages are detected from code fences`() {
    assertEquals(SyntaxLanguage.KOTLIN, languageForFence("kotlin"))
    assertEquals(SyntaxLanguage.RUST, languageForFence("rs"))
    assertEquals(SyntaxLanguage.SHELL, languageForFence("console title=\"x\""))
    assertEquals(SyntaxLanguage.DEFAULT, languageForFence(null))
    assertEquals(SyntaxLanguage.DEFAULT, languageForFence("  "))
  }
}
