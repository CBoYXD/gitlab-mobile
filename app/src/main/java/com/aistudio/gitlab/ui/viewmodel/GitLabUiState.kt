package com.aistudio.gitlab.ui.viewmodel

import com.aistudio.gitlab.data.model.GitLabApprovals
import com.aistudio.gitlab.data.model.GitLabBranch
import com.aistudio.gitlab.data.model.GitLabCommit
import com.aistudio.gitlab.data.model.GitLabContributor
import com.aistudio.gitlab.data.model.GitLabEvent
import com.aistudio.gitlab.data.model.GitLabJob
import com.aistudio.gitlab.data.model.GitLabLabel
import com.aistudio.gitlab.data.model.GitLabRelease
import com.aistudio.gitlab.data.model.GitLabTag
import com.aistudio.gitlab.data.repository.JobTrace
import com.aistudio.gitlab.data.model.GitLabFileDiff
import com.aistudio.gitlab.data.model.GitLabGroup
import com.aistudio.gitlab.data.model.GitLabInstance
import com.aistudio.gitlab.data.model.GitLabIssue
import com.aistudio.gitlab.data.model.GitLabMergeRequest
import com.aistudio.gitlab.data.model.GitLabNote
import com.aistudio.gitlab.data.model.GitLabPipeline
import com.aistudio.gitlab.data.model.GitLabProject
import com.aistudio.gitlab.data.model.GitLabTreeItem
import com.aistudio.gitlab.data.model.GitLabUser
import com.aistudio.gitlab.data.model.GitLabUserStatus

/**
 * A value that loads on its own: screens render data, a spinner, or an error with retry
 * independently of the rest of the screen.
 */
data class Loadable<T>(
  val data: T,
  val loading: Boolean = false,
  val error: String? = null,
  val loaded: Boolean = false,
) {
  /** First load in progress with nothing to show yet. */
  val initialLoading: Boolean get() = loading && !loaded

  fun start() = copy(loading = true, error = null)
  fun success(value: T) = copy(data = value, loading = false, error = null, loaded = true)
  fun failure(message: String) = copy(loading = false, error = message, loaded = true)
}

fun <T> emptyFeed(): Loadable<List<T>> = Loadable(emptyList())

sealed class ScreenDestination {
  data object MainTabs : ScreenDestination()
  data object UserProfile : ScreenDestination()
  data object Instances : ScreenDestination()
  data object IssuesList : ScreenDestination()
  data object MergeRequestsList : ScreenDestination()
  data object PipelinesList : ScreenDestination()
  data object GroupsList : ScreenDestination()
  data class GroupProjects(val group: GitLabGroup) : ScreenDestination()
  data class ProjectDetail(val projectId: Long) : ScreenDestination()
  /** Repository browser; path and ref live in [ProjectDetailState]. */
  data class ProjectCode(val projectId: Long) : ScreenDestination()
  data class ProjectSectionList(val projectId: Long, val section: ProjectSection) : ScreenDestination()
  data class FileViewer(val projectId: Long, val filePath: String, val ref: String? = null) : ScreenDestination()
  data class CommitDetail(val projectId: Long, val sha: String) : ScreenDestination()
  data class PipelineDetail(val projectId: Long, val pipelineId: Long) : ScreenDestination()
  data class JobDetail(val projectId: Long, val jobId: Long) : ScreenDestination()
  data class ReleaseDetail(val projectId: Long, val tagName: String) : ScreenDestination()
  /** A person; [userId] 0 means "look up by [username]" (from an @mention). */
  data class UserDetail(val userId: Long, val username: String = "") : ScreenDestination()
  data object Search : ScreenDestination()
  data class IssueDetail(val projectId: Long, val iid: Long) : ScreenDestination()
  data class MergeRequestDetail(val projectId: Long, val iid: Long) : ScreenDestination()
}

enum class BottomTab(val title: String) {
  HOME("Home"),
  INBOX("Inbox"),
  PROJECTS("Projects")
}

enum class NotificationType {
  ISSUE,
  MERGE_REQUEST,
  PIPELINE,
  MENTION,
  DISCUSSION
}

enum class InboxFilter(val label: String) {
  ALL("All"),
  UNREAD("To do"),
  ASSIGNED("Assigned"),
  MENTIONED("Mentioned")
}

data class GitLabNotification(
  val id: String,
  val type: NotificationType,
  val title: String,
  val projectPath: String,
  val authorName: String,
  val authorUsername: String,
  val authorAvatarUrl: String?,
  val timestamp: String,
  val isUnread: Boolean,
  val actionName: String,
  val targetType: String?,
  val targetProjectId: Long?,
  val targetIid: Long?,
  val targetUrl: String?,
  val subtitle: String = "",
  val statusBadge: String? = null
)

data class LoginState(
  val inProgress: Boolean = false,
  val needsTwoFactor: Boolean = false,
  val awaitingBrowser: Boolean = false,
  val message: String? = null,
  val isError: Boolean = false,
  val finished: Boolean = false
)

/** Lists reachable from a project's page, GitHub style. */
enum class ProjectSection(val title: String) {
  ISSUES("Issues"),
  MERGE_REQUESTS("Merge requests"),
  PIPELINES("Pipelines"),
  COMMITS("Commits"),
  BRANCHES("Branches"),
  TAGS("Tags"),
  RELEASES("Releases"),
  CONTRIBUTORS("Contributors"),
  ACTIVITY("Activity"),
}

data class ProjectDetailState(
  val projectId: Long = 0,
  val project: Loadable<GitLabProject?> = Loadable(null),
  val readme: Loadable<String?> = Loadable(null),
  val languages: Loadable<List<Pair<String, Double>>> = emptyFeed(),
  /** Branch or tag the code browser, commits and README follow. Null means the default branch. */
  val ref: String? = null,
  val treePath: String = "",
  val tree: Loadable<List<GitLabTreeItem>> = emptyFeed(),
  val issues: Loadable<List<GitLabIssue>> = emptyFeed(),
  val mergeRequests: Loadable<List<GitLabMergeRequest>> = emptyFeed(),
  val pipelines: Loadable<List<GitLabPipeline>> = emptyFeed(),
  val commits: Loadable<List<GitLabCommit>> = emptyFeed(),
  val branches: Loadable<List<GitLabBranch>> = emptyFeed(),
  val tags: Loadable<List<GitLabTag>> = emptyFeed(),
  val releases: Loadable<List<GitLabRelease>> = emptyFeed(),
  val contributors: Loadable<List<GitLabContributor>> = emptyFeed(),
  val events: Loadable<List<GitLabEvent>> = emptyFeed(),
) {
  val effectiveRef: String? get() = ref ?: project.data?.defaultBranch
  val webUrl: String? get() = project.data?.webUrl?.takeIf { it.isNotBlank() }
}

data class CommitDetailState(
  val projectId: Long = 0,
  val sha: String = "",
  val commit: Loadable<GitLabCommit?> = Loadable(null),
  val diffs: Loadable<List<GitLabFileDiff>> = emptyFeed(),
)

data class PipelineDetailState(
  val projectId: Long = 0,
  val pipelineId: Long = 0,
  val pipeline: Loadable<GitLabPipeline?> = Loadable(null),
  val jobs: Loadable<List<GitLabJob>> = emptyFeed(),
  val busy: Boolean = false,
)

data class JobDetailState(
  val projectId: Long = 0,
  val jobId: Long = 0,
  val job: Loadable<GitLabJob?> = Loadable(null),
  val trace: Loadable<JobTrace?> = Loadable(null),
  val busy: Boolean = false,
)

data class UserDetailState(
  val userId: Long = 0,
  val username: String = "",
  val user: Loadable<GitLabUser?> = Loadable(null),
  val projects: Loadable<List<GitLabProject>> = emptyFeed(),
  val events: Loadable<List<GitLabEvent>> = emptyFeed(),
)

data class GroupDetailState(
  val group: GitLabGroup? = null,
  val details: Loadable<GitLabGroup?> = Loadable(null),
  val subgroups: Loadable<List<GitLabGroup>> = emptyFeed(),
  val projects: Loadable<List<GitLabProject>> = emptyFeed(),
)

enum class SearchScope(val title: String) {
  PROJECTS("Projects"),
  ISSUES("Issues"),
  MERGE_REQUESTS("Merge requests"),
  USERS("People"),
}

data class SearchState(
  val query: String = "",
  val scope: SearchScope = SearchScope.PROJECTS,
  val projects: Loadable<List<GitLabProject>> = emptyFeed(),
  val issues: Loadable<List<GitLabIssue>> = emptyFeed(),
  val mergeRequests: Loadable<List<GitLabMergeRequest>> = emptyFeed(),
  val users: Loadable<List<GitLabUser>> = emptyFeed(),
)

data class FileState(
  val projectId: Long = 0,
  val path: String = "",
  val content: Loadable<String?> = Loadable(null),
  val ref: String? = null,
)

data class IssueDetailState(
  val projectId: Long = 0,
  val iid: Long = 0,
  val issue: Loadable<GitLabIssue?> = Loadable(null),
  val notes: Loadable<List<GitLabNote>> = emptyFeed(),
  val busy: Boolean = false,
  val posting: Boolean = false,
)

data class MergeRequestDetailState(
  val projectId: Long = 0,
  val iid: Long = 0,
  val mergeRequest: Loadable<GitLabMergeRequest?> = Loadable(null),
  val diffs: Loadable<List<GitLabFileDiff>> = emptyFeed(),
  val notes: Loadable<List<GitLabNote>> = emptyFeed(),
  val commits: Loadable<List<GitLabCommit>> = emptyFeed(),
  val pipelines: Loadable<List<GitLabPipeline>> = emptyFeed(),
  val approvals: Loadable<GitLabApprovals?> = Loadable(null),
  val busy: Boolean = false,
  val posting: Boolean = false,
)

data class GitLabUiState(
  /** False until saved servers have been read from encrypted storage. */
  val bootstrapped: Boolean = false,
  val storageError: String? = null,
  val activeInstance: GitLabInstance? = null,
  val savedInstances: List<GitLabInstance> = emptyList(),
  val gitLabComClientId: String = "",
  val sessionExpired: Boolean = false,

  val currentTab: BottomTab = BottomTab.HOME,
  val backStack: List<ScreenDestination> = emptyList(),

  val refreshing: Boolean = false,
  val currentUser: Loadable<GitLabUser?> = Loadable(null),
  val userStatus: GitLabUserStatus? = null,
  val profileReadme: String? = null,
  val projects: Loadable<List<GitLabProject>> = emptyFeed(),
  val groups: Loadable<List<GitLabGroup>> = emptyFeed(),
  val issues: Loadable<List<GitLabIssue>> = emptyFeed(),
  val mergeRequests: Loadable<List<GitLabMergeRequest>> = emptyFeed(),
  val pipelines: Loadable<List<GitLabPipeline>> = emptyFeed(),
  val notifications: Loadable<List<GitLabNotification>> = emptyFeed(),
  val inboxFilter: InboxFilter = InboxFilter.ALL,

  val searchQuery: String = "",
  val searchResults: Loadable<List<GitLabProject>> = emptyFeed(),
  val group: GroupDetailState = GroupDetailState(),
  val activity: Loadable<List<GitLabEvent>> = emptyFeed(),
  val pendingStars: Set<Long> = emptySet(),

  val project: ProjectDetailState = ProjectDetailState(),
  val file: FileState = FileState(),
  val issueDetail: IssueDetailState = IssueDetailState(),
  val mergeRequestDetail: MergeRequestDetailState = MergeRequestDetailState(),
  val commitDetail: CommitDetailState = CommitDetailState(),
  val pipelineDetail: PipelineDetailState = PipelineDetailState(),
  val jobDetail: JobDetailState = JobDetailState(),
  val userDetail: UserDetailState = UserDetailState(),
  val search: SearchState = SearchState(),
  /** Label colors per project id, fetched once per project. */
  val labels: Map<Long, Map<String, GitLabLabel>> = emptyMap(),

  val loginState: LoginState = LoginState(),
) {
  val destination: ScreenDestination get() = backStack.lastOrNull() ?: ScreenDestination.MainTabs
  val isSignedIn: Boolean get() = activeInstance?.token?.isNotBlank() == true
  val unreadCount: Int get() = notifications.data.count { it.isUnread }
}

/** One-off message shown as a snackbar. Errors with an action stay until dismissed. */
data class UiMessage(
  val text: String,
  val isError: Boolean = false,
  val actionLabel: String? = null,
  val action: (() -> Unit)? = null,
)
