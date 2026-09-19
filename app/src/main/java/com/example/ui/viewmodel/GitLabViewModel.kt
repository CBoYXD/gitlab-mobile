package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.GitLabCommit
import com.example.data.model.GitLabFileDiff
import com.example.data.model.GitLabInstance
import com.example.data.model.GitLabIssue
import com.example.data.model.GitLabMergeRequest
import com.example.data.model.GitLabPipeline
import com.example.data.model.GitLabProject
import com.example.data.model.GitLabTreeItem
import com.example.data.repository.GitLabRepository
import com.example.data.storage.InstanceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ScreenDestination {
  object MainTabs : ScreenDestination()
  object UserProfile : ScreenDestination()
  object Instances : ScreenDestination()
  object IssuesList : ScreenDestination()
  object MergeRequestsList : ScreenDestination()
  object PipelinesList : ScreenDestination()
  data class ProjectDetail(val projectId: Long) : ScreenDestination()
  data class FileViewer(val projectId: Long, val filePath: String) : ScreenDestination()
  data class IssueDetail(val issue: GitLabIssue) : ScreenDestination()
  data class MergeRequestDetail(val mr: GitLabMergeRequest) : ScreenDestination()
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

enum class InboxFilter {
  ALL,
  UNREAD,
  ASSIGNED,
  MENTIONED
}

data class GitLabNotification(
  val id: String,
  val type: NotificationType,
  val title: String,
  val projectPath: String,
  val authorName: String,
  val authorUsername: String,
  val timestamp: String,
  val isUnread: Boolean,
  val targetId: Long? = null,
  val subtitle: String = "",
  val statusBadge: String? = null
)

data class ConnectionTestState(
  val isTesting: Boolean = false,
  val isSuccess: Boolean = false,
  val message: String? = null,
  val version: String? = null
)

data class GitLabUiState(
  val activeInstance: GitLabInstance,
  val savedInstances: List<GitLabInstance> = emptyList(),
  val currentTab: BottomTab = BottomTab.HOME,
  val destination: ScreenDestination = ScreenDestination.MainTabs,
  val isLoading: Boolean = false,
  val projects: List<GitLabProject> = emptyList(),
  val searchQuery: String = "",
  val issues: List<GitLabIssue> = emptyList(),
  val mergeRequests: List<GitLabMergeRequest> = emptyList(),
  val selectedProject: GitLabProject? = null,
  val projectTree: List<GitLabTreeItem> = emptyList(),
  val currentTreePath: String = "",
  val selectedFileContent: String? = null,
  val selectedFilePath: String? = null,
  val isFileLoading: Boolean = false,
  val projectCommits: List<GitLabCommit> = emptyList(),
  val projectPipelines: List<GitLabPipeline> = emptyList(),
  val selectedMrDiffs: List<GitLabFileDiff> = emptyList(),
  val isDiffLoading: Boolean = false,
  val selectedIssue: GitLabIssue? = null,
  val selectedMr: GitLabMergeRequest? = null,
  val connectionTestState: ConnectionTestState = ConnectionTestState(),
  val bannerMessage: String? = null,
  val notifications: List<GitLabNotification> = emptyList(),
  val inboxFilter: InboxFilter = InboxFilter.ALL
)

class GitLabViewModel(application: Application) : AndroidViewModel(application) {

  private val instanceStore = InstanceStore(application)
  private val repository = GitLabRepository()

  private val initialNotifications = listOf(
    GitLabNotification(
      id = "notif-1",
      type = NotificationType.MERGE_REQUEST,
      title = "!42: Optimize high-throughput event processing pipeline",
      projectPath = "GrowCrypt/copytrade",
      authorName = "Bohdan",
      authorUsername = "CBoYXD",
      timestamp = "10m ago",
      isUnread = true,
      subtitle = "Requested your review on 3 changed files (+142, -18)",
      statusBadge = "Review requested"
    ),
    GitLabNotification(
      id = "notif-2",
      type = NotificationType.MENTION,
      title = "Mentioned you in #84: SQLite lock contention during burst trade events",
      projectPath = "GrowCrypt/copytrade",
      authorName = "Elena Rostova",
      authorUsername = "elena_dev",
      timestamp = "1h ago",
      isUnread = true,
      subtitle = "\"@CBoYXD can you check if the WAL pragma fixes this?\"",
      statusBadge = "Mentioned"
    ),
    GitLabNotification(
      id = "notif-3",
      type = NotificationType.PIPELINE,
      title = "Pipeline #3082 passed on branch main",
      projectPath = "gitlab-org/gitlab-runner",
      authorName = "GitLab CI",
      authorUsername = "gitlab-bot",
      timestamp = "3h ago",
      isUnread = false,
      subtitle = "All 4 stages passed (build, test, security, deploy) in 4m 12s",
      statusBadge = "Passed"
    ),
    GitLabNotification(
      id = "notif-4",
      type = NotificationType.ISSUE,
      title = "#102: Support GitLab Duo Code Suggestions in Jetpack Compose",
      projectPath = "mobile/gitlab-android",
      authorName = "Alex Chen",
      authorUsername = "alex_c",
      timestamp = "Yesterday",
      isUnread = true,
      subtitle = "Assigned to you by alex_c",
      statusBadge = "Assigned"
    ),
    GitLabNotification(
      id = "notif-5",
      type = NotificationType.DISCUSSION,
      title = "New comment on !38: Add support for custom self-hosted OAuth tokens",
      projectPath = "gitlab-org/gitlab",
      authorName = "Marcus Vance",
      authorUsername = "mvance",
      timestamp = "2d ago",
      isUnread = false,
      subtitle = "\"Nice catch on the bearer authorization header fallback.\"",
      statusBadge = "Comment"
    ),
    GitLabNotification(
      id = "notif-6",
      type = NotificationType.MERGE_REQUEST,
      title = "Merged !35: Dark mode palette alignment with GitHub Mobile UI",
      projectPath = "design-system/tanuki-tokens",
      authorName = "Sarah Jenkins",
      authorUsername = "sjenkins",
      timestamp = "3d ago",
      isUnread = false,
      subtitle = "Merged into main by Sarah Jenkins",
      statusBadge = "Merged"
    )
  )

  private val _uiState = MutableStateFlow(
    GitLabUiState(
      activeInstance = instanceStore.getActiveInstance(),
      savedInstances = instanceStore.getInstances(),
      notifications = initialNotifications
    )
  )
  val uiState: StateFlow<GitLabUiState> = _uiState.asStateFlow()

  init {
    loadAllData()
  }

  fun setInboxFilter(filter: InboxFilter) {
    _uiState.update { it.copy(inboxFilter = filter) }
  }

  fun toggleNotificationRead(id: String) {
    _uiState.update { state ->
      val updated = state.notifications.map {
        if (it.id == id) it.copy(isUnread = !it.isUnread) else it
      }
      state.copy(notifications = updated)
    }
  }

  fun markAllNotificationsAsRead() {
    _uiState.update { state ->
      state.copy(notifications = state.notifications.map { it.copy(isUnread = false) })
    }
  }

  fun switchTab(tab: BottomTab) {
    _uiState.update { it.copy(currentTab = tab, destination = ScreenDestination.MainTabs) }
  }

  fun navigateTo(destination: ScreenDestination) {
    _uiState.update { it.copy(destination = destination) }
    when (destination) {
      is ScreenDestination.ProjectDetail -> loadProjectDetails(destination.projectId)
      is ScreenDestination.FileViewer -> loadFileContent(destination.projectId, destination.filePath)
      is ScreenDestination.MergeRequestDetail -> loadMergeRequestDetails(destination.mr)
      is ScreenDestination.IssueDetail -> _uiState.update { it.copy(selectedIssue = destination.issue) }
      ScreenDestination.MainTabs,
      ScreenDestination.UserProfile,
      ScreenDestination.Instances,
      ScreenDestination.IssuesList,
      ScreenDestination.MergeRequestsList,
      ScreenDestination.PipelinesList -> {}
    }
  }

  fun navigateBack(): Boolean {
    val currentDest = _uiState.value.destination
    return when (currentDest) {
      is ScreenDestination.FileViewer -> {
        _uiState.update { it.copy(destination = ScreenDestination.ProjectDetail(currentDest.projectId)) }
        true
      }
      is ScreenDestination.IssueDetail -> {
        val projId = currentDest.issue.projectId
        _uiState.update { it.copy(destination = if (projId > 0) ScreenDestination.ProjectDetail(projId) else ScreenDestination.IssuesList) }
        true
      }
      is ScreenDestination.MergeRequestDetail -> {
        val projId = currentDest.mr.projectId
        _uiState.update { it.copy(destination = if (projId > 0) ScreenDestination.ProjectDetail(projId) else ScreenDestination.MergeRequestsList) }
        true
      }
      is ScreenDestination.UserProfile,
      is ScreenDestination.Instances,
      is ScreenDestination.IssuesList,
      is ScreenDestination.MergeRequestsList,
      is ScreenDestination.PipelinesList -> {
        _uiState.update { it.copy(destination = ScreenDestination.MainTabs) }
        true
      }
      is ScreenDestination.ProjectDetail -> {
        if (_uiState.value.currentTreePath.isNotEmpty()) {
          // Go up one directory in repository tree
          val parentPath = _uiState.value.currentTreePath.substringBeforeLast("/", "")
          loadRepositoryTree(currentDest.projectId, parentPath)
          true
        } else {
          _uiState.update { it.copy(destination = ScreenDestination.MainTabs) }
          true
        }
      }
      ScreenDestination.MainTabs -> false
    }
  }

  fun setSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
    loadProjects(query)
  }

  fun switchInstance(instanceId: String) {
    instanceStore.setActiveInstance(instanceId)
    val active = instanceStore.getActiveInstance()
    val all = instanceStore.getInstances()
    _uiState.update {
      it.copy(
        activeInstance = active,
        savedInstances = all,
        bannerMessage = "Switched to ${active.name}",
        destination = ScreenDestination.MainTabs
      )
    }
    loadAllData()
  }

  fun addCustomInstance(name: String, url: String, token: String, makeActive: Boolean) {
    viewModelScope.launch {
      val newInstance = GitLabInstance(
        name = name.trim(),
        url = url.trim(),
        token = token.trim(),
        username = if (token.isNotBlank()) "custom_user" else "guest",
        isActive = makeActive,
        isCustom = true
      )
      instanceStore.addInstance(newInstance)
      val updatedList = instanceStore.getInstances()
      val active = instanceStore.getActiveInstance()
      _uiState.update {
        it.copy(
          savedInstances = updatedList,
          activeInstance = active,
          bannerMessage = "Instance '${newInstance.name}' connected!"
        )
      }
      if (makeActive) {
        loadAllData()
      }
    }
  }

  fun deleteInstance(instanceId: String) {
    instanceStore.deleteInstance(instanceId)
    _uiState.update {
      it.copy(
        savedInstances = instanceStore.getInstances(),
        activeInstance = instanceStore.getActiveInstance(),
        bannerMessage = "Instance removed"
      )
    }
    loadAllData()
  }

  fun testConnection(url: String, token: String) {
    viewModelScope.launch {
      _uiState.update {
        it.copy(connectionTestState = ConnectionTestState(isTesting = true))
      }
      val tempInstance = GitLabInstance(name = "Test", url = url, token = token)
      val result = repository.testConnection(tempInstance)
      result.fold(
        onSuccess = { (version, username) ->
          _uiState.update {
            it.copy(
              connectionTestState = ConnectionTestState(
                isTesting = false,
                isSuccess = true,
                message = "Connected to GitLab $version as @$username",
                version = version
              )
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              connectionTestState = ConnectionTestState(
                isTesting = false,
                isSuccess = false,
                message = "Connection failed: ${error.localizedMessage ?: "Unknown error"}"
              )
            )
          }
        }
      )
    }
  }

  fun clearConnectionTestState() {
    _uiState.update { it.copy(connectionTestState = ConnectionTestState()) }
  }

  fun dismissBanner() {
    _uiState.update { it.copy(bannerMessage = null) }
  }

  fun toggleProjectStar(projectId: Long) {
    val isStarred = instanceStore.toggleStarred(projectId)
    _uiState.update { state ->
      val updatedProjects = state.projects.map {
        if (it.id == projectId) it.copy(
          isStarred = isStarred,
          starCount = if (isStarred) it.starCount + 1 else maxOf(0, it.starCount - 1)
        ) else it
      }
      val updatedSelected = if (state.selectedProject?.id == projectId) {
        state.selectedProject.copy(
          isStarred = isStarred,
          starCount = if (isStarred) state.selectedProject.starCount + 1 else maxOf(0, state.selectedProject.starCount - 1)
        )
      } else state.selectedProject
      state.copy(projects = updatedProjects, selectedProject = updatedSelected)
    }
  }

  fun loadAllData() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      val instance = _uiState.value.activeInstance
      val starred = instanceStore.getStarredProjectIds()

      val projects = repository.getProjects(instance).map {
        it.copy(isStarred = starred.contains(it.id))
      }
      val issues = repository.getIssues(instance)
      val mergeRequests = repository.getMergeRequests(instance)
      val firstProjPipelines = if (projects.isNotEmpty()) {
        repository.getPipelines(instance, projects.first().id)
      } else emptyList()

      _uiState.update {
        it.copy(
          isLoading = false,
          projects = projects,
          issues = issues,
          mergeRequests = mergeRequests,
          projectPipelines = firstProjPipelines
        )
      }
    }
  }

  private fun loadProjects(query: String? = null) {
    viewModelScope.launch {
      val instance = _uiState.value.activeInstance
      val starred = instanceStore.getStarredProjectIds()
      val list = repository.getProjects(instance, query).map {
        it.copy(isStarred = starred.contains(it.id))
      }
      _uiState.update { it.copy(projects = list) }
    }
  }

  fun loadProjectDetails(projectId: Long) {
    viewModelScope.launch {
      val instance = _uiState.value.activeInstance
      val project = repository.getProject(instance, projectId)
      _uiState.update {
        it.copy(
          selectedProject = project,
          currentTreePath = ""
        )
      }
      loadRepositoryTree(projectId, "")
      val commits = repository.getRepositoryCommits(instance, projectId)
      val pipelines = repository.getPipelines(instance, projectId)
      _uiState.update {
        it.copy(
          projectCommits = commits,
          projectPipelines = pipelines
        )
      }
    }
  }

  fun loadRepositoryTree(projectId: Long, path: String) {
    viewModelScope.launch {
      val instance = _uiState.value.activeInstance
      val tree = repository.getRepositoryTree(instance, projectId, path)
      _uiState.update {
        it.copy(
          projectTree = tree,
          currentTreePath = path
        )
      }
    }
  }

  private fun loadFileContent(projectId: Long, filePath: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isFileLoading = true, selectedFilePath = filePath) }
      val instance = _uiState.value.activeInstance
      val content = repository.getFileContent(instance, projectId, filePath)
      _uiState.update {
        it.copy(
          isFileLoading = false,
          selectedFileContent = content,
          selectedFilePath = filePath
        )
      }
    }
  }

  private fun loadMergeRequestDetails(mr: GitLabMergeRequest) {
    viewModelScope.launch {
      _uiState.update { it.copy(selectedMr = mr, isDiffLoading = true) }
      val instance = _uiState.value.activeInstance
      val diffs = repository.getMergeRequestDiffs(instance, mr.projectId, mr.iid)
      _uiState.update {
        it.copy(
          isDiffLoading = false,
          selectedMrDiffs = diffs
        )
      }
    }
  }

  fun createNewIssue(projectId: Long, title: String, description: String?, labels: String?) {
    viewModelScope.launch {
      val instance = _uiState.value.activeInstance
      val result = repository.createIssue(instance, projectId, title, description, labels)
      result.onSuccess { newIssue ->
        _uiState.update { state ->
          state.copy(
            issues = listOf(newIssue) + state.issues,
            bannerMessage = "Issue #${newIssue.iid} created successfully!"
          )
        }
      }
    }
  }
}
