package com.aistudio.gitlab

import com.aistudio.gitlab.data.api.GitLabApiClient
import com.aistudio.gitlab.data.api.GitLabApiException
import com.aistudio.gitlab.data.api.normalizeBaseUrl
import com.aistudio.gitlab.data.auth.GitLabAuth
import com.aistudio.gitlab.data.model.GitLabInstance
import com.aistudio.gitlab.data.repository.GitLabRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class GitLabRepositoryTest {

  private lateinit var server: MockWebServer
  private lateinit var instance: GitLabInstance
  private val repository = GitLabRepository()

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
      authKind = GitLabAuth.KIND_PAT
    )
  }

  @After
  fun tearDown() {
    GitLabApiClient.clientOverride = null
    GitLabApiClient.clearCache()
    server.shutdown()
  }

  @Test
  fun `commit history is bounded to one page even when the server has more`() = runBlocking {
    server.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        val page = request.requestUrl?.queryParameter("page")?.toInt() ?: 1
        return MockResponse()
          .setBody("""[{"id":"sha$page","title":"commit $page"}]""")
          .addHeader("Content-Type", "application/json")
          .addHeader("X-Next-Page", (page + 1).toString())
      }
    }

    val commits = repository.getRepositoryCommits(instance, 1, "main").getOrThrow()

    assertEquals(1, commits.size)
    assertEquals(1, server.requestCount)
  }

  @Test
  fun `explicit nulls fall back to model defaults instead of failing the list`() = runBlocking {
    server.enqueue(
      MockResponse().addHeader("Content-Type", "application/json").setBody(
        """[
          {"id":1,"project":null,"author":null,"action_name":"assigned","target_type":"Issue",
           "target":{"iid":7,"title":"Fix login","project_id":3,"state":"opened"},"body":null,"state":"pending","created_at":null},
          {"id":2,"author":{"id":5,"username":"dev","name":null},"action_name":"mentioned","state":"done"}
        ]"""
      )
    )
    server.enqueue(MockResponse().addHeader("Content-Type", "application/json").setBody("[]"))

    val todos = repository.getTodos(instance).getOrThrow()

    assertEquals(listOf(1L, 2L), todos.map { it.id }.sorted())
    val first = todos.first { it.id == 1L }
    assertEquals("", first.author.username)
    assertEquals(7L, first.target?.iid)
    assertEquals("", todos.first { it.id == 2L }.author.name)
  }

  @Test
  fun `http errors carry a message that says how to recover`() = runBlocking {
    server.enqueue(MockResponse().setResponseCode(429).setBody("Retry later"))

    val error = repository.getProject(instance, 1).exceptionOrNull()

    assertTrue(error is GitLabApiException)
    assertEquals(429, (error as GitLabApiException).code)
  }

  @Test
  fun `html answers from a proxy become a readable error`() = runBlocking {
    server.enqueue(MockResponse().addHeader("Content-Type", "text/html").setBody("<html>Sign in</html>"))

    val error = repository.getProject(instance, 1).exceptionOrNull()

    assertTrue(error is GitLabApiException)
    assertTrue(error!!.message!!.contains("GitLab server"))
  }

  @Test
  fun `cancellation propagates instead of turning into a failure`() = runBlocking {
    server.enqueue(
      MockResponse()
        .addHeader("Content-Type", "application/json")
        .setBody("{}")
        .setHeadersDelay(5, TimeUnit.SECONDS)
    )
    val call = async { repository.getProject(instance, 1) }
    Thread.sleep(200)
    call.cancel()

    val outcome = runCatching { withTimeout(2_000) { call.await() } }

    assertTrue(outcome.exceptionOrNull() is CancellationException)
  }

  @Test
  fun `file content is read past the first buffer segment`() = runBlocking {
    val body = "x".repeat(50_000)
    server.enqueue(MockResponse().setBody(body))

    val content = repository.getFileContent(instance, 1, "big.txt", "main").getOrThrow()

    assertEquals(50_000, content.length)
  }

  @Test
  fun `star on an already starred project is not an error`() = runBlocking {
    server.enqueue(MockResponse().setResponseCode(304))
    server.enqueue(
      MockResponse().addHeader("Content-Type", "application/json")
        .setBody("""{"id":9,"name":"app","star_count":3}""")
    )

    val project = repository.setProjectStarred(instance, 9, starred = true).getOrThrow()

    assertTrue(project.isStarred)
    assertEquals(3, project.starCount)
  }

  @Test
  fun `without a token the project feed is popular public projects`() = runBlocking {
    server.enqueue(MockResponse().addHeader("Content-Type", "application/json").setBody("""[{"id":1,"name":"gitlab"}]"""))

    val projects = repository.getProjects(instance.copy(token = "", authKind = GitLabAuth.KIND_PUBLIC)).getOrThrow()

    assertEquals(listOf(1L), projects.map { it.id })
    val url = server.takeRequest().requestUrl!!
    assertEquals("public", url.queryParameter("visibility"))
    assertEquals("star_count", url.queryParameter("order_by"))
    assertEquals(null, url.queryParameter("membership"))
  }

  @Test
  fun `a pasted project path opens that project first`() = runBlocking {
    server.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        val body = if (request.path!!.startsWith("/api/v4/projects/gitlab-org%2Fgitlab")) """{"id":7,"name":"gitlab"}"""
        else """[{"id":8,"name":"gitlab-runner"},{"id":7,"name":"gitlab"}]"""
        return MockResponse().addHeader("Content-Type", "application/json").setBody(body)
      }
    }

    val projects = repository.getProjects(instance.copy(token = ""), search = "gitlab-org/gitlab").getOrThrow()

    assertEquals(listOf(7L, 8L), projects.map { it.id })
  }

  @Test
  fun `job logs keep only the tail`() = runBlocking {
    val log = "a".repeat(300_000) + "END"
    server.enqueue(MockResponse().setBody(log))

    val trace = repository.getJobTrace(instance, 1, 2).getOrThrow()

    assertTrue(trace.truncated)
    assertTrue(trace.text.endsWith("END"))
    assertTrue(trace.text.length <= 256_000)
  }

  @Test
  fun `languages come back largest first`() = runBlocking {
    server.enqueue(MockResponse().addHeader("Content-Type", "application/json").setBody("""{"Ruby":20.5,"Go":70.1,"Shell":9.4}"""))

    val languages = repository.getLanguages(instance, 1).getOrThrow()

    assertEquals(listOf("Go", "Ruby", "Shell"), languages.map { it.first })
  }

  @Test
  fun `missing approvals api is not an error`() = runBlocking {
    server.enqueue(MockResponse().setResponseCode(404))

    val approvals = repository.getMergeRequestApprovals(instance, 1, 2).getOrThrow()

    assertEquals(null, approvals)
  }

  @Test
  fun `hidden contributor stats read as an empty list`() = runBlocking {
    server.enqueue(MockResponse().setResponseCode(403))

    assertEquals(emptyList<Any>(), repository.getContributors(instance, 1).getOrThrow())
  }

  @Test
  fun `detailed labels keep names and carry colors`() = runBlocking {
    server.enqueue(
      MockResponse().addHeader("Content-Type", "application/json").setBody(
        """[{"id":1,"iid":2,"project_id":3,"title":"t","labels":[{"id":9,"name":"bug","color":"#dc143c","text_color":"#FFFFFF"}]}]"""
      )
    )

    val issue = repository.getProjectIssues(instance, 3).getOrThrow().single()

    assertEquals(listOf("bug"), issue.labels)
    assertEquals("#dc143c", issue.labelDetails.single().color)
    assertEquals("true", server.takeRequest().requestUrl!!.queryParameter("with_labels_details"))
  }

  @Test
  fun `api root urls are normalised`() {
    assertEquals("https://gitlab.example.com/", normalizeBaseUrl("https://gitlab.example.com/api/v4/"))
    assertEquals("https://gitlab.example.com/gitlab/", normalizeBaseUrl("https://gitlab.example.com/gitlab/api/v4?x=1#top"))
  }

  @Test
  fun `personal access tokens from current gitlab are recognised`() {
    assertEquals("glpat-new", GitLabAuth.createdAccessToken("""{"new_token":"glpat-new","active_access_tokens":[]}"""))
    assertEquals("glpat-old", GitLabAuth.createdAccessToken("""{"token":"glpat-old"}"""))
  }
}
