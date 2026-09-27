package com.aistudio.gitlab.ui.viewmodel

import android.app.Application
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.gitlab.data.api.GitLabApiClient
import com.aistudio.gitlab.data.api.GitLabApiException
import com.aistudio.gitlab.data.api.formatGitLabTime
import com.aistudio.gitlab.data.auth.GitLabAuth
import com.aistudio.gitlab.data.auth.GitLabOAuth
import com.aistudio.gitlab.data.auth.GitLabPasswordLogin
import com.aistudio.gitlab.data.auth.PasswordLoginOutcome
import com.aistudio.gitlab.data.model.GitLabGroup
import com.aistudio.gitlab.data.model.GitLabInstance
import com.aistudio.gitlab.data.model.GitLabIssue
import com.aistudio.gitlab.data.model.GitLabMergeRequest
import com.aistudio.gitlab.data.model.GitLabPipeline
import com.aistudio.gitlab.data.model.GitLabProject
import com.aistudio.gitlab.data.model.GitLabTodo
import com.aistudio.gitlab.data.repository.GitLabRepository
import com.aistudio.gitlab.data.storage.InstanceStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Single owner of app state.
 *
 * Everything that talks to a server runs in [session], a scope that is cancelled and replaced
 * whenever the active server changes, so a slow response from the previous server can never land
 * in the new one's state. Storage (Keystore + SharedPreferences) only runs on [Dispatchers.IO].
 * State updates never suspend: values are computed first, then written with a pure `update {}`.
 */
class GitLabViewModel(application: Application) : AndroidViewModel(application) {

  private val store = InstanceStore(application)
  private val repository = GitLabRepository()

  private val _uiState = MutableStateFlow(GitLabUiState())
  val uiState: StateFlow<GitLabUiState> = _uiState.asStateFlow()

  private val _messages = Channel<UiMessage>(Channel.BUFFERED)
  val messages: Flow<UiMessage> = _messages.receiveAsFlow()

  /** URLs to open outside the app (browser), e.g. a link the app has no screen for. */
  private val _externalLinks = Channel<String>(Channel.BUFFERED)
  val externalLinks: Flow<String> = _externalLinks.receiveAsFlow()

  private var session: CoroutineScope = newSession()
  private var dashboardJob: Job? = null
  private var searchJob: Job? = null
  private var issueJob: Job? = null

  private val host = object : SessionHost {
    override val repository: GitLabRepository get() = this@GitLabViewModel.repository
    override val session: CoroutineScope get() = this@GitLabViewModel.session
    override val state: MutableStateFlow<GitLabUiState> get() = _uiState
    override suspend fun <T> api(block: suspend (GitLabInstance) -> Result<T>): Result<T> = this@GitLabViewModel.api(block)
    override fun notify(text: String) {
      this@GitLabViewModel.notify(text)
    }
    override fun notifyError(text: String) {
      this@GitLabViewModel.notifyError(text)
    }
    override fun navigateTo(destination: ScreenDestination) {
      this@GitLabViewModel.navigateTo(destination)
    }
    override fun openExternal(url: String) {
      _externalLinks.trySend(url)
    }
  }

  /** Project page, lists, code browser, files and commits. */
  val code = ProjectFeature(host)

  /** Merge request detail and actions. */
  val review = ReviewFeature(host) { projectId -> code.ensureLabels(projectId) }

  /** Pipeline and job detail. */
  val ci = PipelineFeature(host)

  /** Groups, people, search and in-app links. */
  val explore = ExploreFeature(host)

  private val refreshMutex = Mutex()
  private var passwordLogin: GitLabPasswordLogin? = null
  private var pendingServerName: String = ""
  private var pendingServerUrl: String = ""

  init {
    bootstrap()
  }

  private fun newSession() = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext.job))

  private fun resetSession() {
    session.cancel()
    session = newSession()
    dashboardJob = null
    searchJob = null
    issueJob = null
    code.reset()
    review.reset()
    ci.reset()
    explore.reset()
  }

  private fun bootstrap() {
    viewModelScope.launch {
      val loaded = runStorage { Triple(store.getInstances(), store.getActiveInstance(), store.getGitLabComClientId()) }
      loaded.fold(
        onSuccess = { (instances, active, clientId) ->
          _uiState.update {
            it.copy(bootstrapped = true, savedInstances = instances, activeInstance = active, gitLabComClientId = clientId, storageError = null)
          }
          refreshDashboard()
        },
        onFailure = { error ->
          _uiState.update { it.copy(bootstrapped = true, storageError = error.message ?: "Unable to read saved servers.") }
        }
      )
    }
  }

  fun retryBootstrap() {
    _uiState.update { it.copy(bootstrapped = false, storageError = null) }
    bootstrap()
  }

  // -------------------------------------------------------------------------------------------
  // Navigation
  // -------------------------------------------------------------------------------------------

  fun switchTab(tab: BottomTab) {
    _uiState.update { it.copy(currentTab = tab, backStack = emptyList()) }
  }

  fun navigateTo(destination: ScreenDestination) {
    if (destination == ScreenDestination.MainTabs) {
      _uiState.update { it.copy(backStack = emptyList()) }
      return
    }
    if (_uiState.value.destination == destination) return
    _uiState.update { it.copy(backStack = it.backStack + destination) }
    ci.onDestinationChanged(destination)
    load(destination, force = true)
  }

  /** Returns false when already at the root so the activity can finish. */
  fun navigateBack(): Boolean {
    val state = _uiState.value
    val current = state.destination
    if (current is ScreenDestination.ProjectCode && state.project.treePath.isNotEmpty()) {
      code.openPath(state.project.treePath.substringBeforeLast('/', ""))
      return true
    }
    if (state.backStack.isEmpty()) return false
    val remaining = state.backStack.dropLast(1)
    _uiState.update { it.copy(backStack = remaining) }
    val next = remaining.lastOrNull() ?: ScreenDestination.MainTabs
    ci.onDestinationChanged(next)
    load(next, force = false)
    return true
  }

  /** Loads what a destination shows, unless it already holds that entity. */
  private fun load(destination: ScreenDestination, force: Boolean) {
    val state = _uiState.value
    val projectLoaded = { id: Long -> state.project.projectId == id }
    when (destination) {
      is ScreenDestination.ProjectDetail ->
        if (force || !projectLoaded(destination.projectId)) code.open(destination.projectId)
      is ScreenDestination.ProjectCode -> {
        if (!projectLoaded(destination.projectId)) code.open(destination.projectId)
        code.ensureTree()
      }
      is ScreenDestination.ProjectSectionList -> {
        if (!projectLoaded(destination.projectId)) code.open(destination.projectId)
        code.loadSection(destination.section)
      }
      is ScreenDestination.ReleaseDetail -> {
        if (!projectLoaded(destination.projectId)) code.open(destination.projectId)
        code.loadSection(ProjectSection.RELEASES)
      }
      is ScreenDestination.FileViewer ->
        if (force || state.file.projectId != destination.projectId || state.file.path != destination.filePath || state.file.ref != destination.ref) {
          code.loadFile(destination.projectId, destination.filePath, destination.ref)
        }
      is ScreenDestination.CommitDetail ->
        if (force || state.commitDetail.projectId != destination.projectId || state.commitDetail.sha != destination.sha) {
          code.loadCommit(destination.projectId, destination.sha)
        }
      is ScreenDestination.PipelineDetail ->
        if (force || state.pipelineDetail.pipelineId != destination.pipelineId || state.pipelineDetail.pipeline.data?.status in setOf("running", "pending", "created")) {
          ci.loadPipeline(destination.projectId, destination.pipelineId)
        }
      is ScreenDestination.JobDetail ->
        if (force || state.jobDetail.jobId != destination.jobId || state.jobDetail.job.data?.status in setOf("running", "pending", "created")) {
          ci.loadJob(destination.projectId, destination.jobId)
        }
      is ScreenDestination.UserDetail ->
        if (force || state.userDetail.userId != destination.userId || state.userDetail.username != destination.username) {
          explore.loadUser(destination.userId, destination.username)
        }
      is ScreenDestination.IssueDetail ->
        if (force || state.issueDetail.projectId != destination.projectId || state.issueDetail.iid != destination.iid) {
          openIssue(destination.projectId, destination.iid)
        }
      is ScreenDestination.MergeRequestDetail ->
        if (force || state.mergeRequestDetail.projectId != destination.projectId || state.mergeRequestDetail.iid != destination.iid) {
          review.load(destination.projectId, destination.iid)
        }
      is ScreenDestination.GroupProjects ->
        if (force || state.group.group?.id != destination.group.id) explore.loadGroup(destination.group)
      else -> Unit
    }
  }

  fun openIssue(issue: GitLabIssue) {
    seedIssue(issue)
    navigateToWithoutLoad(ScreenDestination.IssueDetail(issue.projectId, issue.iid))
    openIssue(issue.projectId, issue.iid, seeded = true)
  }

  fun openMergeRequest(mr: GitLabMergeRequest) = review.open(mr)

  private fun navigateToWithoutLoad(destination: ScreenDestination) {
    if (_uiState.value.destination == destination) return
    _uiState.update { it.copy(backStack = it.backStack + destination) }
  }

  // -------------------------------------------------------------------------------------------
  // Dashboard
  // -------------------------------------------------------------------------------------------

  /** Loads every home section in parallel; each section publishes as soon as it arrives. */
  fun refreshDashboard(userInitiated: Boolean = false) {
    dashboardJob?.cancel()
    val instance = _uiState.value.activeInstance
    if (instance == null) {
      _uiState.update { clearedSessionState(it) }
      return
    }
    _uiState.update {
      it.copy(
        refreshing = userInitiated,
        currentUser = it.currentUser.start(),
        projects = it.projects.start(),
        groups = it.groups.start(),
        issues = it.issues.start(),
        mergeRequests = it.mergeRequests.start(),
        pipelines = it.pipelines.start(),
        notifications = it.notifications.start(),
        activity = if (instance.token.isBlank()) it.activity.success(emptyList()) else it.activity.start(),
      )
    }
    dashboardJob = session.launch {
      var reachedServer = false
      var authFailed = false
      coroutineScope {
        launch {
          api { repository.getCurrentUser(it) }.fold(
            onSuccess = { user ->
              reachedServer = true
              _uiState.update { s -> s.copy(currentUser = s.currentUser.success(user)) }
              if (user != null) loadProfileExtras(user.username)
            },
            onFailure = { error ->
              authFailed = authFailed || error.isUnauthorized()
              _uiState.update { s -> s.copy(currentUser = s.currentUser.failure(error.userMessage())) }
            }
          )
        }
        launch {
          val projects = api { repository.getProjects(it) }
          projects.fold(
            onSuccess = { list ->
              reachedServer = true
              _uiState.update { s -> s.copy(projects = s.projects.success(list)) }
            },
            onFailure = { error -> _uiState.update { s -> s.copy(projects = s.projects.failure(error.userMessage())) } }
          )
          // Anonymous sessions have no "my pipelines"; skip the fan-out over popular projects.
          val pipelines = if (instance.token.isBlank()) Result.success(emptyList())
          else projects.getOrNull()?.let { list -> api { repository.getRecentPipelines(it, list) } }
            ?: Result.failure(projects.exceptionOrNull() ?: IllegalStateException())
          pipelines.fold(
            onSuccess = { list -> _uiState.update { s -> s.copy(pipelines = s.pipelines.success(list)) } },
            onFailure = { error -> _uiState.update { s -> s.copy(pipelines = s.pipelines.failure(error.userMessage())) } }
          )
        }
        launch {
          api { repository.getMyIssues(it) }.fold(
            onSuccess = { list -> _uiState.update { s -> s.copy(issues = s.issues.success(list)) } },
            onFailure = { error -> _uiState.update { s -> s.copy(issues = s.issues.failure(error.userMessage())) } }
          )
        }
        launch {
          api { repository.getMyMergeRequests(it) }.fold(
            onSuccess = { list -> _uiState.update { s -> s.copy(mergeRequests = s.mergeRequests.success(list)) } },
            onFailure = { error -> _uiState.update { s -> s.copy(mergeRequests = s.mergeRequests.failure(error.userMessage())) } }
          )
        }
        launch {
          api { repository.getTodos(it) }.fold(
            onSuccess = { list ->
              val mapped = list.map { todo -> todo.toNotification() }
              _uiState.update { s -> s.copy(notifications = s.notifications.success(mapped)) }
            },
            onFailure = { error -> _uiState.update { s -> s.copy(notifications = s.notifications.failure(error.userMessage())) } }
          )
        }
        launch {
          api { repository.getMyEvents(it) }.fold(
            onSuccess = { list -> _uiState.update { s -> s.copy(activity = s.activity.success(list)) } },
            onFailure = { error -> _uiState.update { s -> s.copy(activity = s.activity.failure(error.userMessage())) } }
          )
        }
        launch {
          api { repository.getGroups(it) }.fold(
            onSuccess = { list -> _uiState.update { s -> s.copy(groups = s.groups.success(list)) } },
            onFailure = { error -> _uiState.update { s -> s.copy(groups = s.groups.failure(error.userMessage())) } }
          )
        }
      }
      _uiState.update { it.copy(refreshing = false) }
      val user = _uiState.value.currentUser.data
      persistServerMeta(instance.id, username = user?.username, reachable = reachedServer, authFailed = authFailed)
    }
  }

  private fun loadProfileExtras(username: String) {
    session.launch {
      val status = api { repository.getUserStatus(it) }.getOrNull()
      val readme = api { repository.getProfileReadme(it, username) }.getOrNull()
      _uiState.update { it.copy(userStatus = status, profileReadme = readme) }
    }
  }

  /** Patches only metadata on the freshly stored record, so a newer token is never overwritten. */
  private suspend fun persistServerMeta(instanceId: String, username: String?, reachable: Boolean, authFailed: Boolean) {
    val version = if (reachable) {
      val current = _uiState.value.activeInstance
      if (current?.id == instanceId && current.version.isBlank()) api { repository.getVersion(it) }.getOrNull() else null
    } else null
    val updated = runStorage {
      val latest = store.getInstances().firstOrNull { it.id == instanceId } ?: return@runStorage null
      val patched = latest.copy(
        username = username?.takeIf { it.isNotBlank() } ?: latest.username,
        version = version?.takeIf { it.isNotBlank() } ?: latest.version,
        statusOk = reachable && !authFailed
      )
      if (patched != latest) store.updateInstance(patched)
      store.getInstances() to store.getActiveInstance()
    }.getOrNull() ?: return
    publishInstances(updated.first, updated.second)
    if (authFailed) _uiState.update { it.copy(sessionExpired = true) }
  }

  private fun clearedSessionState(state: GitLabUiState) = state.copy(
    refreshing = false,
    sessionExpired = false,
    currentUser = Loadable(null),
    userStatus = null,
    profileReadme = null,
    projects = emptyFeed(),
    groups = emptyFeed(),
    issues = emptyFeed(),
    mergeRequests = emptyFeed(),
    pipelines = emptyFeed(),
    notifications = emptyFeed(),
    searchQuery = "",
    searchResults = emptyFeed(),
    group = GroupDetailState(),
    activity = emptyFeed(),
    commitDetail = CommitDetailState(),
    pipelineDetail = PipelineDetailState(),
    jobDetail = JobDetailState(),
    userDetail = UserDetailState(),
    search = SearchState(),
    labels = emptyMap(),
    pendingStars = emptySet(),
    project = ProjectDetailState(),
    file = FileState(),
    issueDetail = IssueDetailState(),
    mergeRequestDetail = MergeRequestDetailState(),
  )

  // -------------------------------------------------------------------------------------------
  // Servers and sign-in
  // -------------------------------------------------------------------------------------------

  fun switchInstance(instanceId: String) {
    if (_uiState.value.activeInstance?.id == instanceId) return
    resetSession()
    _uiState.update { clearedSessionState(it).copy(backStack = emptyList(), currentTab = BottomTab.HOME) }
    viewModelScope.launch {
      runStorage {
        store.setActiveInstance(instanceId)
        store.getInstances() to store.getActiveInstance()
      }.fold(
        onSuccess = { (instances, active) ->
          publishInstances(instances, active)
          active?.let { notify("Switched to ${it.name}") }
          refreshDashboard()
        },
        onFailure = { error -> notifyError(error.message ?: "Unable to switch servers.") }
      )
    }
  }

  fun deleteInstance(instanceId: String) {
    val wasActive = _uiState.value.activeInstance?.id == instanceId
    if (wasActive) {
      resetSession()
      _uiState.update { clearedSessionState(it) }
    }
    GitLabApiClient.forget(instanceId)
    viewModelScope.launch {
      runStorage {
        store.deleteInstance(instanceId)
        store.getInstances() to store.getActiveInstance()
      }.fold(
        onSuccess = { (instances, active) ->
          publishInstances(instances, active)
          notify("Server removed")
          if (wasActive) refreshDashboard()
        },
        onFailure = { error -> notifyError(error.message ?: "Unable to remove the server.") }
      )
    }
  }

  private fun publishInstances(instances: List<GitLabInstance>, active: GitLabInstance?) {
    _uiState.update { it.copy(savedInstances = instances, activeInstance = active) }
  }

  fun clearLoginState() {
    passwordLogin = null
    pendingServerName = ""
    pendingServerUrl = ""
    _uiState.update { it.copy(loginState = LoginState()) }
  }

  fun signInWithPassword(name: String, url: String, username: String, password: String) {
    if (_uiState.value.loginState.inProgress) return
    pendingServerName = name
    pendingServerUrl = url
    viewModelScope.launch {
      _uiState.update { it.copy(loginState = LoginState(inProgress = true)) }
      val login = GitLabPasswordLogin()
      passwordLogin = login
      val outcome = withContext(Dispatchers.IO) { login.signIn(url, username, password) }
      handlePasswordOutcome(outcome, duringTwoFactor = false)
    }
  }

  fun submitTwoFactor(code: String) {
    val login = passwordLogin
    if (login == null || pendingServerUrl.isBlank()) {
      _uiState.update { it.copy(loginState = LoginState(isError = true, message = "Sign in again, then enter the code.")) }
      return
    }
    if (_uiState.value.loginState.inProgress) return
    viewModelScope.launch {
      _uiState.update { it.copy(loginState = LoginState(inProgress = true, needsTwoFactor = true)) }
      val outcome = withContext(Dispatchers.IO) { login.submitOtp(code) }
      handlePasswordOutcome(outcome, duringTwoFactor = true)
    }
  }

  fun startOAuthSignIn(name: String, url: String, clientId: String) {
    val id = clientId.trim()
    if (id.isEmpty()) {
      _uiState.update { it.copy(loginState = LoginState(isError = true, message = "Enter the OAuth application ID from your GitLab server.")) }
      return
    }
    val pending = try {
      GitLabOAuth.begin(name, id, url)
    } catch (error: IllegalArgumentException) {
      _uiState.update { it.copy(loginState = LoginState(isError = true, message = error.message ?: "Check the server address.")) }
      return
    }
    viewModelScope.launch {
      val saved = runStorage {
        store.setGitLabComClientId(id)
        GitLabOAuth.savePending(getApplication(), pending)
      }
      if (saved.isFailure) {
        _uiState.update { it.copy(loginState = LoginState(isError = true, message = "Unable to start sign-in. Try again.")) }
        return@launch
      }
      try {
        GitLabOAuth.openBrowser(getApplication(), pending)
        _uiState.update { it.copy(gitLabComClientId = id, loginState = LoginState(awaitingBrowser = true)) }
      } catch (_: ActivityNotFoundException) {
        _uiState.update { it.copy(loginState = LoginState(isError = true, message = "Install or enable a web browser to sign in with OAuth.")) }
      }
    }
  }

  fun signInWithToken(name: String, url: String, token: String) {
    if (token.isBlank()) {
      _uiState.update { it.copy(loginState = LoginState(isError = true, message = "Paste a personal access token with the api scope.")) }
      return
    }
    if (_uiState.value.loginState.inProgress) return
    saveConnectedInstance(name, url, token.trim(), GitLabAuth.KIND_PAT)
  }

  /** Connects without credentials to browse public projects. Nothing can be changed in this mode. */
  fun connectPublic(name: String, url: String) {
    if (_uiState.value.loginState.inProgress) return
    saveConnectedInstance(name, url, token = "", authKind = GitLabAuth.KIND_PUBLIC)
  }

  fun completeOAuthRedirect(uri: Uri) {
    viewModelScope.launch {
      val pending = runStorage { GitLabOAuth.consumePending(getApplication()) }.getOrNull() ?: return@launch
      val error = uri.getQueryParameter("error_description") ?: uri.getQueryParameter("error")
      val state = uri.getQueryParameter("state")
      val code = uri.getQueryParameter("code")
      if (!error.isNullOrBlank()) {
        _uiState.update { it.copy(loginState = LoginState(isError = true, message = error)) }
        notifyError(error)
        return@launch
      }
      if (state != pending.state || code.isNullOrBlank()) {
        val message = "GitLab sign-in didn't finish. Start it again from Add server."
        _uiState.update { it.copy(loginState = LoginState(isError = true, message = message)) }
        notifyError(message)
        return@launch
      }
      _uiState.update { it.copy(loginState = LoginState(inProgress = true)) }
      val tokens = try {
        Result.success(withContext(Dispatchers.IO) { GitLabOAuth.exchange(pending, code) })
      } catch (cancel: CancellationException) {
        throw cancel
      } catch (failure: Exception) {
        Result.failure(failure)
      }
      tokens.fold(
        onSuccess = { set ->
          saveConnectedInstance(
            name = pending.name,
            url = pending.baseUrl,
            token = set.accessToken,
            authKind = GitLabAuth.KIND_OAUTH,
            refreshToken = set.refreshToken,
            tokenExpiresAt = set.expiresAtEpochSec,
            oauthClientId = pending.clientId
          )
        },
        onFailure = { failure ->
          val message = failure.message ?: "GitLab sign-in failed. Try again."
          _uiState.update { it.copy(loginState = LoginState(isError = true, message = message)) }
          notifyError(message)
        }
      )
    }
  }

  private fun handlePasswordOutcome(outcome: PasswordLoginOutcome, duringTwoFactor: Boolean) {
    when (outcome) {
      PasswordLoginOutcome.NeedsTwoFactor -> {
        _uiState.update { it.copy(loginState = LoginState(needsTwoFactor = true)) }
      }
      is PasswordLoginOutcome.Failed -> {
        _uiState.update {
          it.copy(loginState = LoginState(isError = true, message = outcome.message, needsTwoFactor = duringTwoFactor))
        }
      }
      is PasswordLoginOutcome.TokenReady -> {
        saveConnectedInstance(
          name = pendingServerName,
          url = pendingServerUrl,
          token = outcome.token,
          authKind = GitLabAuth.KIND_PASSWORD
        )
      }
    }
  }

  private fun saveConnectedInstance(
    name: String,
    url: String,
    token: String,
    authKind: String,
    refreshToken: String = "",
    tokenExpiresAt: Long = 0L,
    oauthClientId: String = ""
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(loginState = LoginState(inProgress = true)) }
      val trimmedUrl = url.trim()
      if (trimmedUrl.isBlank() || trimmedUrl == "https://") {
        _uiState.update { it.copy(loginState = LoginState(isError = true, message = "Enter your GitLab server address, for example https://gitlab.example.com.")) }
        return@launch
      }
      val official = GitLabAuth.isGitLabCom(trimmedUrl)
      val host = trimmedUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
      val draft = GitLabInstance(
        name = name.trim().ifBlank { if (official) "GitLab.com" else host.substringBefore('/') },
        url = trimmedUrl,
        token = token,
        isActive = true,
        isCustom = !official,
        authKind = authKind,
        refreshToken = refreshToken,
        tokenExpiresAt = tokenExpiresAt,
        oauthClientId = oauthClientId
      )
      repository.testConnection(draft).fold(
        onSuccess = { probe ->
          passwordLogin = null
          val saved = runStorage {
            val added = store.addInstance(draft.copy(username = probe.username, version = probe.version, statusOk = true))
            Triple(added, store.getInstances(), store.getActiveInstance())
          }
          saved.fold(
            onSuccess = { (added, instances, active) ->
              resetSession()
              GitLabApiClient.forget(added.id)
              _uiState.update {
                clearedSessionState(it).copy(
                  savedInstances = instances,
                  activeInstance = active,
                  loginState = LoginState(finished = true),
                  backStack = emptyList(),
                  currentTab = BottomTab.HOME,
                )
              }
              notify(if (added.token.isBlank()) "Connected to ${added.name} without signing in" else "Signed in to ${added.name}")
              refreshDashboard()
            },
            onFailure = { error ->
              _uiState.update { it.copy(loginState = LoginState(isError = true, message = error.message ?: "Unable to save the server.")) }
            }
          )
        },
        onFailure = { error ->
          _uiState.update { it.copy(loginState = LoginState(isError = true, message = error.userMessage())) }
        }
      )
    }
  }

  /**
   * Returns the active server with a valid OAuth token. Refresh runs once at a time and is not
   * cancellable: GitLab refresh tokens are single-use, so an interrupted refresh that the server
   * accepted but we never stored would lock the account out.
   */
  private suspend fun freshInstance(forceRefresh: Boolean = false, staleToken: String? = null): GitLabInstance? {
    val current = _uiState.value.activeInstance ?: return null
    if (current.authKind != GitLabAuth.KIND_OAUTH || current.refreshToken.isBlank()) return current
    val nowSec = System.currentTimeMillis() / 1000
    if (!forceRefresh && current.tokenExpiresAt > nowSec + 60) return current
    return refreshMutex.withLock {
      val latest = _uiState.value.activeInstance?.takeIf { it.id == current.id } ?: return@withLock null
      val stillExpired = latest.tokenExpiresAt <= System.currentTimeMillis() / 1000 + 60
      val alreadyRotated = staleToken != null && latest.token != staleToken
      if (alreadyRotated || (!forceRefresh && !stillExpired)) return@withLock latest
      withContext(NonCancellable) {
        val clientId = latest.oauthClientId.ifBlank { runStorage { store.getGitLabComClientId() }.getOrDefault("") }
        if (clientId.isBlank()) return@withContext latest
        val refreshed = try {
          withContext(Dispatchers.IO) { GitLabOAuth.refresh(latest.url, clientId, latest.refreshToken) }
        } catch (_: Exception) {
          _uiState.update { it.copy(sessionExpired = true) }
          return@withContext latest
        }
        val updated = latest.copy(
          token = refreshed.accessToken,
          refreshToken = refreshed.refreshToken.ifBlank { latest.refreshToken },
          tokenExpiresAt = refreshed.expiresAtEpochSec,
          oauthClientId = clientId
        )
        runStorage { store.updateInstance(updated) }
        _uiState.update { state ->
          state.copy(
            activeInstance = if (state.activeInstance?.id == updated.id) updated else state.activeInstance,
            savedInstances = state.savedInstances.map { if (it.id == updated.id) updated else it },
            sessionExpired = false,
          )
        }
        updated
      }
    }
  }

  /** Runs a request against the active server; on 401 with OAuth, refreshes once and retries. */
  private suspend fun <T> api(block: suspend (GitLabInstance) -> Result<T>): Result<T> {
    val instance = freshInstance() ?: return Result.failure(GitLabApiException(0, "Add a GitLab server first."))
    val first = block(instance).let { result ->
      // Without an account a 401 means "sign in to see this", not an expired session.
      val code = (result.exceptionOrNull() as? GitLabApiException)?.code
      if (instance.token.isBlank() && (code == 401 || code == 403)) {
        Result.failure(GitLabApiException(code, "Sign in to see this. The server only shows it to signed-in users."))
      } else result
    }
    val error = first.exceptionOrNull()
    if (error.isUnauthorized() && instance.authKind == GitLabAuth.KIND_OAUTH && instance.refreshToken.isNotBlank()) {
      val retried = freshInstance(forceRefresh = true, staleToken = instance.token)
      if (retried != null && retried.token != instance.token) return block(retried)
    }
    return first
  }

  private suspend fun <T> runStorage(block: () -> T): Result<T> = withContext(Dispatchers.IO) {
    try {
      Result.success(block())
    } catch (cancel: CancellationException) {
      throw cancel
    } catch (error: Exception) {
      Result.failure(error)
    }
  }

  // -------------------------------------------------------------------------------------------
  // Projects, search, groups
  // -------------------------------------------------------------------------------------------

  fun setSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
    searchJob?.cancel()
    val trimmed = query.trim()
    if (trimmed.isEmpty()) {
      _uiState.update { it.copy(searchResults = emptyFeed()) }
      return
    }
    searchJob = session.launch {
      delay(350)
      _uiState.update { it.copy(searchResults = it.searchResults.start()) }
      api { repository.getProjects(it, trimmed) }.fold(
        onSuccess = { list -> _uiState.update { s -> s.copy(searchResults = s.searchResults.success(list)) } },
        onFailure = { error -> _uiState.update { s -> s.copy(searchResults = s.searchResults.failure(error.userMessage())) } }
      )
    }
  }

  fun toggleProjectStar(projectId: Long) {
    val state = _uiState.value
    if (projectId in state.pendingStars) return
    val known = (state.projects.data + state.searchResults.data + state.group.projects.data + listOfNotNull(state.project.project.data))
      .firstOrNull { it.id == projectId }
    val starred = known?.isStarred ?: false
    _uiState.update { it.copy(pendingStars = it.pendingStars + projectId) }
    session.launch {
      val result = api { repository.setProjectStarred(it, projectId, starred = !starred) }
      _uiState.update { it.copy(pendingStars = it.pendingStars - projectId) }
      result.fold(
        onSuccess = { updated ->
          _uiState.update { s ->
            fun List<GitLabProject>.swap() = map { if (it.id == projectId) updated else it }
            val projects = if (s.projects.data.any { it.id == projectId }) s.projects.data.swap()
            else if (updated.isStarred) listOf(updated) + s.projects.data else s.projects.data
            s.copy(
              projects = s.projects.copy(data = projects),
              searchResults = s.searchResults.copy(data = s.searchResults.data.swap()),
              group = s.group.copy(projects = s.group.projects.copy(data = s.group.projects.data.swap())),
              project = if (s.project.project.data?.id == projectId) s.project.copy(project = s.project.project.copy(data = updated)) else s.project,
            )
          }
        },
        onFailure = { error -> notifyError(error.userMessage()) }
      )
    }
  }

  fun createProject(name: String, description: String, visibility: String, onResult: (String?) -> Unit) {
    session.launch {
      api { repository.createProject(it, name.trim(), description, visibility) }.fold(
        onSuccess = { project ->
          _uiState.update { s -> s.copy(projects = s.projects.copy(data = listOf(project) + s.projects.data.filterNot { it.id == project.id })) }
          onResult(null)
          notify("Created ${project.name}")
        },
        onFailure = { error -> onResult(error.userMessage()) }
      )
    }
  }

  // -------------------------------------------------------------------------------------------
  // Issues
  // -------------------------------------------------------------------------------------------

  private fun seedIssue(issue: GitLabIssue) {
    _uiState.update {
      it.copy(issueDetail = IssueDetailState(issue.projectId, issue.iid, Loadable<GitLabIssue?>(issue, loaded = true)))
    }
  }

  private fun openIssue(projectId: Long, iid: Long, seeded: Boolean = false) {
    code.ensureLabels(projectId)
    issueJob?.cancel()
    if (!seeded) {
      val known = _uiState.value.let { s -> (s.issues.data + s.project.issues.data).firstOrNull { it.projectId == projectId && it.iid == iid } }
      _uiState.update { it.copy(issueDetail = IssueDetailState(projectId, iid, Loadable<GitLabIssue?>(known, loaded = known != null))) }
    }
    _uiState.update { s -> s.copy(issueDetail = s.issueDetail.copy(issue = s.issueDetail.issue.start(), notes = s.issueDetail.notes.start())) }
    issueJob = session.launch {
      coroutineScope {
        launch {
          api { repository.getIssue(it, projectId, iid) }.fold(
            onSuccess = { issue ->
              updateIssueDetail(projectId, iid) { d ->
                val known = d.issue.data?.labelDetails.orEmpty()
                d.copy(issue = d.issue.success(issue.copy(labelDetails = issue.labelDetails.ifEmpty { known })))
              }
            },
            onFailure = { error -> updateIssueDetail(projectId, iid) { d -> d.copy(issue = d.issue.failure(error.userMessage())) } }
          )
        }
        launch {
          api { repository.getIssueNotes(it, projectId, iid) }.fold(
            onSuccess = { notes -> updateIssueDetail(projectId, iid) { d -> d.copy(notes = d.notes.success(notes)) } },
            onFailure = { error -> updateIssueDetail(projectId, iid) { d -> d.copy(notes = d.notes.failure(error.userMessage())) } }
          )
        }
      }
    }
  }

  fun retryIssue() {
    val d = _uiState.value.issueDetail
    if (d.projectId > 0) openIssue(d.projectId, d.iid, seeded = true)
  }

  private inline fun updateIssueDetail(projectId: Long, iid: Long, crossinline transform: (IssueDetailState) -> IssueDetailState) {
    _uiState.update { s -> if (s.issueDetail.projectId == projectId && s.issueDetail.iid == iid) s.copy(issueDetail = transform(s.issueDetail)) else s }
  }

  private fun replaceIssue(updated: GitLabIssue) {
    _uiState.update { s ->
      fun List<GitLabIssue>.swap() = map { if (it.projectId == updated.projectId && it.iid == updated.iid) updated else it }
      s.copy(
        issues = s.issues.copy(data = s.issues.data.swap()),
        project = s.project.copy(issues = s.project.issues.copy(data = s.project.issues.data.swap())),
        issueDetail = if (s.issueDetail.projectId == updated.projectId && s.issueDetail.iid == updated.iid) {
          s.issueDetail.copy(issue = s.issueDetail.issue.success(updated), busy = false)
        } else s.issueDetail,
      )
    }
  }

  fun createIssue(projectId: Long, title: String, description: String?, labels: String?, onResult: (String?) -> Unit) {
    session.launch {
      api { repository.createIssue(it, projectId, title.trim(), description?.takeIf { d -> d.isNotBlank() }, labels?.takeIf { l -> l.isNotBlank() }) }.fold(
        onSuccess = { issue ->
          _uiState.update { s ->
            s.copy(
              issues = s.issues.copy(data = listOf(issue) + s.issues.data),
              project = if (s.project.projectId == projectId && s.project.issues.loaded) {
                s.project.copy(issues = s.project.issues.copy(data = listOf(issue) + s.project.issues.data))
              } else s.project,
            )
          }
          onResult(null)
          notify("Issue #${issue.iid} created")
        },
        onFailure = { error -> onResult(error.userMessage()) }
      )
    }
  }

  fun setIssueClosed(closed: Boolean) {
    val issue = _uiState.value.issueDetail.issue.data ?: return
    if (_uiState.value.issueDetail.busy) return
    updateIssueDetail(issue.projectId, issue.iid) { it.copy(busy = true) }
    session.launch {
      api { repository.setIssueState(it, issue, closed) }.fold(
        onSuccess = { updated ->
          replaceIssue(updated)
          notify(if (closed) "Issue closed" else "Issue reopened")
        },
        onFailure = { error ->
          updateIssueDetail(issue.projectId, issue.iid) { it.copy(busy = false) }
          notifyError(error.userMessage())
        }
      )
    }
  }

  fun updateIssue(title: String, description: String, onResult: (String?) -> Unit) {
    val issue = _uiState.value.issueDetail.issue.data ?: return
    session.launch {
      api { repository.updateIssue(it, issue, title.trim(), description) }.fold(
        onSuccess = { updated ->
          replaceIssue(updated)
          onResult(null)
          notify("Issue updated")
        },
        onFailure = { error -> onResult(error.userMessage()) }
      )
    }
  }

  fun postIssueNote(body: String, onResult: (String?) -> Unit) {
    val detail = _uiState.value.issueDetail
    val issue = detail.issue.data ?: return
    if (detail.posting) return
    updateIssueDetail(issue.projectId, issue.iid) { it.copy(posting = true) }
    session.launch {
      api { repository.createIssueNote(it, issue.projectId, issue.iid, body) }.fold(
        onSuccess = { note ->
          updateIssueDetail(issue.projectId, issue.iid) { it.copy(posting = false, notes = it.notes.copy(data = it.notes.data + note)) }
          onResult(null)
        },
        onFailure = { error ->
          updateIssueDetail(issue.projectId, issue.iid) { it.copy(posting = false) }
          onResult(error.userMessage())
        }
      )
    }
  }

  // -------------------------------------------------------------------------------------------
  // Pipelines, profile, inbox
  // -------------------------------------------------------------------------------------------

  fun retryPipeline(pipeline: GitLabPipeline) {
    session.launch {
      api { repository.retryPipeline(it, pipeline.projectId, pipeline.id) }.fold(
        onSuccess = { retried ->
          _uiState.update { s ->
            fun List<GitLabPipeline>.swap() = map { if (it.id == retried.id) retried else it }
            s.copy(
              pipelines = s.pipelines.copy(data = s.pipelines.data.swap()),
              project = s.project.copy(pipelines = s.project.pipelines.copy(data = s.project.pipelines.data.swap())),
              mergeRequestDetail = s.mergeRequestDetail.copy(pipelines = s.mergeRequestDetail.pipelines.copy(data = s.mergeRequestDetail.pipelines.data.swap())),
            )
          }
          notify("Pipeline #${retried.id} restarted")
          if (_uiState.value.pipelineDetail.pipelineId == retried.id) ci.loadPipeline(retried.projectId, retried.id)
        },
        onFailure = { error -> notifyError(error.userMessage()) }
      )
    }
  }

  fun setUserStatus(message: String) {
    session.launch {
      api { repository.setUserStatus(it, message) }.fold(
        onSuccess = { status ->
          _uiState.update { it.copy(userStatus = status) }
          notify(if (message.isBlank()) "Status cleared" else "Status updated")
        },
        onFailure = { error -> notifyError(error.userMessage()) }
      )
    }
  }

  fun setInboxFilter(filter: InboxFilter) {
    _uiState.update { it.copy(inboxFilter = filter) }
  }

  fun markNotificationDone(id: String) {
    val current = _uiState.value.notifications.data.find { it.id == id } ?: return
    if (!current.isUnread) return
    val todoId = id.toLongOrNull() ?: return
    setNotificationUnread(id, false)
    session.launch {
      api { repository.markTodoDone(it, todoId) }.onFailure { error ->
        setNotificationUnread(id, true)
        notifyError(error.userMessage())
      }
    }
  }

  private fun setNotificationUnread(id: String, unread: Boolean) {
    _uiState.update { s ->
      s.copy(notifications = s.notifications.copy(data = s.notifications.data.map { if (it.id == id) it.copy(isUnread = unread) else it }))
    }
  }

  fun markAllNotificationsDone() {
    val before = _uiState.value.notifications.data
    if (before.none { it.isUnread }) return
    _uiState.update { s -> s.copy(notifications = s.notifications.copy(data = s.notifications.data.map { it.copy(isUnread = false) })) }
    session.launch {
      api { repository.markAllTodosDone(it) }.fold(
        onSuccess = { notify("All to-dos marked as done") },
        onFailure = { error ->
          _uiState.update { s -> s.copy(notifications = s.notifications.copy(data = before)) }
          notifyError(error.userMessage())
        }
      )
    }
  }

  /** Opens what a to-do points at. Returns the URL to open externally when the app has no screen for it. */
  fun openNotification(notification: GitLabNotification): String? {
    markNotificationDone(notification.id)
    val projectId = notification.targetProjectId
    val iid = notification.targetIid
    return when {
      projectId != null && iid != null && notification.targetType == "Issue" -> {
        navigateTo(ScreenDestination.IssueDetail(projectId, iid)); null
      }
      projectId != null && iid != null && notification.targetType == "MergeRequest" -> {
        navigateTo(ScreenDestination.MergeRequestDetail(projectId, iid)); null
      }
      else -> notification.targetUrl
    }
  }

  // -------------------------------------------------------------------------------------------
  // Messages
  // -------------------------------------------------------------------------------------------

  private fun notify(text: String) {
    _messages.trySend(UiMessage(text))
  }

  private fun notifyError(text: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
    _messages.trySend(UiMessage(text, isError = true, actionLabel = actionLabel, action = action))
  }
}


private fun GitLabTodo.toNotification(): GitLabNotification {
  val type = when {
    actionName == "build_failed" -> NotificationType.PIPELINE
    actionName == "mentioned" || actionName == "directly_addressed" -> NotificationType.MENTION
    targetType == "MergeRequest" -> NotificationType.MERGE_REQUEST
    targetType == "Issue" -> NotificationType.ISSUE
    else -> NotificationType.DISCUSSION
  }
  val badge = when (actionName) {
    "assigned" -> "Assigned"
    "mentioned", "directly_addressed" -> "Mentioned"
    "review_requested" -> "Review requested"
    "approval_required" -> "Approval required"
    "build_failed" -> "Pipeline failed"
    "marked" -> "To-do"
    "unmergeable" -> "Can't merge"
    "merge_train_removed" -> "Removed from merge train"
    "member_access_requested" -> "Access requested"
    else -> actionName.replace('_', ' ').replaceFirstChar { char ->
      if (char.isLowerCase()) char.titlecase() else char.toString()
    }
  }
  val iid = target?.iid?.takeIf { it > 0 }
  val prefix = if (targetType == "MergeRequest") "!" else "#"
  val targetTitle = target?.title?.takeIf { it.isNotBlank() }
  val title = when {
    targetTitle != null && iid != null -> "$prefix$iid $targetTitle"
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
    authorAvatarUrl = author.avatarUrl,
    timestamp = formatGitLabTime(createdAt),
    isUnread = state == "pending",
    actionName = actionName,
    targetType = targetType,
    targetProjectId = target?.projectId ?: project?.id,
    targetIid = iid,
    targetUrl = targetUrl,
    subtitle = body?.lineSequence()?.firstOrNull()?.trim()?.take(180).orEmpty().takeIf { it != targetTitle }.orEmpty(),
    statusBadge = badge.ifBlank { null }
  )
}
