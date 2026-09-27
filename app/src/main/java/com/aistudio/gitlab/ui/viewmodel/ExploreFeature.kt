package com.aistudio.gitlab.ui.viewmodel

import android.net.Uri
import com.aistudio.gitlab.data.api.normalizeBaseUrl
import com.aistudio.gitlab.data.model.GitLabGroup
import com.aistudio.gitlab.data.model.GitLabProject
import com.aistudio.gitlab.data.model.GitLabUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Groups, people, global search, and routing GitLab web links to in-app screens. */
class ExploreFeature internal constructor(private val host: SessionHost) {

  private var groupJob: Job? = null
  private var userJob: Job? = null
  private var searchJob: Job? = null
  private var linkJob: Job? = null
  private val projectIdsByPath = mutableMapOf<String, Long>()

  internal fun reset() {
    listOf(groupJob, userJob, searchJob, linkJob).forEach { it?.cancel() }
    projectIdsByPath.clear()
  }

  // -------------------------------------------------------------------------------------------
  // Groups
  // -------------------------------------------------------------------------------------------

  internal fun loadGroup(group: GitLabGroup) {
    groupJob?.cancel()
    host.state.update {
      it.copy(group = GroupDetailState(
        group = group,
        details = Loadable<GitLabGroup?>(group, loaded = true).start(),
        subgroups = emptyFeed<GitLabGroup>().start(),
        projects = emptyFeed<GitLabProject>().start(),
      ))
    }
    val path = group.fullPath
    val repo = host.repository
    groupJob = host.session.launch {
      launch { val r = host.api { repo.getGroup(it, path) }; apply(path) { g -> g.copy(details = g.details.settle(r.map { it as GitLabGroup? })) } }
      launch { val r = host.api { repo.getSubgroups(it, path) }; apply(path) { g -> g.copy(subgroups = g.subgroups.settle(r)) } }
      launch { val r = host.api { repo.getGroupProjects(it, path) }; apply(path) { g -> g.copy(projects = g.projects.settle(r)) } }
    }
  }

  fun retryGroup() {
    host.state.value.group.group?.let(::loadGroup)
  }

  private fun apply(path: String, transform: (GroupDetailState) -> GroupDetailState) {
    host.state.update { s -> if (s.group.group?.fullPath == path) s.copy(group = transform(s.group)) else s }
  }

  // -------------------------------------------------------------------------------------------
  // People
  // -------------------------------------------------------------------------------------------

  fun openUser(user: GitLabUser) {
    if (user.id > 0) host.navigateTo(ScreenDestination.UserDetail(user.id, user.username))
    else if (user.username.isNotBlank()) host.navigateTo(ScreenDestination.UserDetail(0, user.username))
  }

  fun openUsername(username: String) {
    if (username.isNotBlank()) host.navigateTo(ScreenDestination.UserDetail(0, username))
  }

  internal fun loadUser(userId: Long, username: String) {
    userJob?.cancel()
    val known = host.state.value.let { s ->
      listOfNotNull(s.currentUser.data).plus(s.search.users.data).firstOrNull { (userId > 0 && it.id == userId) || (username.isNotBlank() && it.username == username) }
    }
    host.state.update {
      it.copy(userDetail = UserDetailState(userId, username, Loadable<GitLabUser?>(known, loaded = known != null).start(), emptyFeed<GitLabProject>().start(), emptyFeed<com.aistudio.gitlab.data.model.GitLabEvent>().start()))
    }
    val repo = host.repository
    userJob = host.session.launch {
      val byName: suspend () -> Result<GitLabUser> = {
        host.api { repo.findUser(it, username) }.mapCatching { it ?: throw IllegalStateException("No user named @$username on this server.") }
      }
      // GitLab.com hides /users/:id from anonymous requests but still answers the username lookup.
      val resolved = if (userId > 0) {
        host.api { repo.getUser(it, userId) }.let { r -> if (r.isFailure && username.isNotBlank()) byName() else r }
      } else byName()
      host.state.update { s -> if (!s.userDetail.matches(userId, username)) s else s.copy(userDetail = s.userDetail.copy(user = s.userDetail.user.settle(resolved.map { it as GitLabUser? }))) }
      val id = resolved.getOrNull()?.id ?: run {
        host.state.update { s -> if (!s.userDetail.matches(userId, username)) s else s.copy(userDetail = s.userDetail.copy(projects = s.userDetail.projects.success(emptyList()), events = s.userDetail.events.success(emptyList()))) }
        return@launch
      }
      launch {
        val r = host.api { repo.getUserProjects(it, id) }
        host.state.update { s -> if (!s.userDetail.matches(userId, username)) s else s.copy(userDetail = s.userDetail.copy(projects = s.userDetail.projects.settle(r))) }
      }
      launch {
        val r = host.api { repo.getUserEvents(it, id) }
        host.state.update { s -> if (!s.userDetail.matches(userId, username)) s else s.copy(userDetail = s.userDetail.copy(events = s.userDetail.events.settle(r))) }
      }
    }
  }

  fun retryUser() {
    val d = host.state.value.userDetail
    loadUser(d.userId, d.username)
  }

  private fun UserDetailState.matches(id: Long, name: String) = userId == id && username == name

  // -------------------------------------------------------------------------------------------
  // Search
  // -------------------------------------------------------------------------------------------

  fun setQuery(query: String) {
    host.state.update { it.copy(search = it.search.copy(query = query)) }
    scheduleSearch(debounce = true)
  }

  fun setScope(scope: SearchScope) {
    host.state.update { it.copy(search = it.search.copy(scope = scope)) }
    scheduleSearch(debounce = false)
  }

  private fun scheduleSearch(debounce: Boolean) {
    searchJob?.cancel()
    val search = host.state.value.search
    val query = search.query.trim()
    if (query.length < 2) {
      host.state.update { it.copy(search = SearchState(query = it.search.query, scope = it.search.scope)) }
      return
    }
    val scope = search.scope
    val repo = host.repository
    searchJob = host.session.launch {
      if (debounce) delay(350)
      host.state.update { s -> s.copy(search = s.search.startScope(scope)) }
      when (scope) {
        SearchScope.PROJECTS -> host.api { repo.getProjects(it, query) }.let { r -> finish(query) { it.copy(projects = it.projects.settle(r)) } }
        SearchScope.ISSUES -> host.api { repo.searchIssues(it, query) }.let { r -> finish(query) { it.copy(issues = it.issues.settle(r)) } }
        SearchScope.MERGE_REQUESTS -> host.api { repo.searchMergeRequests(it, query) }.let { r -> finish(query) { it.copy(mergeRequests = it.mergeRequests.settle(r)) } }
        SearchScope.USERS -> host.api { repo.searchUsers(it, query) }.let { r -> finish(query) { it.copy(users = it.users.settle(r)) } }
      }
    }
  }

  private fun SearchState.startScope(scope: SearchScope) = when (scope) {
    SearchScope.PROJECTS -> copy(projects = projects.start())
    SearchScope.ISSUES -> copy(issues = issues.start())
    SearchScope.MERGE_REQUESTS -> copy(mergeRequests = mergeRequests.start())
    SearchScope.USERS -> copy(users = users.start())
  }

  private fun finish(query: String, transform: (SearchState) -> SearchState) {
    host.state.update { s -> if (s.search.query.trim() != query) s else s.copy(search = transform(s.search)) }
  }

  // -------------------------------------------------------------------------------------------
  // Links
  // -------------------------------------------------------------------------------------------

  /**
   * Opens a GitLab web URL inside the app when it points at the active server and at something
   * the app has a screen for. Returns false to let the caller open it in the browser.
   */
  fun handleLink(url: String): Boolean {
    val instance = host.state.value.activeInstance ?: return false
    val base = runCatching { Uri.parse(normalizeBaseUrl(instance.url)) }.getOrNull() ?: return false
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    if (!uri.host.equals(base.host, ignoreCase = true)) return false
    val prefix = base.path.orEmpty().trimEnd('/')
    val path = uri.path.orEmpty().removePrefix(prefix).trim('/')
    if (path.isEmpty()) return false
    val parts = path.split("/-/", limit = 2)
    val projectPath = parts[0]
    val rest = parts.getOrNull(1)?.split('/').orEmpty()

    if (rest.isEmpty()) {
      return if ('/' !in projectPath) {
        openUsername(projectPath)
        true
      } else {
        withProject(url, projectPath) { id -> host.navigateTo(ScreenDestination.ProjectDetail(id)) }
      }
    }
    val kind = rest[0]
    val arg = rest.getOrNull(1)
    return when (kind) {
      "issues" -> arg?.toLongOrNull()?.let { iid -> withProject(url, projectPath) { host.navigateTo(ScreenDestination.IssueDetail(it, iid)) } } ?: false
      "merge_requests" -> arg?.toLongOrNull()?.let { iid -> withProject(url, projectPath) { host.navigateTo(ScreenDestination.MergeRequestDetail(it, iid)) } } ?: false
      "pipelines" -> arg?.toLongOrNull()?.let { pid -> withProject(url, projectPath) { host.navigateTo(ScreenDestination.PipelineDetail(it, pid)) } } ?: false
      "jobs" -> arg?.toLongOrNull()?.let { jid -> withProject(url, projectPath) { host.navigateTo(ScreenDestination.JobDetail(it, jid)) } } ?: false
      "commit" -> arg?.let { sha -> withProject(url, projectPath) { host.navigateTo(ScreenDestination.CommitDetail(it, sha)) } } ?: false
      "releases" -> arg?.let { tag -> withProject(url, projectPath) { host.navigateTo(ScreenDestination.ReleaseDetail(it, Uri.decode(tag))) } } ?: false
      "blob" -> {
        // /-/blob/<ref>/<path>: refs with slashes are ambiguous; assume a single-segment ref.
        val ref = arg ?: return false
        val file = rest.drop(2).joinToString("/").takeIf { it.isNotEmpty() } ?: return false
        withProject(url, projectPath) { host.navigateTo(ScreenDestination.FileViewer(it, Uri.decode(file), ref)) }
      }
      else -> false
    }
  }

  /** Resolves a project path to its id (cached), then runs [then]; falls back to the browser. */
  private fun withProject(url: String, path: String, then: (Long) -> Unit): Boolean {
    projectIdsByPath[path]?.let { then(it); return true }
    val current = host.state.value
    (current.projects.data + current.search.projects.data + listOfNotNull(current.project.project.data))
      .firstOrNull { it.pathWithNamespace.equals(path, ignoreCase = true) }
      ?.let { projectIdsByPath[path] = it.id; then(it.id); return true }
    linkJob?.cancel()
    linkJob = host.session.launch {
      host.api { host.repository.getProjectByPath(it, path) }.fold(
        onSuccess = { project -> projectIdsByPath[path] = project.id; then(project.id) },
        onFailure = { host.openExternal(url) },
      )
    }
    return true
  }
}
