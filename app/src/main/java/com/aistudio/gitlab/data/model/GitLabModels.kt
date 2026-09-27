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
  val statusOk: Boolean = false,
  val authKind: String = "",
  val refreshToken: String = "",
  val tokenExpiresAt: Long = 0L,
  val oauthClientId: String = ""
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
  val following: Int? = null,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "work_information") val workInformation: String? = null,
  val linkedin: String? = null,
  val twitter: String? = null,
  val discord: String? = null,
  @Json(name = "is_followed") val isFollowed: Boolean? = null
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
  /** Absent for anonymous requests; null means unknown, not zero. */
  @Json(name = "open_issues_count") val openIssuesCount: Int? = null,
  @Json(name = "last_activity_at") val lastActivityAt: String = "",
  @Json(name = "web_url") val webUrl: String = "",
  @Json(name = "http_url_to_repo") val httpUrlToRepo: String = "",
  @Json(name = "avatar_url") val avatarUrl: String? = null,
  @Json(name = "starred") val isStarred: Boolean = false,
  val topics: List<String> = emptyList(),
  @Json(name = "created_at") val createdAt: String? = null,
  val archived: Boolean = false,
  @Json(name = "empty_repo") val emptyRepo: Boolean = false,
  @Json(name = "readme_url") val readmeUrl: String? = null,
  val namespace: GitLabNamespace? = null,
  val license: GitLabLicense? = null,
  @Json(name = "forked_from_project") val forkedFrom: GitLabProjectRef? = null,
  @Json(name = "issues_enabled") val issuesEnabled: Boolean? = null,
  @Json(name = "merge_requests_enabled") val mergeRequestsEnabled: Boolean? = null,
  @Json(name = "jobs_enabled") val jobsEnabled: Boolean? = null,
  /** ISO timestamp kept for sorting after [lastActivityAt] is formatted for display. */
  @Json(ignore = true) val lastActivityRaw: String = ""
)

@JsonClass(generateAdapter = true)
data class GitLabNamespace(
  val id: Long = 0,
  val name: String = "",
  val path: String = "",
  val kind: String = "",
  @Json(name = "full_path") val fullPath: String = "",
  @Json(name = "avatar_url") val avatarUrl: String? = null,
  @Json(name = "web_url") val webUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitLabLicense(
  val key: String = "",
  val name: String = "",
  val nickname: String? = null
)

@JsonClass(generateAdapter = true)
data class GitLabProjectRef(
  val id: Long = 0,
  val name: String = "",
  @Json(name = "path_with_namespace") val pathWithNamespace: String = ""
)

@JsonClass(generateAdapter = true)
data class GitLabGroup(
  val id: Long,
  val name: String,
  @Json(name = "full_path") val fullPath: String = name,
  val description: String? = null,
  val visibility: String = "",
  @Json(name = "web_url") val webUrl: String = "",
  @Json(name = "avatar_url") val avatarUrl: String? = null,
  @Json(name = "full_name") val fullName: String = "",
  @Json(name = "parent_id") val parentId: Long? = null
)

@JsonClass(generateAdapter = true)
data class GitLabReferences(
  val short: String = "",
  val full: String = ""
)

@JsonClass(generateAdapter = true)
data class GitLabTaskStatus(
  val count: Int = 0,
  @Json(name = "completed_count") val completedCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class GitLabTimeStats(
  @Json(name = "human_time_estimate") val estimate: String? = null,
  @Json(name = "human_total_time_spent") val spent: String? = null
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
  @Json(name = "label_details") val labelDetails: List<GitLabLabel> = emptyList(),
  val upvotes: Int = 0,
  val downvotes: Int = 0,
  @Json(name = "user_notes_count") val userNotesCount: Int = 0,
  @Json(name = "web_url") val webUrl: String = "",
  val milestone: GitLabMilestone? = null,
  @Json(name = "due_date") val dueDate: String? = null,
  val weight: Int? = null,
  val confidential: Boolean = false,
  @Json(name = "discussion_locked") val discussionLocked: Boolean? = null,
  @Json(name = "task_completion_status") val taskStatus: GitLabTaskStatus? = null,
  val references: GitLabReferences? = null,
  @Json(name = "closed_at") val closedAt: String? = null,
  @Json(name = "closed_by") val closedBy: GitLabUser? = null,
  @Json(name = "merge_requests_count") val mergeRequestsCount: Int = 0,
  @Json(name = "time_stats") val timeStats: GitLabTimeStats? = null,
  @Json(name = "issue_type") val issueType: String? = null,
  val severity: String? = null
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
  @Json(name = "label_details") val labelDetails: List<GitLabLabel> = emptyList(),
  @Json(name = "user_notes_count") val userNotesCount: Int = 0,
  @Json(name = "changes_count") val changesCount: String? = null,
  val draft: Boolean = false,
  @Json(name = "work_in_progress") val workInProgress: Boolean = false,
  @Json(name = "web_url") val webUrl: String = "",
  @Json(name = "has_conflicts") val hasConflicts: Boolean? = null,
  @Json(name = "merge_status") val mergeStatus: String? = null,
  @Json(name = "detailed_merge_status") val detailedMergeStatus: String? = null,
  val reviewers: List<GitLabUser> = emptyList(),
  val milestone: GitLabMilestone? = null,
  @Json(name = "head_pipeline") val headPipeline: GitLabPipeline? = null,
  val references: GitLabReferences? = null,
  val upvotes: Int = 0,
  val downvotes: Int = 0,
  @Json(name = "merged_by") val mergedBy: GitLabUser? = null,
  @Json(name = "merge_user") val mergeUser: GitLabUser? = null,
  @Json(name = "merged_at") val mergedAt: String? = null,
  @Json(name = "closed_at") val closedAt: String? = null,
  @Json(name = "closed_by") val closedBy: GitLabUser? = null,
  val squash: Boolean? = null,
  val sha: String? = null,
  @Json(name = "task_completion_status") val taskStatus: GitLabTaskStatus? = null,
  @Json(name = "time_stats") val timeStats: GitLabTimeStats? = null,
  @Json(name = "blocking_discussions_resolved") val discussionsResolved: Boolean? = null,
  @Json(name = "source_project_id") val sourceProjectId: Long? = null,
  @Json(name = "should_remove_source_branch") val removeSourceBranch: Boolean? = null
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
  val stages: List<GitLabStage> = emptyList(),
  val source: String? = null,
  val name: String? = null,
  @Json(name = "before_sha") val beforeSha: String? = null,
  val tag: Boolean? = null,
  @Json(name = "yaml_errors") val yamlErrors: String? = null,
  @Json(name = "started_at") val startedAt: String? = null,
  @Json(name = "finished_at") val finishedAt: String? = null,
  @Json(name = "queued_duration") val queuedDuration: Double? = null
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
  val duration: Double? = null,
  @Json(name = "queued_duration") val queuedDuration: Double? = null,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "started_at") val startedAt: String? = null,
  @Json(name = "finished_at") val finishedAt: String? = null,
  @Json(name = "web_url") val webUrl: String? = null,
  @Json(name = "allow_failure") val allowFailure: Boolean = false,
  @Json(name = "failure_reason") val failureReason: String? = null,
  val ref: String? = null,
  val coverage: Double? = null,
  val runner: GitLabRunner? = null,
  val user: GitLabUser? = null,
  @Json(name = "tag_list") val tagList: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitLabRunner(
  val id: Long = 0,
  val description: String? = null
)

@JsonClass(generateAdapter = true)
data class GitLabCommit(
  val id: String,
  @Json(name = "short_id") val shortId: String = id.take(8),
  val title: String = "",
  @Json(name = "author_name") val authorName: String = "",
  @Json(name = "author_email") val authorEmail: String = "",
  @Json(name = "created_at") val createdAt: String = "",
  val message: String = "",
  @Json(name = "committed_date") val committedDate: String? = null,
  @Json(name = "committer_name") val committerName: String? = null,
  @Json(name = "parent_ids") val parentIds: List<String> = emptyList(),
  @Json(name = "web_url") val webUrl: String? = null,
  val stats: GitLabCommitStats? = null,
  @Json(name = "last_pipeline") val lastPipeline: GitLabPipeline? = null,
  val status: String? = null
)

@JsonClass(generateAdapter = true)
data class GitLabCommitStats(
  val additions: Int = 0,
  val deletions: Int = 0,
  val total: Int = 0
)

@JsonClass(generateAdapter = true)
data class GitLabBranch(
  val name: String,
  val merged: Boolean = false,
  val protected: Boolean = false,
  val default: Boolean = false,
  @Json(name = "web_url") val webUrl: String? = null,
  val commit: GitLabCommit? = null
)

@JsonClass(generateAdapter = true)
data class GitLabTagRelease(
  @Json(name = "tag_name") val tagName: String = "",
  val description: String? = null
)

@JsonClass(generateAdapter = true)
data class GitLabTag(
  val name: String,
  val message: String? = null,
  val target: String? = null,
  val protected: Boolean = false,
  val commit: GitLabCommit? = null,
  val release: GitLabTagRelease? = null
)

@JsonClass(generateAdapter = true)
data class GitLabReleaseLink(
  val id: Long = 0,
  val name: String = "",
  val url: String = "",
  @Json(name = "link_type") val linkType: String? = null
)

@JsonClass(generateAdapter = true)
data class GitLabReleaseSource(
  val format: String = "",
  val url: String = ""
)

@JsonClass(generateAdapter = true)
data class GitLabReleaseAssets(
  val count: Int = 0,
  val links: List<GitLabReleaseLink> = emptyList(),
  val sources: List<GitLabReleaseSource> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitLabRelease(
  @Json(name = "tag_name") val tagName: String,
  val name: String? = null,
  val description: String? = null,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "released_at") val releasedAt: String? = null,
  @Json(name = "upcoming_release") val upcoming: Boolean = false,
  val author: GitLabUser? = null,
  val assets: GitLabReleaseAssets? = null,
  val commit: GitLabCommit? = null
)

@JsonClass(generateAdapter = true)
data class GitLabContributor(
  val name: String = "",
  val commits: Int = 0,
  val additions: Int = 0,
  val deletions: Int = 0
)

@JsonClass(generateAdapter = true)
data class GitLabLabel(
  val id: Long = 0,
  val name: String = "",
  val color: String? = null,
  @Json(name = "text_color") val textColor: String? = null,
  val description: String? = null
)

@JsonClass(generateAdapter = true)
data class GitLabApprovalUser(
  val user: GitLabUser = GitLabUser()
)

@JsonClass(generateAdapter = true)
data class GitLabApprovals(
  val approved: Boolean? = null,
  @Json(name = "approvals_required") val approvalsRequired: Int? = null,
  @Json(name = "approvals_left") val approvalsLeft: Int? = null,
  @Json(name = "approved_by") val approvedBy: List<GitLabApprovalUser> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitLabPushData(
  @Json(name = "commit_count") val commitCount: Int = 0,
  val action: String? = null,
  @Json(name = "ref_type") val refType: String? = null,
  val ref: String? = null,
  @Json(name = "commit_title") val commitTitle: String? = null
)

@JsonClass(generateAdapter = true)
data class GitLabEventNote(
  val body: String? = null,
  @Json(name = "noteable_type") val noteableType: String? = null,
  @Json(name = "noteable_iid") val noteableIid: Long? = null
)

@JsonClass(generateAdapter = true)
data class GitLabEvent(
  val id: Long,
  @Json(name = "project_id") val projectId: Long? = null,
  @Json(name = "action_name") val actionName: String = "",
  @Json(name = "target_type") val targetType: String? = null,
  @Json(name = "target_title") val targetTitle: String? = null,
  @Json(name = "target_iid") val targetIid: Long? = null,
  val author: GitLabUser = GitLabUser(),
  @Json(name = "created_at") val createdAt: String = "",
  @Json(name = "push_data") val pushData: GitLabPushData? = null,
  val note: GitLabEventNote? = null
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
  val system: Boolean = false,
  @Json(name = "updated_at") val updatedAt: String? = null,
  val type: String? = null,
  val resolvable: Boolean = false,
  val resolved: Boolean? = null,
  val internal: Boolean? = null
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
