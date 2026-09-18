package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

/**
 * Model representing a connected GitLab instance (GitLab.com or custom self-hosted).
 */
data class GitLabInstance(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val url: String,
  val token: String = "",
  val username: String = "",
  val isActive: Boolean = false,
  val isCustom: Boolean = true,
  val version: String = "16.11-ee",
  val statusOk: Boolean = true
)

@JsonClass(generateAdapter = true)
data class GitLabUser(
  val id: Long = 0,
  val username: String = "",
  val name: String = "",
  val state: String = "active",
  @Json(name = "avatar_url") val avatarUrl: String? = null,
  @Json(name = "web_url") val webUrl: String? = null,
  val bio: String? = null,
  @Json(name = "public_email") val publicEmail: String? = null
)

@JsonClass(generateAdapter = true)
data class GitLabProject(
  val id: Long,
  val name: String,
  @Json(name = "name_with_namespace") val nameWithNamespace: String = name,
  @Json(name = "path_with_namespace") val pathWithNamespace: String = name,
  val description: String? = null,
  @Json(name = "default_branch") val defaultBranch: String = "main",
  val visibility: String = "public", // public, internal, private
  @Json(name = "star_count") val starCount: Int = 0,
  @Json(name = "forks_count") val forksCount: Int = 0,
  @Json(name = "open_issues_count") val openIssuesCount: Int = 0,
  @Json(name = "last_activity_at") val lastActivityAt: String = "",
  @Json(name = "web_url") val webUrl: String = "",
  @Json(name = "avatar_url") val avatarUrl: String? = null,
  val isStarred: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GitLabIssue(
  val id: Long,
  val iid: Long,
  @Json(name = "project_id") val projectId: Long,
  val title: String,
  val description: String? = null,
  val state: String = "opened", // opened, closed
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
  val state: String = "active"
)

@JsonClass(generateAdapter = true)
data class GitLabMergeRequest(
  val id: Long,
  val iid: Long,
  @Json(name = "project_id") val projectId: Long,
  val title: String,
  val description: String? = null,
  val state: String = "opened", // opened, merged, closed
  @Json(name = "source_branch") val sourceBranch: String = "",
  @Json(name = "target_branch") val targetBranch: String = "main",
  val author: GitLabUser = GitLabUser(),
  val assignees: List<GitLabUser> = emptyList(),
  val labels: List<String> = emptyList(),
  @Json(name = "user_notes_count") val userNotesCount: Int = 0,
  @Json(name = "changes_count") val changesCount: String? = "0",
  val draft: Boolean = false,
  @Json(name = "web_url") val webUrl: String = "",
  @Json(name = "has_conflicts") val hasConflicts: Boolean = false,
  @Json(name = "merge_status") val mergeStatus: String = "can_be_merged"
)

@JsonClass(generateAdapter = true)
data class GitLabPipeline(
  val id: Long,
  @Json(name = "project_id") val projectId: Long = 0,
  val status: String = "success", // success, running, failed, canceled, pending, skipped
  val ref: String = "main",
  val sha: String = "",
  @Json(name = "web_url") val webUrl: String = "",
  @Json(name = "created_at") val createdAt: String = "",
  @Json(name = "updated_at") val updatedAt: String = "",
  val duration: Long? = 0,
  val coverage: String? = null,
  val user: GitLabUser = GitLabUser(),
  val stages: List<GitLabStage> = emptyList()
)

data class GitLabStage(
  val name: String,
  val status: String,
  val jobs: List<GitLabJob> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitLabJob(
  val id: Long,
  val name: String,
  val stage: String,
  val status: String,
  val duration: Float? = null
)

@JsonClass(generateAdapter = true)
data class GitLabCommit(
  val id: String,
  @Json(name = "short_id") val shortId: String = id.take(8),
  val title: String,
  @Json(name = "author_name") val authorName: String = "",
  @Json(name = "author_email") val authorEmail: String = "",
  @Json(name = "created_at") val createdAt: String = "",
  val message: String = ""
)

@JsonClass(generateAdapter = true)
data class GitLabTreeItem(
  val id: String,
  val name: String,
  val type: String, // "tree" (dir) or "blob" (file)
  val path: String,
  val mode: String = "100644"
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
  val body: String,
  val author: GitLabUser = GitLabUser(),
  @Json(name = "created_at") val createdAt: String = "",
  val system: Boolean = false
)

data class GitLabVersion(
  val version: String = "16.11.0-ee",
  val revision: String = "7a83d02"
)
