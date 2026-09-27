package com.aistudio.gitlab.ui.viewmodel

import com.aistudio.gitlab.data.model.GitLabProject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A project's page, its lists, the code browser, files and commits. */
class ProjectFeature internal constructor(private val host: SessionHost) {

  private var projectJob: Job? = null
  private var readmeJob: Job? = null
  private var languagesJob: Job? = null
  private val sectionJobs = mutableMapOf<ProjectSection, Job>()
  private var treeJob: Job? = null
  private var fileJob: Job? = null
  private var commitJob: Job? = null
  private val labelJobs = mutableMapOf<Long, Job>()

  private val detail get() = host.state.value.project

  internal fun reset() {
    listOf(projectJob, readmeJob, languagesJob, treeJob, fileJob, commitJob, refSearchJob).forEach { it?.cancel() }
    sectionJobs.values.forEach { it.cancel() }
    sectionJobs.clear()
    labelJobs.values.forEach { it.cancel() }
    labelJobs.clear()
  }

  // -------------------------------------------------------------------------------------------
  // Project page
  // -------------------------------------------------------------------------------------------

  internal fun open(projectId: Long) {
    reset()
    val cached = host.state.value.let { s ->
      (s.projects.data + s.searchResults.data + s.group.projects.data + s.search.projects.data + s.userDetail.projects.data)
        .firstOrNull { it.id == projectId }
    }
    host.state.update {
      it.copy(project = ProjectDetailState(projectId = projectId, project = Loadable<GitLabProject?>(cached, loading = true, loaded = cached != null)))
    }
    projectJob = host.session.launch {
      host.api { host.repository.getProjectWithLicense(it, projectId) }.fold(
        onSuccess = { fetched ->
          // GET /projects/:id has no "starred" field; keep what the starred list told us.
          val project = fetched.copy(isStarred = fetched.isStarred || cached?.isStarred == true)
          update(projectId) { p -> p.copy(project = p.project.success(project)) }
          loadReadme()
          loadLanguages()
          ensureLabels(projectId)
        },
        onFailure = { error -> update(projectId) { p -> p.copy(project = p.project.failure(error.userMessage())) } }
      )
    }
  }

  fun retry() {
    val id = detail.projectId
    if (id > 0) open(id)
  }

  private fun loadReadme() {
    val d = detail
    val project = d.project.data ?: return
    val ref = d.effectiveRef
    if (ref == null || project.emptyRepo) {
      update(project.id) { it.copy(readme = it.readme.success(null)) }
      return
    }
    readmeJob = host.load(
      readmeJob,
      start = { s -> s.withProject(project.id) { it.copy(readme = it.readme.start()) } },
      request = { host.repository.getReadme(it, project.id, ref) },
      finish = { s, r -> s.withProject(project.id) { it.copy(readme = it.readme.settle(r)) } },
    )
  }

  private fun loadLanguages() {
    val id = detail.projectId
    languagesJob = host.load(
      languagesJob,
      start = { s -> s.withProject(id) { it.copy(languages = it.languages.start()) } },
      request = { host.repository.getLanguages(it, id) },
      finish = { s, r -> s.withProject(id) { it.copy(languages = it.languages.settle(r)) } },
    )
  }

  /** Switches the branch or tag the code browser, README and commits follow. */
  fun setRef(ref: String?) {
    val d = detail
    if (d.project.data == null) return
    val normalized = ref?.takeIf { it != d.project.data.defaultBranch }
    if (normalized == d.ref) return
    update(d.projectId) {
      it.copy(ref = normalized, treePath = "", tree = emptyFeed(), commits = emptyFeed(), readme = Loadable(null))
    }
    loadReadme()
    openPath("")
    if (host.state.value.destination is ScreenDestination.ProjectSectionList) loadSection(ProjectSection.COMMITS, force = true)
  }

  // -------------------------------------------------------------------------------------------
  // Sections
  // -------------------------------------------------------------------------------------------

  fun openSection(section: ProjectSection) {
    val id = detail.projectId
    if (id > 0) host.navigateTo(ScreenDestination.ProjectSectionList(id, section))
  }

  fun retrySection(section: ProjectSection) = loadSection(section, force = true)

  internal fun loadSection(section: ProjectSection, force: Boolean = false) {
    val d = detail
    val project = d.project.data ?: return
    val id = project.id
    val ref = d.effectiveRef
    val feed = when (section) {
      ProjectSection.ISSUES -> d.issues
      ProjectSection.MERGE_REQUESTS -> d.mergeRequests
      ProjectSection.PIPELINES -> d.pipelines
      ProjectSection.COMMITS -> d.commits
      ProjectSection.BRANCHES -> d.branches
      ProjectSection.TAGS -> d.tags
      ProjectSection.RELEASES -> d.releases
      ProjectSection.CONTRIBUTORS -> d.contributors
      ProjectSection.ACTIVITY -> d.events
    }
    if (!force && (feed.loaded || feed.loading)) return
    val repo = host.repository
    sectionJobs[section] = when (section) {
      ProjectSection.ISSUES -> host.load(sectionJobs[section], { s -> s.withProject(id) { it.copy(issues = it.issues.start()) } },
        { repo.getProjectIssues(it, id) }, { s, r -> s.withProject(id) { it.copy(issues = it.issues.settle(r)) } })
      ProjectSection.MERGE_REQUESTS -> host.load(sectionJobs[section], { s -> s.withProject(id) { it.copy(mergeRequests = it.mergeRequests.start()) } },
        { repo.getProjectMergeRequests(it, id) }, { s, r -> s.withProject(id) { it.copy(mergeRequests = it.mergeRequests.settle(r)) } })
      ProjectSection.PIPELINES -> host.load(sectionJobs[section], { s -> s.withProject(id) { it.copy(pipelines = it.pipelines.start()) } },
        { instance -> repo.getPipelines(instance, id).map { list -> repo.attachJobs(instance, list, limit = 8) } },
        { s, r -> s.withProject(id) { it.copy(pipelines = it.pipelines.settle(r)) } })
      ProjectSection.COMMITS -> host.load(sectionJobs[section], { s -> s.withProject(id) { it.copy(commits = it.commits.start()) } },
        { if (ref == null) Result.success(emptyList()) else repo.getCommits(it, id, ref) },
        { s, r -> s.withProject(id) { it.copy(commits = it.commits.settle(r)) } })
      ProjectSection.BRANCHES -> host.load(sectionJobs[section], { s -> s.withProject(id) { it.copy(branches = it.branches.start()) } },
        { repo.getBranches(it, id) }, { s, r -> s.withProject(id) { it.copy(branches = it.branches.settle(r)) } })
      ProjectSection.TAGS -> host.load(sectionJobs[section], { s -> s.withProject(id) { it.copy(tags = it.tags.start()) } },
        { repo.getTags(it, id) }, { s, r -> s.withProject(id) { it.copy(tags = it.tags.settle(r)) } })
      ProjectSection.RELEASES -> host.load(sectionJobs[section], { s -> s.withProject(id) { it.copy(releases = it.releases.start()) } },
        { repo.getReleases(it, id) }, { s, r -> s.withProject(id) { it.copy(releases = it.releases.settle(r)) } })
      ProjectSection.CONTRIBUTORS -> host.load(sectionJobs[section], { s -> s.withProject(id) { it.copy(contributors = it.contributors.start()) } },
        { repo.getContributors(it, id) }, { s, r -> s.withProject(id) { it.copy(contributors = it.contributors.settle(r)) } })
      ProjectSection.ACTIVITY -> host.load(sectionJobs[section], { s -> s.withProject(id) { it.copy(events = it.events.start()) } },
        { repo.getProjectEvents(it, id) }, { s, r -> s.withProject(id) { it.copy(events = it.events.settle(r)) } })
    }
  }

  /** Branches and tags for the ref picker. */
  fun loadRefs() {
    loadSection(ProjectSection.BRANCHES)
    loadSection(ProjectSection.TAGS)
  }

  private var refSearchJob: Job? = null

  /**
   * Filters branches and tags on the server: large projects have thousands of branches, so
   * filtering only the loaded page would hide most of them. An empty query restores the lists.
   */
  fun filterRefs(query: String) {
    val id = detail.projectId
    if (id <= 0) return
    refSearchJob?.cancel()
    val q = query.trim()
    refSearchJob = host.session.launch {
      if (q.isNotEmpty()) kotlinx.coroutines.delay(300)
      host.state.update { s -> s.withProject(id) { it.copy(branches = it.branches.start(), tags = it.tags.start()) } }
      launch {
        val r = host.api { host.repository.getBranches(it, id, q.ifEmpty { null }) }
        host.state.update { s -> s.withProject(id) { it.copy(branches = it.branches.settle(r)) } }
      }
      launch {
        val r = host.api { host.repository.getTags(it, id, q.ifEmpty { null }) }
        host.state.update { s -> s.withProject(id) { it.copy(tags = it.tags.settle(r)) } }
      }
    }
  }

  fun openRelease(tagName: String) {
    val id = detail.projectId
    if (id <= 0) return
    loadSection(ProjectSection.RELEASES)
    host.navigateTo(ScreenDestination.ReleaseDetail(id, tagName))
  }

  // -------------------------------------------------------------------------------------------
  // Code browser, files, commits
  // -------------------------------------------------------------------------------------------

  fun openCode() {
    val id = detail.projectId
    if (id <= 0) return
    host.navigateTo(ScreenDestination.ProjectCode(id))
  }

  internal fun ensureTree() {
    val d = detail
    if (!d.tree.loaded && !d.tree.loading) openPath(d.treePath)
  }

  fun openPath(path: String) {
    val d = detail
    val project = d.project.data ?: return
    val id = project.id
    val ref = d.effectiveRef
    if (ref == null || project.emptyRepo) {
      update(id) { it.copy(treePath = path, tree = it.tree.success(emptyList())) }
      return
    }
    treeJob = host.load(
      treeJob,
      start = { s -> s.withProject(id) { it.copy(treePath = path, tree = it.tree.copy(loading = true, error = null)) } },
      request = { host.repository.getRepositoryTree(it, id, path, ref) },
      finish = { s, r ->
        s.withProject(id) {
          if (it.treePath != path) it
          else r.fold(
            onSuccess = { list -> it.copy(tree = it.tree.success(list)) },
            onFailure = { e -> it.copy(tree = it.tree.copy(data = emptyList()).failure(e.userMessage())) },
          )
        }
      },
    )
  }

  fun openFile(path: String) {
    val d = detail
    if (d.projectId > 0) host.navigateTo(ScreenDestination.FileViewer(d.projectId, path, d.effectiveRef))
  }

  internal fun loadFile(projectId: Long, path: String, ref: String?) {
    fileJob = host.load(
      fileJob,
      start = { it.copy(file = FileState(projectId, path, Loadable<String?>(null, loading = true), ref)) },
      request = { host.repository.getFileContent(it, projectId, path, ref) },
      finish = { s, r ->
        if (s.file.projectId != projectId || s.file.path != path) s
        else s.copy(file = s.file.copy(content = s.file.content.settle(r.map { text -> text as String? })))
      },
    )
  }

  fun retryFile() {
    val f = host.state.value.file
    if (f.projectId > 0) loadFile(f.projectId, f.path, f.ref)
  }

  fun openCommit(projectId: Long, sha: String) {
    host.navigateTo(ScreenDestination.CommitDetail(projectId, sha))
  }

  internal fun loadCommit(projectId: Long, sha: String) {
    commitJob?.cancel()
    host.state.update {
      it.copy(commitDetail = CommitDetailState(projectId, sha, Loadable(null, loading = true), emptyFeed<com.aistudio.gitlab.data.model.GitLabFileDiff>().start()))
    }
    commitJob = host.session.launch {
      launch {
        val result = host.api { host.repository.getCommit(it, projectId, sha) }
        host.state.update { s -> if (s.commitDetail.sha != sha) s else s.copy(commitDetail = s.commitDetail.copy(commit = s.commitDetail.commit.settle(result.map { c -> c as com.aistudio.gitlab.data.model.GitLabCommit? }))) }
      }
      launch {
        val result = host.api { host.repository.getCommitDiff(it, projectId, sha) }
        host.state.update { s -> if (s.commitDetail.sha != sha) s else s.copy(commitDetail = s.commitDetail.copy(diffs = s.commitDetail.diffs.settle(result))) }
      }
    }
  }

  fun retryCommit() {
    val c = host.state.value.commitDetail
    if (c.projectId > 0) loadCommit(c.projectId, c.sha)
  }

  /** Label colors for a project, fetched once and shared by issues and merge requests. */
  internal fun ensureLabels(projectId: Long) {
    if (projectId <= 0 || host.state.value.labels.containsKey(projectId) || labelJobs[projectId]?.isActive == true) return
    labelJobs[projectId] = host.session.launch {
      host.api { host.repository.getLabels(it, projectId) }.onSuccess { labels ->
        host.state.update { s -> s.copy(labels = s.labels + (projectId to labels.associateBy { it.name })) }
      }
    }
  }

  private fun update(projectId: Long, transform: (ProjectDetailState) -> ProjectDetailState) {
    host.state.update { it.withProject(projectId, transform) }
  }
}

internal fun GitLabUiState.withProject(projectId: Long, transform: (ProjectDetailState) -> ProjectDetailState): GitLabUiState =
  if (project.projectId == projectId) copy(project = transform(project)) else this
