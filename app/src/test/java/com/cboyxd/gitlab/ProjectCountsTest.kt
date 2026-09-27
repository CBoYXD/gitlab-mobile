package com.cboyxd.gitlab

import com.cboyxd.gitlab.data.api.GitLabApiClient
import com.cboyxd.gitlab.data.auth.GitLabAuth
import com.cboyxd.gitlab.data.model.GitLabInstance
import com.cboyxd.gitlab.data.repository.GitLabRepository
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Collections

/** Exact open issue and merge request counts for the project page. */
class ProjectCountsTest {

  private lateinit var server: MockWebServer
  private lateinit var instance: GitLabInstance
  private val repository = GitLabRepository()
  private val paths: MutableList<String> = Collections.synchronizedList(mutableListOf())

  @Before
  fun setUp() {
    val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
    val certificates = HandshakeCertificates.Builder()
      .heldCertificate(certificate)
      .addTrustedCertificate(certificate.certificate)
      .build()
    server = MockWebServer()
    server.useHttps(certificates.sslSocketFactory(), false)
    server.start()
    GitLabApiClient.clearCache()
    GitLabApiClient.clientOverride = OkHttpClient.Builder()
      .sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager)
      .build()
    instance = GitLabInstance(
      id = "test-" + System.nanoTime(),
      name = "Test",
      url = server.url("/").toString(),
      token = "glpat-test",
      authKind = GitLabAuth.KIND_PAT,
    )
  }

  @After
  fun tearDown() {
    GitLabApiClient.clientOverride = null
    GitLabApiClient.clearCache()
    server.shutdown()
  }

  /** Routes the two count requests; either side can be replaced to simulate a failure. */
  private fun serve(
    statistics: MockResponse = json("""{"statistics":{"counts":{"all":30,"closed":9,"opened":21}}}"""),
    mergeRequests: MockResponse = json(ONE_MR).addHeader("X-Total", "3"),
  ) {
    server.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        val url = request.requestUrl!!
        paths += url.encodedPath + "?" + (url.query ?: "")
        return when {
          url.encodedPath.endsWith("/projects/7/issues_statistics") -> statistics
          url.encodedPath.endsWith("/projects/7/merge_requests") -> mergeRequests
          else -> MockResponse().setResponseCode(404)
        }
      }
    }
  }

  private companion object {
    const val ONE_MR = """[{"id":201,"iid":12,"project_id":7,"title":"Fix the launch button","state":"opened"}]"""
  }

  private fun json(body: String) = MockResponse().addHeader("Content-Type", "application/json").setBody(body)

  @Test
  fun `both counts are read`() = runBlocking {
    serve()
    val counts = repository.getProjectCounts(instance, 7).getOrThrow()
    assertEquals(21, counts.openIssues)
    assertEquals(3, counts.openMergeRequests)
  }

  @Test
  fun `the merge request count asks for one open item per page`() = runBlocking {
    serve()
    repository.getProjectCounts(instance, 7).getOrThrow()
    val mr = paths.first { it.contains("/merge_requests") }
    assertTrue(mr, mr.contains("state=opened"))
    assertTrue(mr, mr.contains("per_page=1"))
  }

  @Test
  fun `a missing X-Total header leaves the merge request count unknown`() = runBlocking {
    serve(mergeRequests = json(ONE_MR))
    val counts = repository.getProjectCounts(instance, 7).getOrThrow()
    assertEquals(21, counts.openIssues)
    assertNull(counts.openMergeRequests)
  }

  @Test
  fun `a forbidden statistics call keeps the merge request count`() = runBlocking {
    serve(statistics = MockResponse().setResponseCode(403).setBody("""{"message":"403 Forbidden"}"""))
    val counts = repository.getProjectCounts(instance, 7).getOrThrow()
    assertNull(counts.openIssues)
    assertEquals(3, counts.openMergeRequests)
  }

  /**
   * Known bug: getProjectCounts wraps each part in runCatchingApi, which only catches
   * GitLabApiException. A body Moshi can't read (or an IOException on one request) escapes and
   * fails the whole call, so the issue count is lost too. Unignore once each part catches all errors.
   */
  @Test
  fun `an unreadable merge request body keeps the issue count`() = runBlocking {
    serve(mergeRequests = json("""[{"unexpected":true}]""").addHeader("X-Total", "3"))
    val counts = repository.getProjectCounts(instance, 7).getOrThrow()
    assertEquals(21, counts.openIssues)
  }

  @Test
  fun `both failing is still a result with no counts`() = runBlocking {
    serve(
      statistics = MockResponse().setResponseCode(500),
      mergeRequests = MockResponse().setResponseCode(500),
    )
    val counts = repository.getProjectCounts(instance, 7).getOrThrow()
    assertNull(counts.openIssues)
    assertNull(counts.openMergeRequests)
  }
}
