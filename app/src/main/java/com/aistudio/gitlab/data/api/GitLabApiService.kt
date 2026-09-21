package com.aistudio.gitlab.data.api

import com.aistudio.gitlab.data.model.GitLabCommit
import com.aistudio.gitlab.data.model.GitLabFileDiff
import com.aistudio.gitlab.data.model.GitLabGroup
import com.aistudio.gitlab.data.model.GitLabIssue
import com.aistudio.gitlab.data.model.GitLabJob
import com.aistudio.gitlab.data.model.GitLabMergeRequest
import com.aistudio.gitlab.data.model.GitLabNote
import com.aistudio.gitlab.data.model.GitLabPipeline
import com.aistudio.gitlab.data.model.GitLabProject
import com.aistudio.gitlab.data.model.GitLabTodo
import com.aistudio.gitlab.data.model.GitLabTreeItem
import com.aistudio.gitlab.data.model.GitLabUser
import com.aistudio.gitlab.data.model.GitLabUserAchievement
import com.aistudio.gitlab.data.model.GitLabUserStatus
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

  @GET("api/v4/user")
  suspend fun getCurrentUser(): Response<GitLabUser>

  @GET("api/v4/user/status")
  suspend fun getUserStatus(): Response<GitLabUserStatus>

  @PUT("api/v4/user/status")
  suspend fun setUserStatus(
    @Query("message") message: String,
    @Query("emoji") emoji: String?
  ): Response<GitLabUserStatus>

  @GET("api/v4/users/{id}/achievements")
  suspend fun getUserAchievements(@Path("id") userId: Long): Response<List<GitLabUserAchievement>>

  @GET("api/v4/version")
  suspend fun getVersion(): Response<GitLabVersionResponse>

  @GET("api/v4/projects")
  suspend fun getProjects(
    @Query("membership") membership: Boolean? = null,
    @Query("starred") starred: Boolean? = null,
    @Query("visibility") visibility: String? = null,
    @Query("search") search: String? = null,
    @Query("order_by") orderBy: String = "last_activity_at",
    @Query("sort") sort: String = "desc",
    @Query("per_page") perPage: Int = 50,
    @Query("page") page: Int = 1
  ): Response<List<GitLabProject>>

  @GET("api/v4/projects/{id}")
  suspend fun getProject(
    @Path(value = "id", encoded = true) projectId: String
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

  @GET("api/v4/issues")
  suspend fun getIssues(
    @Query("scope") scope: String,
    @Query("state") state: String = "all",
    @Query("order_by") orderBy: String = "updated_at",
    @Query("sort") sort: String = "desc",
    @Query("per_page") perPage: Int = 50,
    @Query("page") page: Int = 1
  ): Response<List<GitLabIssue>>

  @GET("api/v4/projects/{id}/issues")
  suspend fun getProjectIssues(
    @Path("id") projectId: Long,
    @Query("state") state: String = "all",
    @Query("order_by") orderBy: String = "updated_at",
    @Query("sort") sort: String = "desc",
    @Query("per_page") perPage: Int = 50,
    @Query("page") page: Int = 1
  ): Response<List<GitLabIssue>>

  @FormUrlEncoded
  @POST("api/v4/projects/{id}/issues")
  suspend fun createIssue(
    @Path("id") projectId: Long,
    @Field("title") title: String,
    @Field("description") description: String?,
    @Field("labels") labels: String?
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
    @Query("page") page: Int = 1
  ): Response<List<GitLabMergeRequest>>

  @GET("api/v4/projects/{id}/merge_requests")
  suspend fun getProjectMergeRequests(
    @Path("id") projectId: Long,
    @Query("state") state: String = "all",
    @Query("order_by") orderBy: String = "updated_at",
    @Query("sort") sort: String = "desc",
    @Query("per_page") perPage: Int = 50,
    @Query("page") page: Int = 1
  ): Response<List<GitLabMergeRequest>>

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
    @Query("per_page") perPage: Int = 100
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
    @Query("per_page") perPage: Int = 30,
    @Query("page") page: Int = 1
  ): Response<List<GitLabCommit>>

  @GET("api/v4/projects/{id}/repository/files/{file_path}/raw")
  suspend fun getRawFileContent(
    @Path("id") projectId: Long,
    @Path(value = "file_path", encoded = true) filePath: String,
    @Query("ref") ref: String? = null
  ): Response<ResponseBody>

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
