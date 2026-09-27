package com.aistudio.gitlab.ui.viewmodel

import com.aistudio.gitlab.data.model.GitLabMergeRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Merge request detail: overview, discussion, commits, pipelines, approvals and changes. */
class ReviewFeature internal constructor(
  private val host: SessionHost,
  private val labels: (Long) -> Unit,
) {

  private var job: Job? = null

  private val detail get() = host.state.value.mergeRequestDetail

  internal fun reset() {
    job?.cancel()
  }

  fun open(mr: GitLabMergeRequest) {
    host.state.update {
      it.copy(mergeRequestDetail = MergeRequestDetailState(mr.projectId, mr.iid, Loadable<GitLabMergeRequest?>(mr, loaded = true)))
    }
    host.navigateTo(ScreenDestination.MergeRequestDetail(mr.projectId, mr.iid))
  }

  /** Loads everything the detail screen shows, in parallel. Keeps a seeded summary if present. */
  internal fun load(projectId: Long, iid: Long) {
    job?.cancel()
    labels(projectId)
    host.state.update { s ->
      val seeded: Loadable<GitLabMergeRequest?> = s.mergeRequestDetail.takeIf { it.projectId == projectId && it.iid == iid }?.mergeRequest
        ?: (s.mergeRequests.data + s.project.mergeRequests.data).firstOrNull { it.projectId == projectId && it.iid == iid }
          ?.let { Loadable<GitLabMergeRequest?>(it, loaded = true) }
        ?: Loadable(null)
      s.copy(
        mergeRequestDetail = MergeRequestDetailState(
          projectId = projectId,
          iid = iid,
          mergeRequest = seeded.start(),
          diffs = emptyFeed<com.aistudio.gitlab.data.model.GitLabFileDiff>().start(),
          notes = emptyFeed<com.aistudio.gitlab.data.model.GitLabNote>().start(),
          commits = emptyFeed<com.aistudio.gitlab.data.model.GitLabCommit>().start(),
          pipelines = emptyFeed<com.aistudio.gitlab.data.model.GitLabPipeline>().start(),
          approvals = Loadable<com.aistudio.gitlab.data.model.GitLabApprovals?>(null).start(),
        )
      )
    }
    val repo = host.repository
    job = host.session.launch {
      launch {
        val r = host.api { repo.getMergeRequest(it, projectId, iid) }
        // The single-MR endpoint has no label colors; keep the ones the list already delivered.
        apply(projectId, iid) { d ->
          val known = d.mergeRequest.data?.labelDetails.orEmpty()
          d.copy(mergeRequest = d.mergeRequest.settle(r.map { mr -> mr.copy(labelDetails = mr.labelDetails.ifEmpty { known }) as GitLabMergeRequest? }))
        }
      }
      launch { val r = host.api { repo.getMergeRequestDiffs(it, projectId, iid) }; apply(projectId, iid) { it.copy(diffs = it.diffs.settle(r)) } }
      launch { val r = host.api { repo.getMergeRequestNotes(it, projectId, iid) }; apply(projectId, iid) { it.copy(notes = it.notes.settle(r)) } }
      launch { val r = host.api { repo.getMergeRequestCommits(it, projectId, iid) }; apply(projectId, iid) { it.copy(commits = it.commits.settle(r)) } }
      launch { val r = host.api { repo.getMergeRequestPipelines(it, projectId, iid) }; apply(projectId, iid) { it.copy(pipelines = it.pipelines.settle(r)) } }
      launch { val r = host.api { repo.getMergeRequestApprovals(it, projectId, iid) }; apply(projectId, iid) { it.copy(approvals = it.approvals.settle(r)) } }
    }
  }

  fun retry() {
    val d = detail
    if (d.projectId > 0) load(d.projectId, d.iid)
  }

  fun setClosed(closed: Boolean) {
    val mr = detail.mergeRequest.data ?: return
    if (detail.busy) return
    apply(mr.projectId, mr.iid) { it.copy(busy = true) }
    host.session.launch {
      host.api { host.repository.setMergeRequestState(it, mr, closed) }.fold(
        onSuccess = { updated ->
          replace(updated)
          host.notify(if (closed) "Merge request closed" else "Merge request reopened")
        },
        onFailure = { error ->
          apply(mr.projectId, mr.iid) { it.copy(busy = false) }
          host.notifyError(error.userMessage())
        }
      )
    }
  }

  fun merge() {
    val mr = detail.mergeRequest.data ?: return
    if (detail.busy) return
    apply(mr.projectId, mr.iid) { it.copy(busy = true) }
    host.session.launch {
      host.api { host.repository.mergeMergeRequest(it, mr) }.fold(
        onSuccess = { updated ->
          replace(updated)
          host.notify("Merged !${updated.iid}")
        },
        onFailure = { error ->
          apply(mr.projectId, mr.iid) { it.copy(busy = false) }
          host.notifyError(error.userMessage())
        }
      )
    }
  }

  fun postNote(body: String, onResult: (String?) -> Unit) {
    val d = detail
    val mr = d.mergeRequest.data ?: return
    if (d.posting) return
    apply(mr.projectId, mr.iid) { it.copy(posting = true) }
    host.session.launch {
      host.api { host.repository.createMergeRequestNote(it, mr.projectId, mr.iid, body) }.fold(
        onSuccess = { note ->
          apply(mr.projectId, mr.iid) { it.copy(posting = false, notes = it.notes.copy(data = it.notes.data + note)) }
          onResult(null)
        },
        onFailure = { error ->
          apply(mr.projectId, mr.iid) { it.copy(posting = false) }
          onResult(error.userMessage())
        }
      )
    }
  }

  fun create(projectId: Long, title: String, source: String, target: String, description: String?, onResult: (String?) -> Unit) {
    host.session.launch {
      host.api {
        host.repository.createMergeRequest(it, projectId, title.trim(), source.trim(), target.trim(), description?.takeIf { d -> d.isNotBlank() })
      }.fold(
        onSuccess = { mr ->
          host.state.update { s ->
            s.copy(mergeRequests = s.mergeRequests.copy(data = listOf(mr) + s.mergeRequests.data.filterNot { it.projectId == mr.projectId && it.iid == mr.iid }))
          }
          onResult(null)
          host.notify("Merge request !${mr.iid} created")
        },
        onFailure = { error -> onResult(error.userMessage()) }
      )
    }
  }

  private fun replace(updated: GitLabMergeRequest) {
    host.state.update { s ->
      fun List<GitLabMergeRequest>.swap() = map { if (it.projectId == updated.projectId && it.iid == updated.iid) updated else it }
      s.copy(
        mergeRequests = s.mergeRequests.copy(data = s.mergeRequests.data.swap()),
        project = s.project.copy(mergeRequests = s.project.mergeRequests.copy(data = s.project.mergeRequests.data.swap())),
        mergeRequestDetail = if (s.mergeRequestDetail.projectId == updated.projectId && s.mergeRequestDetail.iid == updated.iid) {
          s.mergeRequestDetail.copy(mergeRequest = s.mergeRequestDetail.mergeRequest.success(updated), busy = false)
        } else s.mergeRequestDetail,
      )
    }
  }

  private fun apply(projectId: Long, iid: Long, transform: (MergeRequestDetailState) -> MergeRequestDetailState) {
    host.state.update { s ->
      if (s.mergeRequestDetail.projectId == projectId && s.mergeRequestDetail.iid == iid) s.copy(mergeRequestDetail = transform(s.mergeRequestDetail)) else s
    }
  }
}
