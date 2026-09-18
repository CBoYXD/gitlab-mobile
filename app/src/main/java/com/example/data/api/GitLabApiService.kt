package com.example.data.api

import com.example.data.model.GitLabCommit
import com.example.data.model.GitLabFileDiff
import com.example.data.model.GitLabIssue
import com.example.data.model.GitLabMergeRequest
import com.example.data.model.GitLabPipeline
import com.example.data.model.GitLabProject
import com.example.data.model.GitLabTreeItem
import com.example.data.model.GitLabUser
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class MergeRequestChangesResponse(
  val id: Long,
  val iid: Long,
  val title: String,
  val changes: List<GitLabFileDiff> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitLabVersionResponse(
  val version: String = "16.11-ee",
  val revision: String? = null
)

interface GitLabApiService {

  @GET("api/v4/user")
  suspend fun getCurrentUser(): Response<GitLabUser>

  @GET("api/v4/version")
  suspend fun getVersion(): Response<GitLabVersionResponse>

  @GET("api/v4/projects")
  suspend fun getProjects(
    @Query("membership") membership: Boolean? = null,
    @Query("order_by") orderBy: String = "last_activity_at",
    @Query("sort") sort: String = "desc",
    @Query("search") search: String? = null,
    @Query("per_page") perPage: Int = 30,
    @Query("page") page: Int = 1
  ): Response<List<GitLabProject>>

  @GET("api/v4/projects/{id}")
  suspend fun getProject(
    @Path("id") projectId: Long
  ): Response<GitLabProject>

  @GET("api/v4/projects/{id}/issues")
  suspend fun getProjectIssues(
    @Path("id") projectId: Long,
    @Query("state") state: String? = null,
    @Query("per_page") perPage: Int = 30
  ): Response<List<GitLabIssue>>

  @FormUrlEncoded
  @POST("api/v4/projects/{id}/issues")
  suspend fun createIssue(
    @Path("id") projectId: Long,
    @Field("title") title: String,
    @Field("description") description: String?,
    @Field("labels") labels: String?
  ): Response<GitLabIssue>

  @GET("api/v4/projects/{id}/merge_requests")
  suspend fun getProjectMergeRequests(
    @Path("id") projectId: Long,
    @Query("state") state: String? = null,
    @Query("per_page") perPage: Int = 30
  ): Response<List<GitLabMergeRequest>>

  @GET("api/v4/projects/{id}/merge_requests/{mr_iid}/changes")
  suspend fun getMergeRequestChanges(
    @Path("id") projectId: Long,
    @Path("mr_iid") mrIid: Long
  ): Response<MergeRequestChangesResponse>

  @GET("api/v4/projects/{id}/pipelines")
  suspend fun getProjectPipelines(
    @Path("id") projectId: Long,
    @Query("per_page") perPage: Int = 20
  ): Response<List<GitLabPipeline>>

  @GET("api/v4/projects/{id}/repository/tree")
  suspend fun getRepositoryTree(
    @Path("id") projectId: Long,
    @Query("path") path: String? = null,
    @Query("ref") ref: String? = null,
    @Query("per_page") perPage: Int = 100
  ): Response<List<GitLabTreeItem>>

  @GET("api/v4/projects/{id}/repository/commits")
  suspend fun getRepositoryCommits(
    @Path("id") projectId: Long,
    @Query("ref_name") ref: String? = null,
    @Query("per_page") perPage: Int = 30
  ): Response<List<GitLabCommit>>

  @GET("api/v4/projects/{id}/repository/files/{file_path}/raw")
  suspend fun getRawFileContent(
    @Path("id") projectId: Long,
    @Path(value = "file_path", encoded = true) filePath: String,
    @Query("ref") ref: String = "main"
  ): Response<ResponseBody>

  @GET("api/v4/issues")
  suspend fun getGlobalIssues(
    @Query("scope") scope: String = "all",
    @Query("state") state: String? = null,
    @Query("per_page") perPage: Int = 30
  ): Response<List<GitLabIssue>>

  @GET("api/v4/merge_requests")
  suspend fun getGlobalMergeRequests(
    @Query("scope") scope: String = "all",
    @Query("state") state: String? = null,
    @Query("per_page") perPage: Int = 30
  ): Response<List<GitLabMergeRequest>>

  @POST("api/v4/projects/{id}/star")
  suspend fun starProject(@Path("id") projectId: Long): Response<GitLabProject>

  @POST("api/v4/projects/{id}/unstar")
  suspend fun unstarProject(@Path("id") projectId: Long): Response<GitLabProject>
}
