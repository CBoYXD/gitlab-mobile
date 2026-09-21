package com.aistudio.gitlab.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.gitlab.data.api.formatGitLabTime
import com.aistudio.gitlab.data.api.isAuthError
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
import com.aistudio.gitlab.data.repository.GitLabRepository
import com.aistudio.gitlab.data.storage.InstanceStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ScreenDestination {
  data object MainTabs : ScreenDestination()
  data object UserProfile : ScreenDestination()
  data object Instances : ScreenDestination()
  data object IssuesList : ScreenDestination()
  data object MergeRequestsList : ScreenDestination()
  data object PipelinesList : ScreenDestination()
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
  val activeInstance: GitLabInstance? = null,
  val savedInstances: List<GitLabInstance> = emptyList(),
  val currentTab: BottomTab = BottomTab.HOME,
  val destination: ScreenDestination = ScreenDestination.MainTabs,
  val isLoading: Boolean = false,
  val projects: List<GitLabProject> = emptyList(),
  val groups: List<GitLabGroup> = emptyList(),
  val searchQuery: String = "",
  val issues: List<GitLabIssue> = emptyList(),
  val mergeRequests: List<GitLabMergeRequest> = emptyList(),
  val currentUser: GitLabUser? = null,
  val userStatus: GitLabUserStatus? = null,
  val achievements: List<GitLabUserAchievement> = emptyList(),
  val profileReadme: String? = null,
  val selectedProject: GitLabProject? = null,
  val projectTree: List<GitLabTreeItem> = emptyList(),
  val currentTreePath: String = "",
  val selectedFileContent: String? = null,
  val selectedFilePath: String? = null,
  val fileError: String? = null,
  val isFileLoading: Boolean = false,
  val projectCommits: List<GitLabCommit> = emptyList(),
  val projectPipelines: List<GitLabPipeline> = emptyList(),
  val projectIssues: List<GitLabIssue> = emptyList(),
  val projectMergeRequests: List<GitLabMergeRequest> = emptyList(),
  val projectReadme: String? = null,
  val selectedMrDiffs: List<GitLabFileDiff> = emptyList(),
  val isDiffLoading: Boolean = false,
  val issueNotes: List<GitLabNote> = emptyList(),
  val isNotesLoading: Boolean = false,
  val selectedIssue: GitLabIssue? = null,
  val selectedMr: GitLabMergeRequest? = null,
  val connectionTestState: ConnectionTestState = ConnectionTestState(),
  val bannerMessage: String? = null,
  val errorMessage: String? = null,
  val notifications: List<GitLabNotification> = emptyList(),
  val inboxFilter: InboxFilter = InboxFilter.ALL
)

class GitLabViewModel(application: Application) : AndroidViewModel(application) {

  private val instanceStore = InstanceStore(application)
  private val repository = GitLabRepository()
  private var loadJob: Job? = null
  private var searchJob: Job? = null

  private val _uiState = MutableStateFlow(
    GitLabUiState(
      activeInstance = instanceStore.getActiveInstance(),
      savedInstances = instanceStore.getInstances()
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
    val current = _uiState.value.notifications.find { it.id == id } ?: return
    if (!current.isUnread) {
      _uiState.update { state ->
        state.copy(notifications = state.notifications.map { if (it.id == id) it.copy(isUnread = true) else it })
      }
      return
    }
    val instance = _uiState.value.activeInstance ?: return
    val todoId = id.toLongOrNull() ?: return
    viewModelScope.launch {
      repository.markTodoDone(instance, todoId).fold(
        onSuccess = {
          _uiState.update { state ->
            state.copy(notifications = state.notifications.map { if (it.id == id) it.copy(isUnread = false) else it })
          }
        },
        onFailure = { error -> showBanner(error.message) }
      )
    }
  }

  fun markAllNotificationsAsRead() {
    val instance = _uiState.value.activeInstance ?: return
    viewModelScope.launch {
      repository.markAllTodosDone(instance).fold(
        onSuccess = {
          _uiState.update { state ->
            state.copy(notifications = state.notifications.map { it.copy(isUnread = false) })
          }
        },
        onFailure = { error -> showBanner(error.message) }
      )
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
      is ScreenDestination.IssueDetail -> {
        _uiState.update { it.copy(selectedIssue = destination.issue, issueNotes = emptyList(), isNotesLoading = true) }
        loadIssueNotes(destination.issue)
      }
      ScreenDestination.MainTabs,
      ScreenDestination.UserProfile,
      ScreenDestination.Instances,
      ScreenDestination.IssuesList,
      ScreenDestination.MergeRequestsList,
      ScreenDestination.PipelinesList -> Unit
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
        val projectId = currentDest.issue.projectId
        _uiState.update {
          it.copy(destination = if (projectId > 0) ScreenDestination.ProjectDetail(projectId) else ScreenDestination.IssuesList)
        }
        true
      }
      is ScreenDestination.MergeRequestDetail -> {
        val projectId = currentDest.mr.projectId
        _uiState.update {
          it.copy(destination = if (projectId > 0) ScreenDestination.ProjectDetail(projectId) else ScreenDestination.MergeRequestsList)
        }
        true
      }
      ScreenDestination.UserProfile,
      ScreenDestination.Instances,
      ScreenDestination.IssuesList,
      ScreenDestination.MergeRequestsList,
      ScreenDestination.PipelinesList -> {
        _uiState.update { it.copy(destination = ScreenDestination.MainTabs) }
        true
      }
      is ScreenDestination.ProjectDetail -> {
        if (_uiState.value.currentTreePath.isNotEmpty()) {
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
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      delay(350)
      val instance = _uiState.value.activeInstance ?: return@launch
      repository.getProjects(instance, query).fold(
        onSuccess = { projects -> _uiState.update { it.copy(projects = projects) } },
        onFailure = { error -> showBanner(error.message) }
      )
    }
  }

  fun switchInstance(instanceId: String) {
    instanceStore.setActiveInstance(instanceId)
    val active = instanceStore.getActiveInstance()
    _uiState.update {
      it.copy(
        activeInstance = active,
        savedInstances = instanceStore.getInstances(),
        bannerMessage = active?.let { instance -> "Switched to ${instance.name}" },
        destination = ScreenDestination.MainTabs,
        projects = emptyList(),
        issues = emptyList(),
        mergeRequests = emptyList(),
        notifications = emptyList(),
        groups = emptyList(),
        projectPipelines = emptyList()
      )
    }
    loadAllData()
  }

  fun addCustomInstance(name: String, url: String, token: String, makeActive: Boolean) {
    viewModelScope.launch {
      val trimmedUrl = url.trim()
      val host = trimmedUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
      val draft = GitLabInstance(
        name = name.trim().ifBlank { host.substringBefore('/') },
        url = trimmedUrl,
        token = token.trim(),
        isActive = makeActive,
        isCustom = !host.startsWith("gitlab.com") && !host.startsWith("www.gitlab.com")
      )
      repository.testConnection(draft).fold(
        onSuccess = { probe ->
          instanceStore.addInstance(
            draft.copy(
              username = probe.username,
              version = probe.version,
              statusOk = true
            )
          )
          _uiState.update {
            it.copy(
              savedInstances = instanceStore.getInstances(),
              activeInstance = instanceStore.getActiveInstance(),
              bannerMessage = "Connected to ${draft.name}"
            )
          }
          if (makeActive) loadAllData()
        },
        onFailure = { error -> showBanner(error.message ?: "Couldn't connect to ${draft.name}") }
      )
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
      _uiState.update { it.copy(connectionTestState = ConnectionTestState(isTesting = true)) }
      val tempInstance = GitLabInstance(name = "Test", url = url, token = token)
      repository.testConnection(tempInstance).fold(
        onSuccess = { probe ->
          val who = probe.username.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "public access"
          val version = probe.version.takeIf { it.isNotBlank() }?.let { "GitLab $it" } ?: "GitLab"
          _uiState.update {
            it.copy(
              connectionTestState = ConnectionTestState(
                isTesting = false,
                isSuccess = true,
                message = "Connected to $version as $who",
                version = probe.version.takeIf { value -> value.isNotBlank() }
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
                message = error.message ?: "Connection failed"
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
    val instance = _uiState.value.activeInstance ?: return
    val starredNow = _uiState.value.projects.find { it.id == projectId }?.isStarred
      ?: _uiState.value.selectedProject?.takeIf { it.id == projectId }?.isStarred
      ?: false
    viewModelScope.launch {
      repository.setProjectStarred(instance, projectId, starred = !starredNow).fold(
        onSuccess = { updated ->
          _uiState.update { state ->
            state.copy(
              projects = state.projects.map { if (it.id == projectId) updated else it },
              selectedProject = if (state.selectedProject?.id == projectId) updated else state.selectedProject
            )
          }
        },
        onFailure = { error -> showBanner(error.message) }
      )
    }
  }

  fun createProject(name: String, description: String, visibility: String) {
    val instance = _uiState.value.activeInstance
    if (instance == null) {
      showBanner("Connect a GitLab server first")
      return
    }
    viewModelScope.launch {
      repository.createProject(instance, name, description, visibility).fold(
        onSuccess = { project ->
          _uiState.update { state ->
            state.copy(
              projects = listOf(project) + state.projects.filterNot { it.id == project.id },
              bannerMessage = "Project ${project.name} created"
            )
          }
        },
        onFailure = { error -> showBanner(error.message) }
      )
    }
  }

  fun createNewIssue(projectId: Long, title: String, description: String?, labels: String?) {
    val instance = _uiState.value.activeInstance
    if (instance == null) {
      showBanner("Connect a GitLab server first")
      return
    }
    viewModelScope.launch {
      repository.createIssue(instance, projectId, title, description, labels).fold(
        onSuccess = { issue ->
          _uiState.update { state ->
            state.copy(
              issues = listOf(issue) + state.issues,
              projectIssues = if (state.selectedProject?.id == projectId) listOf(issue) + state.projectIssues else state.projectIssues,
              bannerMessage = "Issue #${issue.iid} created"
            )
          }
        },
        onFailure = { error -> showBanner(error.message) }
      )
    }
  }

  fun postIssueNote(issue: GitLabIssue, body: String) {
    val instance = _uiState.value.activeInstance ?: return
    viewModelScope.launch {
      repository.createIssueNote(instance, issue.projectId, issue.iid, body).fold(
        onSuccess = { note ->
          _uiState.update { state -> state.copy(issueNotes = state.issueNotes + note) }
        },
        onFailure = { error -> showBanner(error.message) }
      )
    }
  }

  fun setUserStatus(message: String) {
    val instance = _uiState.value.activeInstance ?: return
    viewModelScope.launch {
      repository.setUserStatus(instance, message).fold(
        onSuccess = { status ->
          _uiState.update { it.copy(userStatus = status, bannerMessage = "Status updated") }
        },
        onFailure = { error -> showBanner(error.message) }
      )
    }
  }

  fun retryPipeline(pipeline: GitLabPipeline) {
    val instance = _uiState.value.activeInstance ?: return
    viewModelScope.launch {
      repository.retryPipeline(instance, pipeline.projectId, pipeline.id).fold(
        onSuccess = { created ->
          _uiState.update { state ->
            state.copy(
              projectPipelines = listOf(created) + state.projectPipelines.filterNot { it.id == created.id },
              bannerMessage = "Pipeline #${created.id} restarted"
            )
          }
        },
        onFailure = { error -> showBanner(error.message) }
      )
    }
  }

  fun loadAllData() {
    loadJob?.cancel()
    loadJob = viewModelScope.launch {
      val instance = _uiState.value.activeInstance
      if (instance == null) {
        _uiState.update {
          it.copy(
            isLoading = false,
            projects = emptyList(),
            groups = emptyList(),
            issues = emptyList(),
            mergeRequests = emptyList(),
            projectPipelines = emptyList(),
            notifications = emptyList(),
            currentUser = null,
            userStatus = null,
            achievements = emptyList(),
            profileReadme = null,
            errorMessage = null
          )
        }
        return@launch
      }
      _uiState.update { it.copy(isLoading = true) }
      val failures = mutableListOf<Throwable>()
      val search = _uiState.value.searchQuery
      val projects = repository.getProjects(instance, search).orEmpty(failures)
      val issues = repository.getMyIssues(instance).orEmpty(failures)
      val mergeRequests = repository.getMyMergeRequests(instance).orEmpty(failures)
      val groups = repository.getGroups(instance).orEmpty(failures)
      val todos = repository.getTodos(instance).orEmpty(failures)
      val user = repository.getCurrentUser(instance).getOrElse { error ->
        failures += error
        null
      }
      val pipelines = if (projects.isEmpty()) {
        emptyList()
      } else {
        val listed = coroutineScope {
          projects.take(3).map { project ->
            async { repository.getPipelines(instance, project.id).getOrDefault(emptyList()) }
          }.awaitAll().flatten()
        }.sortedByDescending { it.id }.take(20)
        repository.attachJobs(instance, listed, limit = 6)
      }
      val status = if (user != null) repository.getUserStatus(instance).getOrNull() else null
      val achievements = if (user != null) repository.getAchievements(instance, user.id).getOrDefault(emptyList()) else emptyList()
      val profileReadme = if (user != null) repository.getProfileReadme(instance, user.username).getOrNull() else null
      if (user != null || projects.isNotEmpty()) {
        val version = if (instance.version.isBlank() && user != null) {
          repository.getVersion(instance).getOrNull().orEmpty().ifBlank { instance.version }
        } else {
          instance.version
        }
        instanceStore.updateInstance(
          instance.copy(
            username = user?.username?.takeIf { it.isNotBlank() } ?: instance.username,
            version = version,
            statusOk = true
          )
        )
      } else if (failures.any { !it.isAuthError() || instance.token.isNotBlank() }) {
        instanceStore.updateInstance(instance.copy(statusOk = false))
      }
      val message = summarizeFailures(failures, instance)
      _uiState.update { state ->
        val clearStaleError = message == null && state.errorMessage != null && state.bannerMessage == state.errorMessage
        state.copy(
          isLoading = false,
          activeInstance = instanceStore.getActiveInstance(),
          savedInstances = instanceStore.getInstances(),
          projects = projects,
          groups = groups,
          issues = issues,
          mergeRequests = mergeRequests,
          projectPipelines = pipelines,
          notifications = todos.map { it.toNotification() },
          currentUser = user,
          userStatus = status,
          achievements = achievements,
          profileReadme = profileReadme,
          errorMessage = message,
          bannerMessage = message ?: if (clearStaleError) null else state.bannerMessage
        )
      }
    }
  }

  fun loadProjectDetails(projectId: Long) {
    viewModelScope.launch {
      val instance = _uiState.value.activeInstance ?: return@launch
      val cached = _uiState.value.projects.find { it.id == projectId }
      _uiState.update {
        it.copy(
          selectedProject = cached,
          currentTreePath = "",
          projectTree = emptyList(),
          projectIssues = emptyList(),
          projectMergeRequests = emptyList(),
          projectReadme = null
        )
      }
      val project = repository.getProject(instance, projectId).getOrElse { error ->
        showBanner(error.message)
        _uiState.update { it.copy(destination = ScreenDestination.MainTabs) }
        return@launch
      }
      _uiState.update { it.copy(selectedProject = project, currentTreePath = "") }
      val ref = project.defaultBranch
      coroutineScope {
        val tree = async { repository.getRepositoryTree(instance, projectId, null, ref) }
        val commits = async { repository.getRepositoryCommits(instance, projectId, ref) }
        val pipelines = async { repository.getPipelines(instance, projectId) }
        val issues = async { repository.getProjectIssues(instance, projectId) }
        val mergeRequests = async { repository.getProjectMergeRequests(instance, projectId) }
        val readme = async { repository.getReadme(instance, projectId, ref) }
        val pipelineList = pipelines.await().getOrElse { error ->
          showBanner(error.message)
          emptyList()
        }
        val failures = listOf(tree.await(), commits.await(), issues.await(), mergeRequests.await(), readme.await())
          .mapNotNull { it.exceptionOrNull()?.message }
        if (failures.isNotEmpty()) showBanner(failures.first())
        _uiState.update {
          it.copy(
            projectTree = tree.await().getOrDefault(emptyList()),
            projectCommits = commits.await().getOrDefault(emptyList()),
            projectPipelines = repository.attachJobs(instance, pipelineList, limit = 8),
            projectIssues = issues.await().getOrDefault(emptyList()),
            projectMergeRequests = mergeRequests.await().getOrDefault(emptyList()),
            projectReadme = readme.await().getOrNull()
          )
        }
      }
    }
  }

  fun loadRepositoryTree(projectId: Long, path: String) {
    viewModelScope.launch {
      val instance = _uiState.value.activeInstance ?: return@launch
      val ref = _uiState.value.selectedProject?.defaultBranch
      repository.getRepositoryTree(instance, projectId, path, ref).fold(
        onSuccess = { tree -> _uiState.update { it.copy(projectTree = tree, currentTreePath = path) } },
        onFailure = { error ->
          _uiState.update { it.copy(projectTree = emptyList(), currentTreePath = path, bannerMessage = error.message) }
        }
      )
    }
  }

  private fun loadFileContent(projectId: Long, filePath: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isFileLoading = true, selectedFilePath = filePath, selectedFileContent = null, fileError = null) }
      val instance = _uiState.value.activeInstance
      if (instance == null) {
        _uiState.update { it.copy(isFileLoading = false, fileError = "Connect a GitLab server first") }
        return@launch
      }
      val ref = _uiState.value.selectedProject?.takeIf { it.id == projectId }?.defaultBranch
      repository.getFileContent(instance, projectId, filePath, ref).fold(
        onSuccess = { content ->
          _uiState.update { it.copy(isFileLoading = false, selectedFileContent = content, fileError = null) }
        },
        onFailure = { error ->
          _uiState.update { it.copy(isFileLoading = false, selectedFileContent = null, fileError = error.message) }
        }
      )
    }
  }

  private fun loadMergeRequestDetails(mr: GitLabMergeRequest) {
    viewModelScope.launch {
      _uiState.update { it.copy(selectedMr = mr, isDiffLoading = true, selectedMrDiffs = emptyList()) }
      val instance = _uiState.value.activeInstance
      if (instance == null) {
        _uiState.update { it.copy(isDiffLoading = false) }
        return@launch
      }
      repository.getMergeRequestDiffs(instance, mr.projectId, mr.iid).fold(
        onSuccess = { diffs -> _uiState.update { it.copy(isDiffLoading = false, selectedMrDiffs = diffs) } },
        onFailure = { error ->
          _uiState.update { it.copy(isDiffLoading = false, selectedMrDiffs = emptyList(), bannerMessage = error.message) }
        }
      )
    }
  }

  private fun loadIssueNotes(issue: GitLabIssue) {
    viewModelScope.launch {
      val instance = _uiState.value.activeInstance
      if (instance == null) {
        _uiState.update { it.copy(isNotesLoading = false, issueNotes = emptyList()) }
        return@launch
      }
      repository.getIssueNotes(instance, issue.projectId, issue.iid).fold(
        onSuccess = { notes -> _uiState.update { it.copy(isNotesLoading = false, issueNotes = notes) } },
        onFailure = { error ->
          _uiState.update { it.copy(isNotesLoading = false, issueNotes = emptyList(), bannerMessage = error.message) }
        }
      )
    }
  }

  private fun showBanner(message: String?) {
    if (message.isNullOrBlank()) return
    _uiState.update { it.copy(bannerMessage = message, errorMessage = message) }
  }

  private fun summarizeFailures(failures: List<Throwable>, instance: GitLabInstance): String? {
    val unexpected = failures.filterNot { instance.token.isBlank() && it.isAuthError() }
    if (unexpected.isNotEmpty()) return unexpected.first().message ?: "Request failed"
    if (instance.token.isBlank()) {
      return "Add a personal access token to load your issues, merge requests, and to-dos."
    }
    return null
  }

  private fun <T> Result<List<T>>.orEmpty(failures: MutableList<Throwable>): List<T> =
    getOrElse { error ->
      failures += error
      emptyList()
    }
}

private fun GitLabTodo.toNotification(): GitLabNotification {
  val type = when {
    actionName == "build_failed" -> NotificationType.PIPELINE
    actionName == "mentioned" || actionName == "directly_addressed" -> NotificationType.MENTION
    actionName == "marked" -> NotificationType.DISCUSSION
    targetType == "MergeRequest" -> NotificationType.MERGE_REQUEST
    targetType == "Issue" -> NotificationType.ISSUE
    else -> NotificationType.DISCUSSION
  }
  val badge = when (actionName) {
    "assigned" -> "Assigned"
    "mentioned", "directly_addressed" -> "Mentioned"
    "review_requested" -> "Review requested"
    "approval_required" -> "Approval required"
    "build_failed" -> "Failed"
    "marked" -> "To-do"
    "unmergeable" -> "Conflicts"
    else -> actionName.replace('_', ' ').replaceFirstChar { char ->
      if (char.isLowerCase()) char.titlecase() else char.toString()
    }
  }
  val iid = target?.iid ?: 0L
  val prefix = if (targetType == "MergeRequest") "!" else "#"
  val targetTitle = target?.title?.takeIf { it.isNotBlank() }
  val title = when {
    targetTitle != null && iid > 0 -> "$prefix$iid: $targetTitle"
    targetTitle != null -> targetTitle
    else -> body?.lineSequence()?.firstOrNull()?.trim()?.take(140)?.ifBlank { null } ?: badge
  }
  return GitLabNotification(
    id = id.toString(),
    type = type,
    title = title,
    projectPath = project?.pathWithNamespace.orEmpty(),
    authorName = author.name.ifBlank { author.username },
    authorUsername = author.username,
    timestamp = formatGitLabTime(createdAt),
    isUnread = state == "pending",
    targetId = target?.projectId,
    subtitle = body?.lineSequence()?.firstOrNull()?.trim()?.take(180).orEmpty(),
    statusBadge = badge.ifBlank { null }
  )
}
