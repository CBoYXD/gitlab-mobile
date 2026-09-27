package com.cboyxd.gitlab.data.repository

import com.cboyxd.gitlab.data.api.GitLabApiClient
import com.cboyxd.gitlab.data.api.GitLabApiException
import com.cboyxd.gitlab.data.api.GitLabApiService
import com.cboyxd.gitlab.data.api.encodeGitLabPath
import com.cboyxd.gitlab.data.api.formatGitLabTime
import com.cboyxd.gitlab.data.api.parseGitLabMillis
import com.cboyxd.gitlab.data.api.requireBody
import com.cboyxd.gitlab.data.api.requireList
import com.cboyxd.gitlab.data.api.stagesFromJobs
import com.cboyxd.gitlab.data.api.toGitLabException
import com.cboyxd.gitlab.data.model.GitLabApprovals
import com.cboyxd.gitlab.data.model.GitLabBranch
import com.cboyxd.gitlab.data.model.GitLabCommit
import com.cboyxd.gitlab.data.model.GitLabContributor
import com.cboyxd.gitlab.data.model.GitLabEvent
import com.cboyxd.gitlab.data.model.GitLabJob
import com.cboyxd.gitlab.data.model.GitLabLabel
import com.cboyxd.gitlab.data.model.GitLabRelease
import com.cboyxd.gitlab.data.model.GitLabTag
import com.cboyxd.gitlab.data.model.GitLabFileDiff
import com.cboyxd.gitlab.data.model.GitLabGroup
import com.cboyxd.gitlab.data.model.GitLabInstance
import com.cboyxd.gitlab.data.model.GitLabIssue
import com.cboyxd.gitlab.data.model.GitLabMergeRequest
import com.cboyxd.gitlab.data.model.GitLabNote
import com.cboyxd.gitlab.data.model.GitLabPipeline
import com.cboyxd.gitlab.data.model.GitLabProject
import com.cboyxd.gitlab.data.model.ProjectCounts
import com.cboyxd.gitlab.data.model.GitLabTodo
import com.cboyxd.gitlab.data.model.GitLabTreeItem
import com.cboyxd.gitlab.data.model.GitLabUser
import com.cboyxd.gitlab.data.model.GitLabUserStatus
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import okio.Buffer
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

data class JobTrace(val text: String, val truncated: Boolean)

data class ConnectionProbe(
  val version: String,
  val username: String,
  val name: String
)

/**
 * Stateless access to one GitLab server per call. Every list is bounded by [maxPages]: the app
 * shows recent activity, it never walks a whole history (all commits, all pipelines, all done
 * to-dos), which is what used to make screens hang on large servers.
 */
class GitLabRepository {

  suspend fun testConnection(instance: GitLabInstance): Result<ConnectionProbe> = call {
    val service = service(instance)
    if (instance.token.isNotBlank()) {
      val user = service.getCurrentUser().requireBody()
      ConnectionProbe(version = versionOrBlank(service), username = user.username, name = user.name)
    } else {
      service.getProjects(visibility = "public", perPage = 1).requireList()
      ConnectionProbe(version = versionOrBlank(service), username = "", name = "")
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

  suspend fun getProfileReadme(instance: GitLabInstance, username: String): Result<String?> = call {
    if (username.isBlank()) return@call null
    val service = service(instance)
    val response = service.getProject(encodeGitLabPath("$username/$username"))
    if (response.code() == 404 || response.code() == 403) return@call null
    val project = response.requireBody()
    readReadme(service, project.id, project.defaultBranch)
  }

  suspend fun getProjects(instance: GitLabInstance, search: String? = null): Result<List<GitLabProject>> = call {
    val service = service(instance)
    val query = search?.trim()?.trim('/')?.takeIf { it.isNotEmpty() }
    val signedIn = instance.token.isNotBlank()
    if (query != null) {
      // A pasted path ("group/project") opens that project directly, even outside the search index.
      val byPath = if ('/' in query) {
        runCatchingApi { service.getProject(encodeGitLabPath(query)).takeIf { it.isSuccessful }?.body() }.getOrNull()
      } else null
      val found = fetchPages(maxPages = 1) { page ->
        service.getProjects(
          membership = if (signedIn) true else null,
          visibility = if (signedIn) null else "public",
          search = query,
          searchNamespaces = if ('/' in query) true else null,
          perPage = 50,
          page = page
        )
      }
      return@call (listOfNotNull(byPath) + found).distinctBy { it.id }.map { it.withDisplayTimes() }
    }
    if (!signedIn) {
      // Without an account there is no membership; show the most starred public projects instead.
      return@call fetchPages(maxPages = 1) { page ->
        service.getProjects(visibility = "public", orderBy = "star_count", perPage = 20, page = page)
      }.map { it.withDisplayTimes() }
    }
    coroutineScope {
      val member = async { fetchPages(maxPages = 2) { page -> service.getProjects(membership = true, perPage = 100, page = page) } }
      val starred = async {
        try {
          fetchPages(maxPages = 1) { page -> service.getProjects(starred = true, perPage = 100, page = page) }
        } catch (_: GitLabApiException) {
          emptyList()
        }
      }
      mergeProjects(member.await(), starred.await()).map { it.withDisplayTimes() }
    }
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
    // 304 means it already had that state; reload so the counts stay truthful.
    val project = if (response.code() == 304) service.getProject(projectId.toString()).requireBody() else response.requireBody()
    project.copy(isStarred = starred).withDisplayTimes()
  }

  suspend fun getGroups(instance: GitLabInstance): Result<List<GitLabGroup>> = call {
    if (instance.token.isBlank()) return@call emptyList()
    fetchPages(maxPages = 2) { page -> service(instance).getGroups(membership = true, perPage = 100, page = page) }
  }

  suspend fun getGroupProjects(instance: GitLabInstance, groupPath: String): Result<List<GitLabProject>> = call {
    fetchPages(maxPages = 2) { page -> service(instance).getGroupProjects(encodeGitLabPath(groupPath), perPage = 100, page = page) }
      .map { it.withDisplayTimes() }
  }

  suspend fun getMyIssues(instance: GitLabInstance): Result<List<GitLabIssue>> = call {
    if (instance.token.isBlank()) return@call emptyList()
    val service = service(instance)
    coroutineScope {
      val assigned = async { fetchPages(maxPages = 1) { page -> service.getIssues(scope = "assigned_to_me", perPage = 100, page = page) } }
      val created = async {
        runCatchingApi { fetchPages(maxPages = 1) { page -> service.getIssues(scope = "created_by_me", perPage = 100, page = page) } }
      }
      val assignedList = assigned.await()
      val createdList = created.await().getOrElse { error -> if (assignedList.isEmpty()) throw error else emptyList() }
      (assignedList + createdList).distinctBy { it.projectId to it.iid }.newestFirst { it.updatedAt }.map { it.withDisplayTimes() }
    }
  }

  suspend fun getIssue(instance: GitLabInstance, projectId: Long, iid: Long): Result<GitLabIssue> = call {
    service(instance).getIssue(projectId, iid).requireBody().withDisplayTimes()
  }

  suspend fun getProjectIssues(instance: GitLabInstance, projectId: Long): Result<List<GitLabIssue>> = call {
    fetchPages(maxPages = 1) { page -> service(instance).getProjectIssues(projectId, perPage = 50, page = page) }
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
    if (projectId <= 0) throw GitLabApiException(400, "Choose a project for the issue.")
    service(instance).createIssue(projectId, title, description, labels).requireBody().withDisplayTimes()
  }

  suspend fun setIssueState(instance: GitLabInstance, issue: GitLabIssue, close: Boolean): Result<GitLabIssue> = call {
    requireToken(instance)
    service(instance).setIssueState(issue.projectId, issue.iid, if (close) "close" else "reopen")
      .requireBody().withDisplayTimes()
  }

  suspend fun updateIssue(instance: GitLabInstance, issue: GitLabIssue, title: String, description: String?): Result<GitLabIssue> = call {
    requireToken(instance)
    service(instance).updateIssue(issue.projectId, issue.iid, title, description)
      .requireBody().withDisplayTimes()
  }

  suspend fun getIssueNotes(instance: GitLabInstance, projectId: Long, issueIid: Long): Result<List<GitLabNote>> = call {
    fetchPages(maxPages = 5) { page -> service(instance).getIssueNotes(projectId, issueIid, perPage = 100, page = page) }
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
    coroutineScope {
      val assigned = async { fetchPages(maxPages = 1) { page -> service.getMergeRequests(scope = "assigned_to_me", perPage = 100, page = page) } }
      val created = async {
        runCatchingApi { fetchPages(maxPages = 1) { page -> service.getMergeRequests(scope = "created_by_me", perPage = 100, page = page) } }
      }
      val reviews = async {
        if (instance.username.isBlank()) emptyList()
        else runCatchingApi {
          fetchPages(maxPages = 1) { page ->
            service.getMergeRequests(scope = "all", reviewerUsername = instance.username, state = "opened", perPage = 100, page = page)
          }
        }.getOrDefault(emptyList())
      }
      val assignedList = assigned.await()
      val createdList = created.await().getOrElse { error -> if (assignedList.isEmpty()) throw error else emptyList() }
      (assignedList + createdList + reviews.await())
        .distinctBy { it.projectId to it.iid }
        .newestFirst { it.updatedAt }
        .map { it.withDisplayTimes() }
    }
  }

  suspend fun getMergeRequest(instance: GitLabInstance, projectId: Long, iid: Long): Result<GitLabMergeRequest> = call {
    service(instance).getMergeRequest(projectId, iid).requireBody().withDisplayTimes()
  }

  suspend fun getProjectMergeRequests(instance: GitLabInstance, projectId: Long): Result<List<GitLabMergeRequest>> = call {
    fetchPages(maxPages = 1) { page -> service(instance).getProjectMergeRequests(projectId, perPage = 50, page = page) }
      .map { it.withDisplayTimes() }
  }

  /**
   * Exact open issue and merge request counts. The project's own open_issues_count is a cached
   * counter that GitLab refreshes lazily and leaves out for anonymous requests, so the page asks
   * issues_statistics and the X-Total of a one-item merge request page instead.
   */
  suspend fun getProjectCounts(instance: GitLabInstance, projectId: Long): Result<ProjectCounts> = call {
    val service = service(instance)
    coroutineScope {
      val issues = async {
        countOrNull { service.getIssueStatistics(projectId).takeIf { it.isSuccessful }?.body()?.statistics?.counts?.opened }
      }
      val mergeRequests = async {
        countOrNull { service.countProjectMergeRequests(projectId).takeIf { it.isSuccessful }?.headers()?.get("X-Total")?.toIntOrNull() }
      }
      ProjectCounts(openIssues = issues.await(), openMergeRequests = mergeRequests.await())
    }
  }

  suspend fun createMergeRequest(
    instance: GitLabInstance,
    projectId: Long,
    title: String,
    sourceBranch: String,
    targetBranch: String,
    description: String?
  ): Result<GitLabMergeRequest> = call {
    requireToken(instance)
    if (projectId <= 0) throw GitLabApiException(400, "Choose a project for the merge request.")
    service(instance).createMergeRequest(projectId, title, sourceBranch, targetBranch, description)
      .requireBody().withDisplayTimes()
  }

  suspend fun setMergeRequestState(instance: GitLabInstance, mr: GitLabMergeRequest, close: Boolean): Result<GitLabMergeRequest> = call {
    requireToken(instance)
    service(instance).setMergeRequestState(mr.projectId, mr.iid, if (close) "close" else "reopen")
      .requireBody().withDisplayTimes()
  }

  suspend fun mergeMergeRequest(instance: GitLabInstance, mr: GitLabMergeRequest): Result<GitLabMergeRequest> = call {
    requireToken(instance)
    val response = service(instance).mergeMergeRequest(mr.projectId, mr.iid)
    when (response.code()) {
      405 -> throw GitLabApiException(405, "This merge request can't be merged yet. Resolve its blocking items in GitLab, then try again.")
      406 -> throw GitLabApiException(406, "This merge request has conflicts. Resolve them, then merge.")
      409 -> throw GitLabApiException(409, "The source branch changed. Refresh the merge request and try again.")
    }
    response.requireBody().withDisplayTimes()
  }

  suspend fun getMergeRequestDiffs(
    instance: GitLabInstance,
    projectId: Long,
    mrIid: Long
  ): Result<List<GitLabFileDiff>> = call {
    val service = service(instance)
    try {
      fetchPages(maxPages = 4) { page -> service.getMergeRequestDiffs(projectId, mrIid, perPage = 30, page = page) }
    } catch (error: GitLabApiException) {
      // The /diffs endpoint arrived in GitLab 15.7; older servers only have /changes.
      if (error.code != 404) throw error
      service.getMergeRequestChanges(projectId, mrIid).requireBody().changes
    }
  }

  suspend fun getPipelines(instance: GitLabInstance, projectId: Long, perPage: Int = 20): Result<List<GitLabPipeline>> = call {
    fetchPages(maxPages = 1) { page -> service(instance).getProjectPipelines(projectId, perPage = perPage, page = page) }
      .map { it.withDisplayTimes() }
  }

  /**
   * Recent pipelines across the most recently active projects. Bounded to [projectLimit] projects,
   * a handful of pipelines each, fetched four at a time.
   */
  suspend fun getRecentPipelines(
    instance: GitLabInstance,
    projects: List<GitLabProject>,
    projectLimit: Int = 8,
    total: Int = 20
  ): Result<List<GitLabPipeline>> = call {
    if (projects.isEmpty()) return@call emptyList()
    val service = service(instance)
    val slots = Semaphore(4)
    val candidates = projects
      .sortedByDescending { parseGitLabMillis(it.lastActivityRaw) ?: 0L }
      .take(projectLimit)
    val listed = coroutineScope {
      candidates.map { project ->
        async {
          slots.withPermit {
            try {
              service.getProjectPipelines(project.id, perPage = 5).requireList()
            } catch (error: GitLabApiException) {
              // Projects without CI access answer 403; skip them rather than fail the list.
              emptyList()
            }
          }
        }
      }.awaitAll().flatten()
    }
    val recent = listed.sortedByDescending { parseGitLabMillis(it.createdAt) ?: it.id }.take(total)
    attachStages(service, recent.map { it.withDisplayTimes() }, limit = 6)
  }

  suspend fun attachJobs(
    instance: GitLabInstance,
    pipelines: List<GitLabPipeline>,
    limit: Int
  ): List<GitLabPipeline> = withContext(Dispatchers.IO) {
    attachStages(service(instance), pipelines, limit)
  }

  private suspend fun attachStages(service: GitLabApiService, pipelines: List<GitLabPipeline>, limit: Int): List<GitLabPipeline> {
    if (pipelines.isEmpty() || limit <= 0) return pipelines
    val slots = Semaphore(4)
    return coroutineScope {
      pipelines.mapIndexed { index, pipeline ->
        async {
          if (index >= limit || pipeline.projectId == 0L) {
            pipeline
          } else {
            val jobs = slots.withPermit {
              try {
                service.getPipelineJobs(pipeline.projectId, pipeline.id).requireList()
              } catch (cancel: CancellationException) {
                throw cancel
              } catch (_: Exception) {
                emptyList()
              }
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
    val service = service(instance)
    val retried = service.retryPipeline(projectId, pipelineId).requireBody().withDisplayTimes()
    attachStages(service, listOf(retried), limit = 1).first()
  }

  suspend fun getRepositoryTree(
    instance: GitLabInstance,
    projectId: Long,
    path: String?,
    ref: String?
  ): Result<List<GitLabTreeItem>> = call {
    val normalizedPath = path?.trim()?.trim('/')?.takeIf { it.isNotEmpty() }
    fetchPages(maxPages = 5) { page ->
      service(instance).getRepositoryTree(projectId, path = normalizedPath, ref = ref, perPage = 100, page = page)
    }.sortedWith(compareBy<GitLabTreeItem> { if (it.type == "tree") 0 else 1 }.thenBy { it.name.lowercase() })
  }

  suspend fun getRepositoryCommits(
    instance: GitLabInstance,
    projectId: Long,
    ref: String?
  ): Result<List<GitLabCommit>> = call {
    fetchPages(maxPages = 1) { page -> service(instance).getRepositoryCommits(projectId, ref = ref, perPage = 40, page = page) }
      .map { it.withDisplayTimes() }
  }

  suspend fun getFileContent(
    instance: GitLabInstance,
    projectId: Long,
    filePath: String,
    ref: String?
  ): Result<String> = call {
    val response = service(instance).getRawFileContent(projectId, encodeGitLabPath(filePath), ref ?: "HEAD")
    if (response.code() == 404) throw GitLabApiException(404, "This file isn't on the default branch anymore. Go back and refresh the file list.")
    if (!response.isSuccessful) throw response.toGitLabException()
    decodeBody(response.body())
  }

  suspend fun getReadme(instance: GitLabInstance, projectId: Long, ref: String?): Result<String?> = call {
    readReadme(service(instance), projectId, ref)
  }

  suspend fun getTodos(instance: GitLabInstance): Result<List<GitLabTodo>> = call {
    if (instance.token.isBlank()) return@call emptyList()
    val service = service(instance)
    coroutineScope {
      val pending = async { fetchPages(maxPages = 2) { page -> service.getTodos(state = "pending", perPage = 100, page = page) } }
      val done = async {
        runCatchingApi { fetchPages(maxPages = 1) { page -> service.getTodos(state = "done", perPage = 30, page = page) } }
          .getOrDefault(emptyList())
      }
      (pending.await() + done.await()).distinctBy { it.id }
    }
  }

  suspend fun markTodoDone(instance: GitLabInstance, todoId: Long): Result<Unit> = call {
    requireToken(instance)
    val response = service(instance).markTodoAsDone(todoId)
    response.body()?.close()
    if (!response.isSuccessful) throw response.toGitLabException()
  }

  suspend fun markAllTodosDone(instance: GitLabInstance): Result<Unit> = call {
    requireToken(instance)
    val response = service(instance).markAllTodosAsDone()
    response.body()?.close()
    if (!response.isSuccessful) throw response.toGitLabException()
  }

  // ---------------------------------------------------------------------------------------------
  // Project metadata
  // ---------------------------------------------------------------------------------------------

  suspend fun getProjectWithLicense(instance: GitLabInstance, projectId: Long): Result<GitLabProject> = call {
    service(instance).getProject(projectId.toString(), license = true).requireBody().withDisplayTimes()
  }

  suspend fun getProjectByPath(instance: GitLabInstance, path: String): Result<GitLabProject> = call {
    service(instance).getProject(encodeGitLabPath(path)).requireBody().withDisplayTimes()
  }

  /** Language shares in percent, largest first. */
  suspend fun getLanguages(instance: GitLabInstance, projectId: Long): Result<List<Pair<String, Double>>> = call {
    service(instance).getLanguages(projectId).requireBody().entries.map { it.key to it.value }.sortedByDescending { it.second }
  }

  suspend fun getBranches(instance: GitLabInstance, projectId: Long, search: String? = null): Result<List<GitLabBranch>> = call {
    // Most recently updated first, as GitHub shows them; the default branch is pinned below.
    fetchPages(maxPages = 1) { page -> service(instance).getBranches(projectId, search?.takeIf { it.isNotBlank() }, page = page) }
      .sortedWith(compareByDescending<GitLabBranch> { it.default }.thenByDescending { parseGitLabMillis(it.commit?.committedDate ?: "") ?: 0L })
      .map { it.copy(commit = it.commit?.withDisplayTimes()) }
  }

  suspend fun getTags(instance: GitLabInstance, projectId: Long, search: String? = null): Result<List<GitLabTag>> = call {
    fetchPages(maxPages = 1) { page -> service(instance).getTags(projectId, search = search?.takeIf { it.isNotBlank() }, page = page) }
      .map { it.copy(commit = it.commit?.withDisplayTimes()) }
  }

  suspend fun getReleases(instance: GitLabInstance, projectId: Long): Result<List<GitLabRelease>> = call {
    try {
      fetchPages(maxPages = 1) { page -> service(instance).getReleases(projectId, page = page) }
        .map { it.copy(releasedAt = it.releasedAt?.let(::formatGitLabTime), createdAt = it.createdAt?.let(::formatGitLabTime), commit = it.commit?.withDisplayTimes()) }
    } catch (error: GitLabApiException) {
      // Projects that turned the Releases feature off answer 403: that reads as "no releases".
      if (error.code == 403) emptyList() else throw error
    }
  }

  suspend fun getContributors(instance: GitLabInstance, projectId: Long): Result<List<GitLabContributor>> = call {
    val response = service(instance).getContributors(projectId)
    // Contributor stats need repository access; public projects sometimes answer 403.
    if (response.code() == 403) return@call emptyList()
    response.requireList()
  }

  suspend fun getLabels(instance: GitLabInstance, projectId: Long): Result<List<GitLabLabel>> = call {
    fetchPages(maxPages = 3) { page -> service(instance).getLabels(projectId, page = page) }
  }

  suspend fun getProjectEvents(instance: GitLabInstance, projectId: Long): Result<List<GitLabEvent>> = call {
    service(instance).getProjectEvents(projectId).requireList().map { it.copy(createdAt = formatGitLabTime(it.createdAt)) }
  }

  suspend fun getCommits(instance: GitLabInstance, projectId: Long, ref: String?, path: String? = null): Result<List<GitLabCommit>> = call {
    fetchPages(maxPages = 1) { page -> service(instance).getRepositoryCommits(projectId, ref = ref, path = path, perPage = 40, page = page) }
      .map { it.withDisplayTimes() }
  }

  suspend fun getCommit(instance: GitLabInstance, projectId: Long, sha: String): Result<GitLabCommit> = call {
    service(instance).getCommit(projectId, sha).requireBody().withDisplayTimes()
  }

  suspend fun getCommitDiff(instance: GitLabInstance, projectId: Long, sha: String): Result<List<GitLabFileDiff>> = call {
    fetchPages(maxPages = 4) { page -> service(instance).getCommitDiff(projectId, sha, page = page) }
  }

  // ---------------------------------------------------------------------------------------------
  // Merge request details
  // ---------------------------------------------------------------------------------------------

  suspend fun getMergeRequestNotes(instance: GitLabInstance, projectId: Long, iid: Long): Result<List<GitLabNote>> = call {
    fetchPages(maxPages = 5) { page -> service(instance).getMergeRequestNotes(projectId, iid, page = page) }
      .map { it.withDisplayTimes() }
  }

  suspend fun createMergeRequestNote(instance: GitLabInstance, projectId: Long, iid: Long, body: String): Result<GitLabNote> = call {
    requireToken(instance)
    service(instance).createMergeRequestNote(projectId, iid, body).requireBody().withDisplayTimes()
  }

  suspend fun getMergeRequestCommits(instance: GitLabInstance, projectId: Long, iid: Long): Result<List<GitLabCommit>> = call {
    fetchPages(maxPages = 2) { page -> service(instance).getMergeRequestCommits(projectId, iid, page = page) }
      .map { it.withDisplayTimes() }
  }

  suspend fun getMergeRequestPipelines(instance: GitLabInstance, projectId: Long, iid: Long): Result<List<GitLabPipeline>> = call {
    val response = service(instance).getMergeRequestPipelines(projectId, iid)
    if (response.code() == 403) return@call emptyList()
    response.requireList().map { it.withDisplayTimes() }
  }

  /** Approval state, or null when the server has no approvals API for this request (CE, anonymous). */
  suspend fun getMergeRequestApprovals(instance: GitLabInstance, projectId: Long, iid: Long): Result<GitLabApprovals?> = call {
    val response = service(instance).getMergeRequestApprovals(projectId, iid)
    if (response.code() == 401 || response.code() == 403 || response.code() == 404) return@call null
    response.requireBody()
  }

  // ---------------------------------------------------------------------------------------------
  // Pipelines and jobs
  // ---------------------------------------------------------------------------------------------

  suspend fun getPipeline(instance: GitLabInstance, projectId: Long, pipelineId: Long): Result<GitLabPipeline> = call {
    service(instance).getPipeline(projectId, pipelineId).requireBody().withDisplayTimes()
  }

  suspend fun getPipelineJobs(instance: GitLabInstance, projectId: Long, pipelineId: Long): Result<List<GitLabJob>> = call {
    // Large pipelines have hundreds of jobs; the API pages them newest first.
    fetchPages(maxPages = 5) { page -> service(instance).getPipelineJobs(projectId, pipelineId, page = page) }.sortedBy { it.id }
  }

  suspend fun cancelPipeline(instance: GitLabInstance, projectId: Long, pipelineId: Long): Result<GitLabPipeline> = call {
    requireToken(instance)
    service(instance).cancelPipeline(projectId, pipelineId).requireBody().withDisplayTimes()
  }

  suspend fun getJob(instance: GitLabInstance, projectId: Long, jobId: Long): Result<GitLabJob> = call {
    service(instance).getJob(projectId, jobId).requireBody()
  }

  suspend fun retryJob(instance: GitLabInstance, projectId: Long, jobId: Long): Result<GitLabJob> = call {
    requireToken(instance)
    service(instance).retryJob(projectId, jobId).requireBody()
  }

  /** The end of a job log. Logs can be many megabytes; only the last [MAX_TRACE_BYTES] are kept. */
  suspend fun getJobTrace(instance: GitLabInstance, projectId: Long, jobId: Long): Result<JobTrace> = call {
    val response = service(instance).getJobTrace(projectId, jobId)
    if (!response.isSuccessful) throw response.toGitLabException()
    val body = response.body() ?: return@call JobTrace("", truncated = false)
    body.use {
      val source = it.source()
      val buffer = Buffer()
      var dropped = false
      while (source.read(buffer, 64 * 1024L) != -1L) {
        if (buffer.size > MAX_TRACE_BYTES * 2) {
          buffer.skip(buffer.size - MAX_TRACE_BYTES)
          dropped = true
        }
      }
      if (buffer.size > MAX_TRACE_BYTES) {
        buffer.skip(buffer.size - MAX_TRACE_BYTES)
        dropped = true
      }
      JobTrace(buffer.readUtf8(), truncated = dropped)
    }
  }

  // ---------------------------------------------------------------------------------------------
  // People, groups, search, activity
  // ---------------------------------------------------------------------------------------------

  suspend fun getUser(instance: GitLabInstance, userId: Long): Result<GitLabUser> = call {
    service(instance).getUser(userId).requireBody()
  }

  suspend fun findUser(instance: GitLabInstance, username: String): Result<GitLabUser?> = call {
    service(instance).getUsersByName(username = username).requireList().firstOrNull()
  }

  suspend fun searchUsers(instance: GitLabInstance, query: String): Result<List<GitLabUser>> = call {
    service(instance).getUsersByName(search = query).requireList()
  }

  suspend fun getUserProjects(instance: GitLabInstance, userId: Long): Result<List<GitLabProject>> = call {
    service(instance).getUserProjects(userId).requireList().map { it.withDisplayTimes() }
  }

  suspend fun getUserEvents(instance: GitLabInstance, userId: Long): Result<List<GitLabEvent>> = call {
    val response = service(instance).getUserEvents(userId)
    // Private profiles hide their activity.
    if (response.code() == 403 || response.code() == 404) return@call emptyList()
    response.requireList().map { it.copy(createdAt = formatGitLabTime(it.createdAt)) }
  }

  suspend fun getMyEvents(instance: GitLabInstance): Result<List<GitLabEvent>> = call {
    if (instance.token.isBlank()) return@call emptyList()
    service(instance).getMyEvents().requireList().map { it.copy(createdAt = formatGitLabTime(it.createdAt)) }
  }

  suspend fun getGroup(instance: GitLabInstance, groupPath: String): Result<GitLabGroup> = call {
    service(instance).getGroup(encodeGitLabPath(groupPath)).requireBody()
  }

  suspend fun getSubgroups(instance: GitLabInstance, groupPath: String): Result<List<GitLabGroup>> = call {
    fetchPages(maxPages = 1) { page -> service(instance).getSubgroups(encodeGitLabPath(groupPath), page = page) }
  }

  suspend fun searchIssues(instance: GitLabInstance, query: String): Result<List<GitLabIssue>> = call {
    requireToken(instance)
    service(instance).searchIssues(query).requireList().map { it.withDisplayTimes() }
  }

  suspend fun searchMergeRequests(instance: GitLabInstance, query: String): Result<List<GitLabMergeRequest>> = call {
    requireToken(instance)
    service(instance).searchMergeRequests(query).requireList().map { it.withDisplayTimes() }
  }

  private suspend fun versionOrBlank(service: GitLabApiService): String = try {
    service.getVersion().let { response -> if (response.isSuccessful) response.body()?.version.orEmpty() else "" }
  } catch (cancel: CancellationException) {
    throw cancel
  } catch (_: Exception) {
    ""
  }

  private suspend fun readReadme(
    service: GitLabApiService,
    projectId: Long,
    ref: String?
  ): String? {
    for (name in README_FILES) {
      val response = service.getRawFileContent(projectId, encodeGitLabPath(name), ref ?: "HEAD")
      if (response.code() == 404) {
        response.errorBody()?.close()
        continue
      }
      if (!response.isSuccessful) throw response.toGitLabException()
      return decodeBody(response.body())
    }
    return null
  }

  private fun decodeBody(body: ResponseBody?): String {
    if (body == null) return ""
    return body.use { responseBody ->
      val source = responseBody.source()
      val buffer = Buffer()
      // read() returns at most one segment; keep reading until the cap or end of stream.
      while (buffer.size <= MAX_FILE_BYTES) {
        if (source.read(buffer, MAX_FILE_BYTES + 1 - buffer.size) == -1L) break
      }
      if (buffer.size == 0L) return@use ""
      val truncated = buffer.size > MAX_FILE_BYTES
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
    repeat(maxPages) {
      val response = request(page)
      if (!response.isSuccessful) throw response.toGitLabException()
      val chunk = response.body().orEmpty()
      all += chunk
      val next = response.headers()["X-Next-Page"]?.toIntOrNull()
      if (next == null || next <= page || chunk.isEmpty()) return all
      page = next
    }
    return all
  }

  /** Converts expected failures into Result; cancellation always propagates. */
  private suspend fun <T> call(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
    try {
      Result.success(block())
    } catch (cancel: CancellationException) {
      throw cancel
    } catch (error: Throwable) {
      Result.failure(error.toApiException())
    }
  }

  /** One half of [getProjectCounts]: any failure (network, parsing, HTTP) only blanks that count. */
  private suspend fun countOrNull(block: suspend () -> Int?): Int? = try {
    block()
  } catch (cancel: CancellationException) {
    throw cancel
  } catch (_: Exception) {
    null
  }

  private suspend fun <T> runCatchingApi(block: suspend () -> T): Result<T> = try {
    Result.success(block())
  } catch (cancel: CancellationException) {
    throw cancel
  } catch (error: GitLabApiException) {
    Result.failure(error)
  }

  private fun service(instance: GitLabInstance) = GitLabApiClient.createService(instance)

  private fun requireToken(instance: GitLabInstance) {
    if (instance.token.isBlank()) {
      throw GitLabApiException(401, "Sign in to this server to make changes.")
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

  private fun GitLabProject.withDisplayTimes() = copy(lastActivityAt = formatGitLabTime(lastActivityAt), lastActivityRaw = lastActivityAt)

  private fun GitLabIssue.withDisplayTimes() = copy(
    createdAt = formatGitLabTime(createdAt),
    updatedAt = formatGitLabTime(updatedAt),
    closedAt = closedAt?.let(::formatGitLabTime)
  )

  private fun GitLabMergeRequest.withDisplayTimes() = copy(
    createdAt = formatGitLabTime(createdAt),
    updatedAt = formatGitLabTime(updatedAt),
    mergedAt = mergedAt?.let(::formatGitLabTime),
    closedAt = closedAt?.let(::formatGitLabTime),
    headPipeline = headPipeline?.withDisplayTimes()
  )

  private fun GitLabPipeline.withDisplayTimes() = copy(
    createdAt = formatGitLabTime(createdAt),
    updatedAt = formatGitLabTime(updatedAt)
  )

  private fun GitLabCommit.withDisplayTimes() = copy(
    createdAt = formatGitLabTime(createdAt),
    committedDate = committedDate?.let(::formatGitLabTime)
  )

  private fun GitLabNote.withDisplayTimes() = copy(createdAt = formatGitLabTime(createdAt))

  private companion object {
    val README_FILES = listOf("README.md", "README.markdown", "README.rst", "README.txt", "README", "readme.md")
    const val MAX_FILE_BYTES = 400_000L
    const val MAX_TRACE_BYTES = 256_000L
  }
}

internal fun Throwable.toApiException(): Throwable = when (this) {
  is GitLabApiException -> this
  // JsonEncodingException is an IOException, so it must be matched before the network cases.
  is JsonDataException, is JsonEncodingException ->
    GitLabApiException(-1, "The server sent data this app can't read. Check that the URL points to a GitLab server.")
  is UnknownHostException -> GitLabApiException(0, "Unable to find the server. Check the address and your connection.")
  is SocketTimeoutException -> GitLabApiException(0, "The server took too long to respond. Try again.")
  is SSLException -> GitLabApiException(0, "Unable to open a secure connection. Check the server's HTTPS certificate.")
  is IOException -> GitLabApiException(0, "Unable to reach the server. Check your connection and try again.")
  is IllegalArgumentException -> GitLabApiException(-1, message ?: "Check the server address.")
  else -> GitLabApiException(-1, message ?: "Something went wrong. Try again.")
}

private fun decodeBytes(bytes: ByteArray, truncated: Boolean): String {
  if (bytes.isEmpty()) return ""
  val sniff = bytes.size.coerceAtMost(8_000)
  if ((0 until sniff).any { bytes[it] == 0.toByte() }) {
    return "Binary file (${bytes.size} bytes). Open it on the web to view it."
  }
  val text = String(bytes, Charsets.UTF_8)
  return if (truncated) text + "\n\n… The file is longer than 400 KB. Open it on the web to see the rest." else text
}
