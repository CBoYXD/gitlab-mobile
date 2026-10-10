package com.cboyxd.gitlab.data.api

import com.cboyxd.gitlab.data.model.GitLabAvatar
import com.cboyxd.gitlab.data.model.GitLabApprovals
import com.cboyxd.gitlab.data.model.GitLabBranch
import com.cboyxd.gitlab.data.model.GitLabCommit
import com.cboyxd.gitlab.data.model.GitLabContributor
import com.cboyxd.gitlab.data.model.GitLabEvent
import com.cboyxd.gitlab.data.model.GitLabLabel
import com.cboyxd.gitlab.data.model.GitLabRelease
import com.cboyxd.gitlab.data.model.GitLabTag
import com.cboyxd.gitlab.data.model.GitLabFileDiff
import com.cboyxd.gitlab.data.model.GitLabGroup
import com.cboyxd.gitlab.data.model.GitLabIssueStatistics
import com.cboyxd.gitlab.data.model.GitLabIssue
import com.cboyxd.gitlab.data.model.GitLabJob
import com.cboyxd.gitlab.data.model.GitLabMergeRequest
import com.cboyxd.gitlab.data.model.GitLabNote
import com.cboyxd.gitlab.data.model.GitLabPipeline
import com.cboyxd.gitlab.data.model.GitLabProject
import com.cboyxd.gitlab.data.model.GitLabTodo
import com.cboyxd.gitlab.data.model.GitLabTreeItem
import com.cboyxd.gitlab.data.model.GitLabUser
import com.cboyxd.gitlab.data.model.GitLabUserStatus
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class GitLabVersionResponse(
  val version: String = "",
  val revision: String? = null
)

@JsonClass(generateAdapter = true)
data class MergeRequestChangesResponse(
  val changes: List<GitLabFileDiff> = emptyList()
)

interface GitLabApiService {

  @GET("api/v4/projects/{id}/merge_requests/{mr_iid}/discussions")
  suspend fun getMergeRequestDiscussions(
    @Path("id") projectId: Long,
    @Path("mr_iid") mrIid: Long,
    @Query("per_page") perPage: Int = 100,
    @Query("page") page: Int = 1,
  ): Response<List<com.cboyxd.gitlab.data.model.GitLabDiscussion>>

  @GET("api/v4/projects/{id}/repository/compare")
  suspend fun compareDiscussionRevision(
    @Path("id") projectId: Long,
    @Query("from") baseSha: String,
    @Query("to") headSha: String,
    @Query("straight") straight: Boolean = true,
  ): Response<com.cboyxd.gitlab.data.model.GitLabComparison>


  @GET("api/v4/user")
  suspend fun getCurrentUser(): Response<GitLabUser>

  @GET("api/v4/avatar")
  suspend fun getAvatar(@Query("email") email: String, @Query("size") size: Int = 64): Response<GitLabAvatar>

  @GET("api/v4/user/status")
  suspend fun getUserStatus(): Response<GitLabUserStatus>

  @PUT("api/v4/user/status")
  suspend fun setUserStatus(
    @Query("message") message: String,
    @Query("emoji") emoji: String?
  ): Response<GitLabUserStatus>

  @GET("api/v4/version")
  suspend fun getVersion(): Response<GitLabVersionResponse>

  @GET("api/v4/projects")
  suspend fun getProjects(
    @Query("membership") membership: Boolean? = null,
    @Query("starred") starred: Boolean? = null,
    @Query("visibility") visibility: String? = null,
    @Query("search") search: String? = null,
    @Query("search_namespaces") searchNamespaces: Boolean? = null,
    @Query("order_by") orderBy: String = "last_activity_at",
    @Query("sort") sort: String = "desc",
    @Query("per_page") perPage: Int = 50,
    @Query("page") page: Int = 1
  ): Response<List<GitLabProject>>

  @GET("api/v4/projects/{id}")
  suspend fun getProject(
    @Path(value = "id", encoded = true) projectId: String,
    @Query("license") license: Boolean? = null
  ): Response<GitLabProject>

  @FormUrlEncoded
  @POST("api/v4/projects")
  suspend fun createProject(
    @Field("name") name: String,
    @Field("description") description: String?,
    @Field("visibility") visibility: String
  ): Response<GitLabProject>

  @POST("api/v4/projects/{id}/star")
  suspend fun starProject(@Path("id") projectId: Long): Response<GitLabProject>

  @POST("api/v4/projects/{id}/unstar")
  suspend fun unstarProject(@Path("id") projectId: Long): Response<GitLabProject>

  @GET("api/v4/groups")
  suspend fun getGroups(
    @Query("membership") membership: Boolean? = null,
    @Query("order_by") orderBy: String = "name",
    @Query("sort") sort: String = "asc",
    @Query("per_page") perPage: Int = 50,
    @Query("page") page: Int = 1
  ): Response<List<GitLabGroup>>

  @GET("api/v4/groups/{id}/projects")
  suspend fun getGroupProjects(
    @Path(value = "id", encoded = true) groupId: String,
    @Query("include_subgroups") includeSubgroups: Boolean = true,
    @Query("with_shared") withShared: Boolean = true,
    @Query("per_page") perPage: Int = 50,
    @Query("page") page: Int = 1
  ): Response<List<GitLabProject>>

  @GET("api/v4/issues")
  suspend fun getIssues(
    @Query("scope") scope: String,
    @Query("state") state: String = "all",
    @Query("order_by") orderBy: String = "updated_at",
    @Query("sort") sort: String = "desc",
    @Query("per_page") perPage: Int = 50,
    @Query("with_labels_details") withLabelDetails: Boolean = true,
    @Query("page") page: Int = 1
  ): Response<List<GitLabIssue>>

  @GET("api/v4/projects/{id}/issues")
  suspend fun getProjectIssues(
    @Path("id") projectId: Long,
    @Query("state") state: String = "all",
    @Query("order_by") orderBy: String = "updated_at",
    @Query("sort") sort: String = "desc",
    @Query("per_page") perPage: Int = 50,
    @Query("with_labels_details") withLabelDetails: Boolean = true,
    @Query("page") page: Int = 1
  ): Response<List<GitLabIssue>>

  @GET("api/v4/projects/{id}/issues/{issue_iid}")
  suspend fun getIssue(
    @Path("id") projectId: Long,
    @Path("issue_iid") issueIid: Long,
    @Query("with_labels_details") withLabelDetails: Boolean = true
  ): Response<GitLabIssue>

  @GET("api/v4/projects/{id}/merge_requests/{mr_iid}")
  suspend fun getMergeRequest(
    @Path("id") projectId: Long,
    @Path("mr_iid") mrIid: Long,
    @Query("with_labels_details") withLabelDetails: Boolean = true
  ): Response<GitLabMergeRequest>

  @FormUrlEncoded
  @POST("api/v4/projects/{id}/issues")
  suspend fun createIssue(
    @Path("id") projectId: Long,
    @Field("title") title: String,
    @Field("description") description: String?,
    @Field("labels") labels: String?
  ): Response<GitLabIssue>

  @FormUrlEncoded
  @PUT("api/v4/projects/{id}/issues/{issue_iid}")
  suspend fun setIssueState(
    @Path("id") projectId: Long,
    @Path("issue_iid") issueIid: Long,
    @Field("state_event") stateEvent: String
  ): Response<GitLabIssue>

  @FormUrlEncoded
  @PUT("api/v4/projects/{id}/issues/{issue_iid}")
  suspend fun updateIssue(
    @Path("id") projectId: Long,
    @Path("issue_iid") issueIid: Long,
    @Field("title") title: String,
    @Field("description") description: String?
  ): Response<GitLabIssue>

  @GET("api/v4/projects/{id}/issues/{issue_iid}/notes")
  suspend fun getIssueNotes(
    @Path("id") projectId: Long,
    @Path("issue_iid") issueIid: Long,
    @Query("sort") sort: String = "asc",
    @Query("per_page") perPage: Int = 50,
    @Query("page") page: Int = 1
  ): Response<List<GitLabNote>>

  @FormUrlEncoded
  @POST("api/v4/projects/{id}/issues/{issue_iid}/notes")
  suspend fun createIssueNote(
    @Path("id") projectId: Long,
    @Path("issue_iid") issueIid: Long,
    @Field("body") body: String
  ): Response<GitLabNote>

  @GET("api/v4/merge_requests")
  suspend fun getMergeRequests(
    @Query("scope") scope: String? = null,
    @Query("state") state: String = "all",
    @Query("reviewer_username") reviewerUsername: String? = null,
    @Query("order_by") orderBy: String = "updated_at",
    @Query("sort") sort: String = "desc",
    @Query("per_page") perPage: Int = 50,
    @Query("with_labels_details") withLabelDetails: Boolean = true,
    @Query("page") page: Int = 1,
    @Query("updated_after") updatedAfter: String? = null
  ): Response<List<GitLabMergeRequest>>

  @GET("api/v4/projects/{id}/merge_requests")
  suspend fun getProjectMergeRequests(
    @Path("id") projectId: Long,
    @Query("state") state: String = "all",
    @Query("order_by") orderBy: String = "updated_at",
    @Query("sort") sort: String = "desc",
    @Query("per_page") perPage: Int = 50,
    @Query("with_labels_details") withLabelDetails: Boolean = true,
    @Query("page") page: Int = 1
  ): Response<List<GitLabMergeRequest>>

  /** Open merge requests, one per page: the count comes from the X-Total header. */
  @GET("api/v4/projects/{id}/merge_requests")
  suspend fun countProjectMergeRequests(
    @Path("id") projectId: Long,
    @Query("state") state: String = "opened",
    @Query("per_page") perPage: Int = 1,
  ): Response<List<GitLabMergeRequest>>

  @GET("api/v4/projects/{id}/issues_statistics")
  suspend fun getIssueStatistics(@Path("id") projectId: Long): Response<GitLabIssueStatistics>

  @FormUrlEncoded
  @POST("api/v4/projects/{id}/merge_requests")
  suspend fun createMergeRequest(
    @Path("id") projectId: Long,
    @Field("title") title: String,
    @Field("source_branch") sourceBranch: String,
    @Field("target_branch") targetBranch: String,
    @Field("description") description: String?
  ): Response<GitLabMergeRequest>

  @FormUrlEncoded
  @PUT("api/v4/projects/{id}/merge_requests/{mr_iid}")
  suspend fun setMergeRequestState(
    @Path("id") projectId: Long,
    @Path("mr_iid") mrIid: Long,
    @Field("state_event") stateEvent: String
  ): Response<GitLabMergeRequest>

  @FormUrlEncoded
  @PUT("api/v4/projects/{id}/merge_requests/{mr_iid}/merge")
  suspend fun mergeMergeRequest(
    @Path("id") projectId: Long,
    @Path("mr_iid") mrIid: Long,
    @Field("should_remove_source_branch") removeSourceBranch: Boolean = false
  ): Response<GitLabMergeRequest>

  @GET("api/v4/projects/{id}/merge_requests/{mr_iid}/diffs")
  suspend fun getMergeRequestDiffs(
    @Path("id") projectId: Long,
    @Path("mr_iid") mrIid: Long,
    @Query("per_page") perPage: Int = 20,
    @Query("page") page: Int = 1
  ): Response<List<GitLabFileDiff>>

  @GET("api/v4/projects/{id}/merge_requests/{mr_iid}/changes")
  suspend fun getMergeRequestChanges(
    @Path("id") projectId: Long,
    @Path("mr_iid") mrIid: Long
  ): Response<MergeRequestChangesResponse>

  @GET("api/v4/projects/{id}/pipelines")
  suspend fun getProjectPipelines(
    @Path("id") projectId: Long,
    @Query("order_by") orderBy: String = "id",
    @Query("sort") sort: String = "desc",
    @Query("per_page") perPage: Int = 20,
    @Query("page") page: Int = 1
  ): Response<List<GitLabPipeline>>

  @GET("api/v4/projects/{id}/pipelines/{pipeline_id}/jobs")
  suspend fun getPipelineJobs(
    @Path("id") projectId: Long,
    @Path("pipeline_id") pipelineId: Long,
    @Query("include_retried") includeRetried: Boolean = false,
    @Query("per_page") perPage: Int = 100,
    @Query("page") page: Int = 1
  ): Response<List<GitLabJob>>

  @POST("api/v4/projects/{id}/pipelines/{pipeline_id}/retry")
  suspend fun retryPipeline(
    @Path("id") projectId: Long,
    @Path("pipeline_id") pipelineId: Long
  ): Response<GitLabPipeline>

  @GET("api/v4/projects/{id}/repository/tree")
  suspend fun getRepositoryTree(
    @Path("id") projectId: Long,
    @Query("path") path: String? = null,
    @Query("ref") ref: String? = null,
    @Query("per_page") perPage: Int = 100,
    @Query("page") page: Int = 1
  ): Response<List<GitLabTreeItem>>

  @GET("api/v4/projects/{id}/repository/commits")
  suspend fun getRepositoryCommits(
    @Path("id") projectId: Long,
    @Query("ref_name") ref: String? = null,
    @Query("path") path: String? = null,
    @Query("per_page") perPage: Int = 30,
    @Query("page") page: Int = 1
  ): Response<List<GitLabCommit>>

  @GET("api/v4/projects/{id}/repository/files/{file_path}/raw")
  suspend fun getRawFileContent(
    @Path("id") projectId: Long,
    @Path(value = "file_path", encoded = true) filePath: String,
    @Query("ref") ref: String? = null
  ): Response<ResponseBody>

  // ---- Repository, releases and project metadata ----

  @GET("api/v4/projects/{id}/languages")
  suspend fun getLanguages(@Path("id") projectId: Long): Response<Map<String, Double>>

  @GET("api/v4/projects/{id}/repository/branches")
  suspend fun getBranches(
    @Path("id") projectId: Long,
    @Query("search") search: String? = null,
    @Query("sort") sort: String = "updated_desc",
    @Query("per_page") perPage: Int = 100,
    @Query("page") page: Int = 1
  ): Response<List<GitLabBranch>>

  @GET("api/v4/projects/{id}/repository/tags")
  suspend fun getTags(
    @Path("id") projectId: Long,
    @Query("search") search: String? = null,
    @Query("order_by") orderBy: String = "updated",
    @Query("sort") sort: String = "desc",
    @Query("per_page") perPage: Int = 50,
    @Query("page") page: Int = 1
  ): Response<List<GitLabTag>>

  @GET("api/v4/projects/{id}/releases")
  suspend fun getReleases(
    @Path("id") projectId: Long,
    @Query("per_page") perPage: Int = 30,
    @Query("page") page: Int = 1
  ): Response<List<GitLabRelease>>

  @GET("api/v4/projects/{id}/repository/contributors")
  suspend fun getContributors(
    @Path("id") projectId: Long,
    @Query("order_by") orderBy: String = "commits",
    @Query("sort") sort: String = "desc",
    @Query("per_page") perPage: Int = 50,
    @Query("page") page: Int = 1
  ): Response<List<GitLabContributor>>

  @GET("api/v4/projects/{id}/labels")
  suspend fun getLabels(
    @Path("id") projectId: Long,
    @Query("per_page") perPage: Int = 100,
    @Query("page") page: Int = 1
  ): Response<List<GitLabLabel>>

  @GET("api/v4/projects/{id}/events")
  suspend fun getProjectEvents(
    @Path("id") projectId: Long,
    @Query("per_page") perPage: Int = 40,
    @Query("page") page: Int = 1
  ): Response<List<GitLabEvent>>

  @GET("api/v4/projects/{id}/repository/commits/{sha}")
  suspend fun getCommit(
    @Path("id") projectId: Long,
    @Path(value = "sha", encoded = true) sha: String,
    @Query("stats") stats: Boolean = true
  ): Response<GitLabCommit>

  @GET("api/v4/projects/{id}/repository/commits/{sha}/diff")
  suspend fun getCommitDiff(
    @Path("id") projectId: Long,
    @Path(value = "sha", encoded = true) sha: String,
    @Query("per_page") perPage: Int = 50,
    @Query("page") page: Int = 1
  ): Response<List<GitLabFileDiff>>

  // ---- Merge request details ----

  @GET("api/v4/projects/{id}/merge_requests/{mr_iid}/notes")
  suspend fun getMergeRequestNotes(
    @Path("id") projectId: Long,
    @Path("mr_iid") mrIid: Long,
    @Query("sort") sort: String = "asc",
    @Query("order_by") orderBy: String = "created_at",
    @Query("per_page") perPage: Int = 100,
    @Query("page") page: Int = 1
  ): Response<List<GitLabNote>>

  @FormUrlEncoded
  @POST("api/v4/projects/{id}/merge_requests/{mr_iid}/notes")
  suspend fun createMergeRequestNote(
    @Path("id") projectId: Long,
    @Path("mr_iid") mrIid: Long,
    @Field("body") body: String
  ): Response<GitLabNote>

  @GET("api/v4/projects/{id}/merge_requests/{mr_iid}/commits")
  suspend fun getMergeRequestCommits(
    @Path("id") projectId: Long,
    @Path("mr_iid") mrIid: Long,
    @Query("per_page") perPage: Int = 50,
    @Query("page") page: Int = 1
  ): Response<List<GitLabCommit>>

  @GET("api/v4/projects/{id}/merge_requests/{mr_iid}/pipelines")
  suspend fun getMergeRequestPipelines(
    @Path("id") projectId: Long,
    @Path("mr_iid") mrIid: Long,
    @Query("per_page") perPage: Int = 20,
    @Query("page") page: Int = 1
  ): Response<List<GitLabPipeline>>

  @GET("api/v4/projects/{id}/merge_requests/{mr_iid}/approvals")
  suspend fun getMergeRequestApprovals(
    @Path("id") projectId: Long,
    @Path("mr_iid") mrIid: Long
  ): Response<GitLabApprovals>

  // ---- Pipelines and jobs ----

  @GET("api/v4/projects/{id}/pipelines/{pipeline_id}")
  suspend fun getPipeline(
    @Path("id") projectId: Long,
    @Path("pipeline_id") pipelineId: Long
  ): Response<GitLabPipeline>

  @POST("api/v4/projects/{id}/pipelines/{pipeline_id}/cancel")
  suspend fun cancelPipeline(
    @Path("id") projectId: Long,
    @Path("pipeline_id") pipelineId: Long
  ): Response<GitLabPipeline>

  @GET("api/v4/projects/{id}/jobs/{job_id}")
  suspend fun getJob(
    @Path("id") projectId: Long,
    @Path("job_id") jobId: Long
  ): Response<GitLabJob>

  @GET("api/v4/projects/{id}/jobs/{job_id}/trace")
  suspend fun getJobTrace(
    @Path("id") projectId: Long,
    @Path("job_id") jobId: Long
  ): Response<ResponseBody>

  @POST("api/v4/projects/{id}/jobs/{job_id}/retry")
  suspend fun retryJob(
    @Path("id") projectId: Long,
    @Path("job_id") jobId: Long
  ): Response<GitLabJob>

  // ---- People, groups, search, activity ----

  @GET("api/v4/users/{id}")
  suspend fun getUser(@Path("id") userId: Long): Response<GitLabUser>

  @GET("api/v4/users")
  suspend fun getUsersByName(
    @Query("username") username: String? = null,
    @Query("search") search: String? = null,
    @Query("per_page") perPage: Int = 20
  ): Response<List<GitLabUser>>

  @GET("api/v4/users/{id}/projects")
  suspend fun getUserProjects(
    @Path("id") userId: Long,
    @Query("order_by") orderBy: String = "last_activity_at",
    @Query("per_page") perPage: Int = 30,
    @Query("page") page: Int = 1
  ): Response<List<GitLabProject>>

  @GET("api/v4/users/{id}/events")
  suspend fun getUserEvents(
    @Path("id") userId: Long,
    @Query("per_page") perPage: Int = 30,
    @Query("page") page: Int = 1
  ): Response<List<GitLabEvent>>

  @GET("api/v4/events")
  suspend fun getMyEvents(
    @Query("per_page") perPage: Int = 30,
    @Query("page") page: Int = 1
  ): Response<List<GitLabEvent>>

  @GET("api/v4/groups/{id}")
  suspend fun getGroup(
    @Path(value = "id", encoded = true) groupId: String,
    @Query("with_projects") withProjects: Boolean = false
  ): Response<GitLabGroup>

  @GET("api/v4/groups/{id}/subgroups")
  suspend fun getSubgroups(
    @Path(value = "id", encoded = true) groupId: String,
    @Query("per_page") perPage: Int = 100,
    @Query("page") page: Int = 1
  ): Response<List<GitLabGroup>>

  @GET("api/v4/search")
  suspend fun searchIssues(
    @Query("search") search: String,
    @Query("scope") scope: String = "issues",
    @Query("per_page") perPage: Int = 30
  ): Response<List<GitLabIssue>>

  @GET("api/v4/search")
  suspend fun searchMergeRequests(
    @Query("search") search: String,
    @Query("scope") scope: String = "merge_requests",
    @Query("per_page") perPage: Int = 30
  ): Response<List<GitLabMergeRequest>>

  @GET("api/v4/todos")
  suspend fun getTodos(
    @Query("state") state: String? = null,
    @Query("per_page") perPage: Int = 50,
    @Query("page") page: Int = 1
  ): Response<List<GitLabTodo>>

  @POST("api/v4/todos/{id}/mark_as_done")
  suspend fun markTodoAsDone(@Path("id") todoId: Long): Response<ResponseBody>

  @POST("api/v4/todos/mark_as_done")
  suspend fun markAllTodosAsDone(): Response<ResponseBody>
}
