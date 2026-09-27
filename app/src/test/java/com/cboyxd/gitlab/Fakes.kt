package com.cboyxd.gitlab

import com.cboyxd.gitlab.data.auth.GitLabAuth
import com.cboyxd.gitlab.data.model.GitLabCommit
import com.cboyxd.gitlab.data.model.GitLabFileDiff
import com.cboyxd.gitlab.data.model.GitLabInstance
import com.cboyxd.gitlab.data.model.GitLabIssue
import com.cboyxd.gitlab.data.model.GitLabLabel
import com.cboyxd.gitlab.data.model.GitLabMergeRequest
import com.cboyxd.gitlab.data.model.GitLabNote
import com.cboyxd.gitlab.data.model.GitLabReferences
import com.cboyxd.gitlab.data.model.GitLabTreeItem
import com.cboyxd.gitlab.data.model.GitLabProject
import com.cboyxd.gitlab.data.model.GitLabRelease
import com.cboyxd.gitlab.data.model.GitLabUser
import com.cboyxd.gitlab.ui.viewmodel.FileState
import com.cboyxd.gitlab.ui.viewmodel.GitLabUiState
import com.cboyxd.gitlab.ui.viewmodel.IssueDetailState
import com.cboyxd.gitlab.ui.viewmodel.MergeRequestDetailState
import com.cboyxd.gitlab.ui.viewmodel.Loadable
import com.cboyxd.gitlab.ui.viewmodel.ProjectDetailState

/** Invented public-looking data for UI and screenshot tests. Nothing here comes from a real server. */
object Fakes {
  val signedIn = GitLabInstance(
    id = "test-account",
    name = "GitLab.com",
    url = "https://gitlab.com",
    token = "glpat-test",
    username = "tanuki",
    isActive = true,
    isCustom = false,
    statusOk = true,
    authKind = GitLabAuth.KIND_PAT,
  )

  val user = GitLabUser(id = 1, username = "tanuki", name = "Tanuki Tester")

  fun project(
    id: Long,
    name: String,
    namespace: String = "Example Org",
    starred: Boolean = false,
    stars: Int = 0,
  ) = GitLabProject(
    id = id,
    name = name,
    nameWithNamespace = "$namespace / $name",
    pathWithNamespace = "${namespace.lowercase().replace(' ', '-')}/${name.lowercase()}",
    description = "An example project used by the app's tests.",
    defaultBranch = "main",
    visibility = "public",
    starCount = stars,
    forksCount = 3,
    openIssuesCount = 12,
    webUrl = "https://gitlab.com/example/${name.lowercase()}",
    isStarred = starred,
    issuesEnabled = true,
    mergeRequestsEnabled = true,
    jobsEnabled = true,
  )

  val projects = listOf(
    project(1, "rocket", starred = true, stars = 1234),
    project(2, "comet", namespace = "Space Tools", starred = true, stars = 56),
    project(3, "asteroid", starred = false, stars = 7),
  )

  fun guestState() = GitLabUiState(
    bootstrapped = true,
    activeInstance = GitLabAuth.GUEST,
    projects = Loadable(projects, loaded = true),
  )

  fun signedInState() = GitLabUiState(
    bootstrapped = true,
    activeInstance = signedIn,
    savedInstances = listOf(signedIn),
    currentUser = Loadable(user, loaded = true),
    projects = Loadable(projects, loaded = true),
    issues = Loadable(emptyList(), loaded = true),
    mergeRequests = Loadable(emptyList(), loaded = true),
    pipelines = Loadable(emptyList(), loaded = true),
    groups = Loadable(emptyList(), loaded = true),
    activity = Loadable(emptyList(), loaded = true),
  )

  fun projectState(): ProjectDetailState {
    val rocket = projects.first()
    return ProjectDetailState(
      projectId = rocket.id,
      project = Loadable(rocket, loaded = true),
      readme = Loadable("# Rocket\n\nAn example **README** with a [link](https://example.com).", loaded = true),
      releases = Loadable(
        listOf(GitLabRelease(tagName = "v1.2.0", name = "Rocket v1.2.0", releasedAt = "3d ago", createdAt = "3d ago")),
        loaded = true,
      ),
    )
  }

  val labels = mapOf(
    "bug" to GitLabLabel(id = 1, name = "bug", color = "#dd2b0e"),
    "priority::high" to GitLabLabel(id = 2, name = "priority::high", color = "#e9a820"),
  )

  fun issue(iid: Long, title: String, state: String = "opened", labels: List<String> = emptyList(), notes: Int = 0) = GitLabIssue(
    id = 100 + iid,
    iid = iid,
    projectId = 1,
    title = title,
    description = "Steps to reproduce:\n\n1. Open the app\n2. Tap **Home**",
    state = state,
    createdAt = "3h ago",
    updatedAt = "3h ago",
    author = user,
    labels = labels,
    // Lists ask for with_labels_details, so colors arrive with the item.
    labelDetails = labels.mapNotNull { this.labels[it] },
    userNotesCount = notes,
    webUrl = "https://gitlab.com/example-org/rocket/-/issues/$iid",
    references = GitLabReferences(short = "#$iid", full = "example-org/rocket#$iid"),
  )

  val issues = listOf(
    issue(7, "Launch button does nothing on tablets", labels = listOf("bug", "priority::high"), notes = 4),
    issue(6, "Add dark mode to the settings page"),
    issue(5, "Old crash on startup", state = "closed"),
  )

  fun mergeRequest(iid: Long, title: String, state: String = "opened", draft: Boolean = false) = GitLabMergeRequest(
    id = 200 + iid,
    iid = iid,
    projectId = 1,
    title = title,
    description = "Fixes the launch button.",
    state = state,
    sourceBranch = "fix-launch",
    targetBranch = "main",
    createdAt = "6h ago",
    updatedAt = "6h ago",
    author = user,
    labels = listOf("bug"),
    labelDetails = listOfNotNull(labels["bug"]),
    draft = draft,
    webUrl = "https://gitlab.com/example-org/rocket/-/merge_requests/$iid",
    detailedMergeStatus = "mergeable",
    references = GitLabReferences(short = "!$iid", full = "example-org/rocket!$iid"),
  )

  val mergeRequests = listOf(
    mergeRequest(12, "Fix the launch button on tablets"),
    mergeRequest(11, "Draft: rewrite the settings page", draft = true),
    mergeRequest(10, "Bump the version", state = "merged"),
  )

  fun issueDetail(notesFailed: Boolean = false) = IssueDetailState(
    projectId = 1,
    iid = 7,
    issue = Loadable(issues.first(), loaded = true),
    notes = if (notesFailed) Loadable(emptyList<GitLabNote>(), loaded = true, error = "403 Forbidden")
    else Loadable(listOf(GitLabNote(id = 1, body = "I can reproduce this.", author = user, createdAt = "2h ago")), loaded = true),
  )

  val diffs = listOf(
    GitLabFileDiff(oldPath = "src/launch.kt", newPath = "src/launch.kt", diff = "@@ -1,3 +1,3 @@\n fun launch() {\n-  return\n+  ignite()\n }\n"),
    GitLabFileDiff(oldPath = "README.md", newPath = "README.md", diff = "@@ -1 +1,2 @@\n # Rocket\n+Now launches.\n"),
  )

  fun mergeRequestDetail() = MergeRequestDetailState(
    projectId = 1,
    iid = 12,
    mergeRequest = Loadable(mergeRequests.first(), loaded = true),
    diffs = Loadable(diffs, loaded = true),
    notes = Loadable(listOf(GitLabNote(id = 2, body = "Looks good to me.", author = user, createdAt = "1h ago")), loaded = true),
    commits = Loadable(listOf(GitLabCommit(id = "abc123", title = "Fix launch")), loaded = true),
  )

  fun codeState(): ProjectDetailState = projectState().copy(
    tree = Loadable(
      listOf(
        GitLabTreeItem(id = "1", name = "src", type = "tree", path = "src"),
        GitLabTreeItem(id = "2", name = "docs", type = "tree", path = "docs"),
        GitLabTreeItem(id = "3", name = "README.md", type = "blob", path = "README.md"),
        GitLabTreeItem(id = "4", name = "build.gradle.kts", type = "blob", path = "build.gradle.kts"),
      ),
      loaded = true,
    ),
  )

  fun file() = FileState(
    projectId = 1,
    path = "src/launch.kt",
    ref = "main",
    content = Loadable((1..40).joinToString("\n") { "val line$it = $it" }, loaded = true),
  )
}
