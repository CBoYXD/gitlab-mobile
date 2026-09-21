package com.aistudio.gitlab.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

data class GitLabInstance(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val url: String,
  val token: String = "",
  val username: String = "",
  val isActive: Boolean = false,
  val isCustom: Boolean = true,
  val version: String = "",
  val statusOk: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GitLabUser(
  val id: Long = 0,
  val username: String = "",
  val name: String = "",
  val state: String = "",
  @Json(name = "avatar_url") val avatarUrl: String? = null,
  @Json(name = "web_url") val webUrl: String? = null,
  val bio: String? = null,
  @Json(name = "public_email") val publicEmail: String? = null,
  val location: String? = null,
  @Json(name = "website_url") val websiteUrl: String? = null,
  val pronouns: String? = null,
  val organization: String? = null,
  @Json(name = "job_title") val jobTitle: String? = null,
  val followers: Int? = null,
  val following: Int? = null
)

@JsonClass(generateAdapter = true)
data class GitLabUserStatus(
  val emoji: String? = null,
  val message: String? = null,
  val availability: String? = null
)

@JsonClass(generateAdapter = true)
data class GitLabAchievement(
  val id: Long = 0,
  val name: String = "",
  val description: String? = null,
  @Json(name = "avatar_url") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitLabUserAchievement(
  val id: Long = 0,
  val achievement: GitLabAchievement? = null
) {
  val title: String get() = achievement?.name?.takeIf { it.isNotBlank() } ?: ""
}

@JsonClass(generateAdapter = true)
data class GitLabProject(
  val id: Long,
  val name: String,
  @Json(name = "name_with_namespace") val nameWithNamespace: String = name,
  @Json(name = "path_with_namespace") val pathWithNamespace: String = name,
  val description: String? = null,
  @Json(name = "default_branch") val defaultBranch: String? = null,
  val visibility: String = "",
  @Json(name = "star_count") val starCount: Int = 0,
  @Json(name = "forks_count") val forksCount: Int = 0,
  @Json(name = "open_issues_count") val openIssuesCount: Int = 0,
  @Json(name = "last_activity_at") val lastActivityAt: String = "",
  @Json(name = "web_url") val webUrl: String = "",
  @Json(name = "http_url_to_repo") val httpUrlToRepo: String = "",
  @Json(name = "avatar_url") val avatarUrl: String? = null,
  @Json(name = "starred") val isStarred: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GitLabGroup(
  val id: Long,
  val name: String,
  @Json(name = "full_path") val fullPath: String = name,
  val description: String? = null,
  val visibility: String = "",
  @Json(name = "web_url") val webUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class GitLabIssue(
  val id: Long,
  val iid: Long,
  @Json(name = "project_id") val projectId: Long,
  val title: String,
  val description: String? = null,
  val state: String = "",
  @Json(name = "created_at") val createdAt: String = "",
  @Json(name = "updated_at") val updatedAt: String = "",
  val author: GitLabUser = GitLabUser(),
  val assignees: List<GitLabUser> = emptyList(),
  val labels: List<String> = emptyList(),
  val upvotes: Int = 0,
  val downvotes: Int = 0,
  @Json(name = "user_notes_count") val userNotesCount: Int = 0,
  @Json(name = "web_url") val webUrl: String = "",
  val milestone: GitLabMilestone? = null
)

@JsonClass(generateAdapter = true)
data class GitLabMilestone(
  val id: Long = 0,
  val title: String = "",
  val state: String = ""
)

@JsonClass(generateAdapter = true)
data class GitLabMergeRequest(
  val id: Long,
  val iid: Long,
  @Json(name = "project_id") val projectId: Long,
  val title: String,
  val description: String? = null,
  val state: String = "",
  @Json(name = "source_branch") val sourceBranch: String = "",
  @Json(name = "target_branch") val targetBranch: String = "",
  @Json(name = "created_at") val createdAt: String = "",
  @Json(name = "updated_at") val updatedAt: String = "",
  val author: GitLabUser = GitLabUser(),
  val assignees: List<GitLabUser> = emptyList(),
  val labels: List<String> = emptyList(),
  @Json(name = "user_notes_count") val userNotesCount: Int = 0,
  @Json(name = "changes_count") val changesCount: String? = null,
  val draft: Boolean = false,
  @Json(name = "work_in_progress") val workInProgress: Boolean = false,
  @Json(name = "web_url") val webUrl: String = "",
  @Json(name = "has_conflicts") val hasConflicts: Boolean? = null,
  @Json(name = "merge_status") val mergeStatus: String? = null
) {
  val isDraft: Boolean get() = draft || workInProgress
}

@JsonClass(generateAdapter = true)
data class GitLabPipeline(
  val id: Long,
  @Json(name = "project_id") val projectId: Long = 0,
  val status: String = "",
  val ref: String = "",
  val sha: String = "",
  @Json(name = "web_url") val webUrl: String = "",
  @Json(name = "created_at") val createdAt: String = "",
  @Json(name = "updated_at") val updatedAt: String = "",
  val duration: Double? = null,
  val coverage: String? = null,
  val user: GitLabUser? = null,
  val stages: List<GitLabStage> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitLabStage(
  val name: String,
  val status: String,
  val jobs: List<GitLabJob> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitLabJob(
  val id: Long,
  val name: String,
  val stage: String = "",
  val status: String = "",
  val duration: Double? = null
)

@JsonClass(generateAdapter = true)
data class GitLabCommit(
  val id: String,
  @Json(name = "short_id") val shortId: String = id.take(8),
  val title: String = "",
  @Json(name = "author_name") val authorName: String = "",
  @Json(name = "author_email") val authorEmail: String = "",
  @Json(name = "created_at") val createdAt: String = "",
  val message: String = ""
)

@JsonClass(generateAdapter = true)
data class GitLabTreeItem(
  val id: String,
  val name: String,
  val type: String,
  val path: String,
  val mode: String = ""
)

@JsonClass(generateAdapter = true)
data class GitLabFileDiff(
  @Json(name = "old_path") val oldPath: String = "",
  @Json(name = "new_path") val newPath: String = "",
  val diff: String = "",
  @Json(name = "new_file") val newFile: Boolean = false,
  @Json(name = "renamed_file") val renamedFile: Boolean = false,
  @Json(name = "deleted_file") val deletedFile: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GitLabNote(
  val id: Long,
  val body: String = "",
  val author: GitLabUser = GitLabUser(),
  @Json(name = "created_at") val createdAt: String = "",
  val system: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GitLabTodoTarget(
  val iid: Long = 0,
  val title: String? = null,
  @Json(name = "project_id") val projectId: Long? = null,
  val state: String? = null
)

@JsonClass(generateAdapter = true)
data class GitLabTodo(
  val id: Long,
  val project: GitLabProject? = null,
  val author: GitLabUser = GitLabUser(),
  @Json(name = "action_name") val actionName: String = "",
  @Json(name = "target_type") val targetType: String? = null,
  val target: GitLabTodoTarget? = null,
  @Json(name = "target_url") val targetUrl: String? = null,
  val body: String? = null,
  val state: String = "",
  @Json(name = "created_at") val createdAt: String = ""
)
