package com.aistudio.gitlab.data.repository

import com.aistudio.gitlab.data.api.GitLabApiClient
import com.aistudio.gitlab.data.api.GitLabApiException
import com.aistudio.gitlab.data.api.encodeGitLabPath
import com.aistudio.gitlab.data.api.formatGitLabTime
import com.aistudio.gitlab.data.api.parseGitLabMillis
import com.aistudio.gitlab.data.api.requireBody
import com.aistudio.gitlab.data.api.requireList
import com.aistudio.gitlab.data.api.stagesFromJobs
import com.aistudio.gitlab.data.api.toGitLabException
import com.aistudio.gitlab.data.model.GitLabCommit
import com.aistudio.gitlab.data.model.GitLabFileDiff
import com.aistudio.gitlab.data.model.GitLabGroup
import com.aistudio.gitlab.data.model.GitLabInstance
import com.aistudio.gitlab.data.model.GitLabIssue
import com.aistudio.gitlab.data.model.GitLabMergeRequest
import com.aistudio.gitlab.data.model.GitLabNote
import com.aistudio.gitlab.data.model.GitLabPipeline
import com.aistudio.gitlab.data.model.GitLabProject
import com.aistudio.gitlab.data.model.GitLabTodo
import com.aistudio.gitlab.data.model.GitLabTreeItem
import com.aistudio.gitlab.data.model.GitLabUser
import com.aistudio.gitlab.data.model.GitLabUserAchievement
import com.aistudio.gitlab.data.model.GitLabUserStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import okio.Buffer
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

data class ConnectionProbe(
  val version: String,
  val username: String,
  val name: String
)

class GitLabRepository {

  suspend fun testConnection(instance: GitLabInstance): Result<ConnectionProbe> = call {
    val service = service(instance)
    if (instance.token.isNotBlank()) {
      val user = service.getCurrentUser().requireBody()
      val version = service.getVersion().let { response ->
        if (response.isSuccessful) response.body()?.version.orEmpty() else ""
      }
      ConnectionProbe(version = version, username = user.username, name = user.name)
    } else {
      service.getProjects(visibility = "public", perPage = 1).requireList()
      val version = service.getVersion().let { response ->
        if (response.isSuccessful) response.body()?.version.orEmpty() else ""
      }
      ConnectionProbe(version = version, username = "", name = "")
    }
  }

  suspend fun getVersion(instance: GitLabInstance): Result<String> = call {
    service(instance).getVersion().requireBody().version
  }

  suspend fun getCurrentUser(instance: GitLabInstance): Result<GitLabUser?> = call {
    if (instance.token.isBlank()) null else service(instance).getCurrentUser().requireBody()
  }

  suspend fun getUserStatus(instance: GitLabInstance): Result<GitLabUserStatus> = call {
    requireToken(instance)
    service(instance).getUserStatus().requireBody()
  }

  suspend fun setUserStatus(instance: GitLabInstance, message: String): Result<GitLabUserStatus> = call {
    requireToken(instance)
    val emoji = if (message.isBlank()) null else "speech_balloon"
    service(instance).setUserStatus(message = message, emoji = emoji).requireBody()
  }

  suspend fun getAchievements(instance: GitLabInstance, userId: Long): Result<List<GitLabUserAchievement>> = call {
    if (instance.token.isBlank() || userId <= 0) return@call emptyList()
    val response = service(instance).getUserAchievements(userId)
    if (response.code() == 404) return@call emptyList()
    response.requireList().filter { it.title.isNotBlank() }
  }

  suspend fun getProfileReadme(instance: GitLabInstance, username: String): Result<String?> = call {
    if (username.isBlank()) return@call null
    val response = service(instance).getProject(encodeGitLabPath("$username/$username"))
    if (response.code() == 404) return@call null
    val project = response.requireBody()
    readReadme(service(instance), project.id, project.defaultBranch)
  }

  suspend fun getProjects(instance: GitLabInstance, search: String? = null): Result<List<GitLabProject>> = call {
    val service = service(instance)
    val query = search?.trim()?.takeIf { it.isNotEmpty() }
    if (instance.token.isBlank()) {
      if (query == null) return@call emptyList()
      return@call fetchPages(2) { page ->
        service.getProjects(visibility = "public", search = query, page = page)
      }.map { it.withDisplayTimes() }
    }
    if (query != null) {
      return@call fetchPages(2) { page ->
        service.getProjects(membership = true, search = query, page = page)
      }.map { it.withDisplayTimes() }
    }
    val member = fetchPages(2) { page -> service.getProjects(membership = true, page = page) }
    val starred = try {
      fetchPages(1) { page -> service.getProjects(starred = true, page = page) }
    } catch (_: GitLabApiException) {
      emptyList()
    }
    mergeProjects(member, starred).map { it.withDisplayTimes() }
  }

  suspend fun getProject(instance: GitLabInstance, projectId: Long): Result<GitLabProject> = call {
    service(instance).getProject(projectId.toString()).requireBody().withDisplayTimes()
  }

  suspend fun createProject(
    instance: GitLabInstance,
    name: String,
    description: String?,
    visibility: String
  ): Result<GitLabProject> = call {
    requireToken(instance)
    service(instance).createProject(name, description?.takeIf { it.isNotBlank() }, visibility)
      .requireBody()
      .withDisplayTimes()
  }

  suspend fun setProjectStarred(
    instance: GitLabInstance,
    projectId: Long,
    starred: Boolean
  ): Result<GitLabProject> = call {
    requireToken(instance)
    val service = service(instance)
    val response = if (starred) service.starProject(projectId) else service.unstarProject(projectId)
    response.requireBody().copy(isStarred = starred).withDisplayTimes()
  }

  suspend fun getGroups(instance: GitLabInstance): Result<List<GitLabGroup>> = call {
    if (instance.token.isBlank()) return@call emptyList()
    fetchPages(2) { page -> service(instance).getGroups(membership = true, page = page) }
  }

  suspend fun getMyIssues(instance: GitLabInstance): Result<List<GitLabIssue>> = call {
    if (instance.token.isBlank()) return@call emptyList()
    val service = service(instance)
    val assigned = fetchPages(2) { page -> service.getIssues(scope = "assigned_to_me", page = page) }
    val created = try {
      fetchPages(1) { page -> service.getIssues(scope = "created_by_me", page = page) }
    } catch (error: GitLabApiException) {
      if (assigned.isEmpty()) throw error else emptyList()
    }
    (assigned + created).distinctBy { it.projectId to it.iid }.newestFirst { it.updatedAt }.map { it.withDisplayTimes() }
  }

  suspend fun getProjectIssues(instance: GitLabInstance, projectId: Long): Result<List<GitLabIssue>> = call {
    fetchPages(2) { page -> service(instance).getProjectIssues(projectId, page = page) }
      .map { it.withDisplayTimes() }
  }

  suspend fun createIssue(
    instance: GitLabInstance,
    projectId: Long,
    title: String,
    description: String?,
    labels: String?
  ): Result<GitLabIssue> = call {
    requireToken(instance)
    if (projectId <= 0) throw GitLabApiException(400, "Choose a project for the issue")
    service(instance).createIssue(projectId, title, description, labels).requireBody().withDisplayTimes()
  }

  suspend fun getIssueNotes(instance: GitLabInstance, projectId: Long, issueIid: Long): Result<List<GitLabNote>> = call {
    fetchPages(3) { page -> service(instance).getIssueNotes(projectId, issueIid, page = page) }
      .map { it.withDisplayTimes() }
  }

  suspend fun createIssueNote(
    instance: GitLabInstance,
    projectId: Long,
    issueIid: Long,
    body: String
  ): Result<GitLabNote> = call {
    requireToken(instance)
    service(instance).createIssueNote(projectId, issueIid, body).requireBody().withDisplayTimes()
  }

  suspend fun getMyMergeRequests(instance: GitLabInstance): Result<List<GitLabMergeRequest>> = call {
    if (instance.token.isBlank()) return@call emptyList()
    val service = service(instance)
    val assigned = fetchPages(2) { page -> service.getMergeRequests(scope = "assigned_to_me", page = page) }
    val created = try {
      fetchPages(1) { page -> service.getMergeRequests(scope = "created_by_me", page = page) }
    } catch (error: GitLabApiException) {
      if (assigned.isEmpty()) throw error else emptyList()
    }
    val reviews = if (instance.username.isBlank()) {
      emptyList()
    } else {
      try {
        fetchPages(1) { page ->
          service.getMergeRequests(reviewerUsername = instance.username, state = "opened", page = page)
        }
      } catch (_: GitLabApiException) {
        emptyList()
      }
    }
    (assigned + created + reviews).distinctBy { it.projectId to it.iid }.newestFirst { it.updatedAt }.map { it.withDisplayTimes() }
  }

  suspend fun getProjectMergeRequests(instance: GitLabInstance, projectId: Long): Result<List<GitLabMergeRequest>> = call {
    fetchPages(2) { page -> service(instance).getProjectMergeRequests(projectId, page = page) }
      .map { it.withDisplayTimes() }
  }

  suspend fun getMergeRequestDiffs(
    instance: GitLabInstance,
    projectId: Long,
    mrIid: Long
  ): Result<List<GitLabFileDiff>> = call {
    val service = service(instance)
    try {
      fetchPages(3) { page -> service.getMergeRequestDiffs(projectId, mrIid, page = page) }
    } catch (error: GitLabApiException) {
      if (error.code != 404) throw error
      service.getMergeRequestChanges(projectId, mrIid).requireBody().changes
    }
  }

  suspend fun getPipelines(instance: GitLabInstance, projectId: Long): Result<List<GitLabPipeline>> = call {
    fetchPages(1) { page -> service(instance).getProjectPipelines(projectId, page = page) }
      .map { it.withDisplayTimes() }
  }

  suspend fun attachJobs(
    instance: GitLabInstance,
    pipelines: List<GitLabPipeline>,
    limit: Int
  ): List<GitLabPipeline> = withContext(Dispatchers.IO) {
    if (pipelines.isEmpty() || limit <= 0) return@withContext pipelines
    val service = service(instance)
    coroutineScope {
      pipelines.mapIndexed { index, pipeline ->
        async {
          if (index >= limit || pipeline.projectId == 0L) {
            pipeline
          } else {
            val jobs = try {
              service.getPipelineJobs(pipeline.projectId, pipeline.id).requireList()
            } catch (_: Exception) {
              emptyList()
            }
            pipeline.copy(stages = stagesFromJobs(jobs))
          }
        }
      }.awaitAll()
    }
  }

  suspend fun retryPipeline(
    instance: GitLabInstance,
    projectId: Long,
    pipelineId: Long
  ): Result<GitLabPipeline> = call {
    requireToken(instance)
    val created = service(instance).retryPipeline(projectId, pipelineId).requireBody().withDisplayTimes()
    attachJobs(instance, listOf(created), limit = 1).first()
  }

  suspend fun getRepositoryTree(
    instance: GitLabInstance,
    projectId: Long,
    path: String?,
    ref: String?
  ): Result<List<GitLabTreeItem>> = call {
    val normalizedPath = path?.trim()?.trim('/')?.takeIf { it.isNotEmpty() }
    fetchPages(2) { page ->
      service(instance).getRepositoryTree(projectId, path = normalizedPath, ref = ref, page = page)
    }.sortedWith(compareBy<GitLabTreeItem> { if (it.type == "tree") 0 else 1 }.thenBy { it.name.lowercase() })
  }

  suspend fun getRepositoryCommits(
    instance: GitLabInstance,
    projectId: Long,
    ref: String?
  ): Result<List<GitLabCommit>> = call {
    fetchPages(1) { page -> service(instance).getRepositoryCommits(projectId, ref = ref, page = page) }
      .map { it.withDisplayTimes() }
  }

  suspend fun getFileContent(
    instance: GitLabInstance,
    projectId: Long,
    filePath: String,
    ref: String?
  ): Result<String> = call {
    val response = service(instance).getRawFileContent(projectId, encodeGitLabPath(filePath), ref)
    if (response.code() == 404) throw GitLabApiException(404, "File not found")
    if (!response.isSuccessful) throw response.toGitLabException()
    decodeBody(response.body())
  }

  suspend fun getReadme(instance: GitLabInstance, projectId: Long, ref: String?): Result<String?> = call {
    readReadme(service(instance), projectId, ref)
  }

  suspend fun getTodos(instance: GitLabInstance): Result<List<GitLabTodo>> = call {
    if (instance.token.isBlank()) return@call emptyList()
    val service = service(instance)
    val pending = fetchPages(2) { page -> service.getTodos(state = "pending", page = page) }
    val done = try {
      fetchPages(1) { page -> service.getTodos(state = "done", page = page) }
    } catch (_: GitLabApiException) {
      emptyList()
    }
    (pending + done).distinctBy { it.id }
  }

  suspend fun markTodoDone(instance: GitLabInstance, todoId: Long): Result<Unit> = call {
    requireToken(instance)
    val response = service(instance).markTodoAsDone(todoId)
    if (!response.isSuccessful) throw response.toGitLabException()
  }

  suspend fun markAllTodosDone(instance: GitLabInstance): Result<Unit> = call {
    requireToken(instance)
    val response = service(instance).markAllTodosAsDone()
    if (!response.isSuccessful) throw response.toGitLabException()
  }

  private suspend fun readReadme(
    service: com.aistudio.gitlab.data.api.GitLabApiService,
    projectId: Long,
    ref: String?
  ): String? {
    for (name in README_FILES) {
      val response = service.getRawFileContent(projectId, encodeGitLabPath(name), ref)
      if (response.code() == 404) continue
      if (!response.isSuccessful) throw response.toGitLabException()
      return decodeBody(response.body())
    }
    return null
  }

  private fun decodeBody(body: ResponseBody?): String {
    if (body == null) return ""
    return body.use { responseBody ->
      val buffer = Buffer()
      val read = responseBody.source().read(buffer, MAX_FILE_BYTES + 1)
      if (read <= 0L) return@use ""
      val truncated = read > MAX_FILE_BYTES
      val bytes = if (truncated) buffer.readByteArray(MAX_FILE_BYTES) else buffer.readByteArray()
      decodeBytes(bytes, truncated)
    }
  }

  private suspend fun <T> fetchPages(
    maxPages: Int,
    request: suspend (page: Int) -> Response<List<T>>
  ): List<T> {
    val all = mutableListOf<T>()
    var page = 1
    var fetched = 0
    while (fetched < maxPages) {
      val response = request(page)
      if (!response.isSuccessful) {
        if (all.isEmpty()) throw response.toGitLabException()
        break
      }
      val chunk = response.body().orEmpty()
      all += chunk
      fetched++
      val next = response.headers()["X-Next-Page"]?.toIntOrNull()
      if (next == null || next <= page || chunk.isEmpty()) break
      page = next
    }
    return all
  }

  private suspend fun <T> call(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
    try {
      Result.success(block())
    } catch (error: GitLabApiException) {
      Result.failure(error)
    } catch (_: UnknownHostException) {
      Result.failure(GitLabApiException(0, "Couldn't resolve the GitLab server"))
    } catch (_: SocketTimeoutException) {
      Result.failure(GitLabApiException(0, "The GitLab server took too long to respond"))
    } catch (_: SSLException) {
      Result.failure(GitLabApiException(0, "TLS connection to the GitLab server failed"))
    } catch (error: IOException) {
      Result.failure(GitLabApiException(0, error.message ?: "Network error"))
    } catch (error: Exception) {
      Result.failure(error)
    }
  }

  private fun service(instance: GitLabInstance) = GitLabApiClient.createService(instance)

  private fun requireToken(instance: GitLabInstance) {
    if (instance.token.isBlank()) {
      throw GitLabApiException(401, "A personal access token is required for this action.")
    }
  }

  private fun mergeProjects(member: List<GitLabProject>, starred: List<GitLabProject>): List<GitLabProject> {
    val starredIds = starred.map { it.id }.toSet()
    val merged = LinkedHashMap<Long, GitLabProject>()
    member.forEach { project ->
      merged[project.id] = project.copy(isStarred = project.isStarred || project.id in starredIds)
    }
    starred.forEach { project ->
      val existing = merged[project.id]
      merged[project.id] = (existing ?: project).copy(isStarred = true)
    }
    return merged.values.toList()
  }

  private fun <T> List<T>.newestFirst(time: (T) -> String): List<T> =
    sortedByDescending { parseGitLabMillis(time(it)) ?: 0L }

  private fun GitLabProject.withDisplayTimes() = copy(lastActivityAt = formatGitLabTime(lastActivityAt))

  private fun GitLabIssue.withDisplayTimes() = copy(
    createdAt = formatGitLabTime(createdAt),
    updatedAt = formatGitLabTime(updatedAt)
  )

  private fun GitLabMergeRequest.withDisplayTimes() = copy(
    createdAt = formatGitLabTime(createdAt),
    updatedAt = formatGitLabTime(updatedAt)
  )

  private fun GitLabPipeline.withDisplayTimes() = copy(
    createdAt = formatGitLabTime(createdAt),
    updatedAt = formatGitLabTime(updatedAt)
  )

  private fun GitLabCommit.withDisplayTimes() = copy(createdAt = formatGitLabTime(createdAt))

  private fun GitLabNote.withDisplayTimes() = copy(createdAt = formatGitLabTime(createdAt))

  private companion object {
    val README_FILES = listOf("README.md", "README.markdown", "README", "readme.md")
    const val MAX_FILE_BYTES = 400_000L
  }
}

private fun decodeBytes(bytes: ByteArray, truncated: Boolean): String {
  if (bytes.isEmpty()) return ""
  if (bytes.any { it == 0.toByte() }) {
    return "Binary file (${bytes.size} bytes)."
  }
  val text = String(bytes, Charsets.UTF_8)
  return if (truncated) text + "\n\n… truncated …" else text
}
