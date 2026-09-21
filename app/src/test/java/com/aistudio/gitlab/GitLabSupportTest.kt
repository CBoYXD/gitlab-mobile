package com.aistudio.gitlab

import com.aistudio.gitlab.data.api.encodeGitLabPath
import com.aistudio.gitlab.data.api.formatGitLabTime
import com.aistudio.gitlab.data.api.normalizeBaseUrl
import com.aistudio.gitlab.data.api.parseGitLabError
import com.aistudio.gitlab.data.api.parseGitLabMillis
import com.aistudio.gitlab.data.api.stagesFromJobs
import com.aistudio.gitlab.data.model.GitLabJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitLabSupportTest {

  @Test
  fun `normalizes server urls`() {
    assertEquals("https://gitlab.com/", normalizeBaseUrl("gitlab.com"))
    assertEquals("https://gitlab.com/", normalizeBaseUrl("https://gitlab.com/"))
    assertEquals("http://gitlab.internal/", normalizeBaseUrl("http://gitlab.internal"))
  }

  @Test
  fun `rejects an empty server url`() {
    val error = runCatching { normalizeBaseUrl("https://") }.exceptionOrNull()
    assertTrue(error is IllegalArgumentException)
  }

  @Test
  fun `encodes repository file paths`() {
    assertEquals("src%2Fmain%2FApp.kt", encodeGitLabPath("src/main/App.kt"))
    assertEquals("README.md", encodeGitLabPath("/README.md"))
  }

  @Test
  fun `formats gitlab timestamps relative to now`() {
    val parsed = parseGitLabMillis("2026-09-21T12:00:00Z")
    assertTrue(parsed != null)
    assertEquals("2h ago", formatGitLabTime("2026-09-21T12:00:00Z", parsed!! + 2 * 60 * 60 * 1000))
    assertEquals("2026-09-01", formatGitLabTime("2026-09-01T12:00:00Z", parsed + 20L * 24 * 60 * 60 * 1000))
  }

  @Test
  fun `reads gitlab error payloads`() {
    assertEquals(
      "401 Unauthorized",
      parseGitLabError("""{"message":"401 Unauthorized"}""", 401)
    )
    assertEquals(
      "\"name\":[\"has already been taken\"]",
      parseGitLabError("""{"message":{"name":["has already been taken"]}}""", 400)
    )
    assertEquals("Not found", parseGitLabError(null, 404))
  }

  @Test
  fun `groups jobs into pipeline stages in id order`() {
    val stages = stagesFromJobs(
      listOf(
        GitLabJob(id = 3, name = "deploy", stage = "deploy", status = "pending"),
        GitLabJob(id = 1, name = "compile", stage = "build", status = "success"),
        GitLabJob(id = 2, name = "unit", stage = "test", status = "failed")
      )
    )
    assertEquals(listOf("build", "test", "deploy"), stages.map { it.name })
    assertEquals(listOf("success", "failed", "pending"), stages.map { it.status })
    assertNull(stagesFromJobs(emptyList()).firstOrNull())
  }
}
