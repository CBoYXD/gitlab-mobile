package com.cboyxd.gitlab.ui.viewmodel

import com.cboyxd.gitlab.data.model.GitLabJob
import com.cboyxd.gitlab.data.model.GitLabPipeline
import com.cboyxd.gitlab.data.repository.JobTrace
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val ACTIVE = setOf("running", "pending", "created", "preparing", "waiting_for_resource", "scheduled", "canceling")
private const val POLL_MS = 8_000L

/** Pipeline detail with its jobs, and job detail with the log. Active ones refresh while open. */
class PipelineFeature internal constructor(private val host: SessionHost) {

  private var pipelineJob: Job? = null
  private var jobJob: Job? = null

  internal fun reset() {
    pipelineJob?.cancel()
    jobJob?.cancel()
  }

  /** Stops polling when the user leaves the screen. */
  internal fun onDestinationChanged(destination: ScreenDestination) {
    if (destination !is ScreenDestination.PipelineDetail) pipelineJob?.cancel()
    if (destination !is ScreenDestination.JobDetail) jobJob?.cancel()
  }

  fun openPipeline(projectId: Long, pipelineId: Long) = host.navigateTo(ScreenDestination.PipelineDetail(projectId, pipelineId))

  fun openJob(projectId: Long, jobId: Long) = host.navigateTo(ScreenDestination.JobDetail(projectId, jobId))

  internal fun loadPipeline(projectId: Long, pipelineId: Long) {
    pipelineJob?.cancel()
    host.state.update { s ->
      val known = (s.pipelines.data + s.project.pipelines.data + s.mergeRequestDetail.pipelines.data).firstOrNull { it.id == pipelineId }
      s.copy(
        pipelineDetail = PipelineDetailState(
          projectId, pipelineId,
          pipeline = Loadable<GitLabPipeline?>(known, loaded = known != null).start(),
          jobs = emptyFeed<GitLabJob>().start(),
        )
      )
    }
    pipelineJob = host.session.launch {
      while (true) {
        val pipeline = host.api { host.repository.getPipeline(it, projectId, pipelineId) }
        val jobs = host.api { host.repository.getPipelineJobs(it, projectId, pipelineId) }
        host.state.update { s ->
          if (s.pipelineDetail.pipelineId != pipelineId) s
          else s.copy(pipelineDetail = s.pipelineDetail.copy(
            pipeline = s.pipelineDetail.pipeline.settle(pipeline.map { it as GitLabPipeline? }),
            jobs = s.pipelineDetail.jobs.settle(jobs),
          ))
        }
        if (pipeline.getOrNull()?.status !in ACTIVE) break
        delay(POLL_MS)
      }
    }
  }

  fun retryLoadPipeline() {
    val d = host.state.value.pipelineDetail
    if (d.projectId > 0) loadPipeline(d.projectId, d.pipelineId)
  }

  internal fun loadJob(projectId: Long, jobId: Long) {
    jobJob?.cancel()
    host.state.update { s ->
      // Seed from the pipeline's job list so the header shows at once (and even when the job
      // endpoints need an account, as on GitLab.com).
      val known = s.pipelineDetail.jobs.data.firstOrNull { it.id == jobId }
      s.copy(jobDetail = JobDetailState(projectId, jobId, Loadable<GitLabJob?>(known, loaded = known != null).start(), Loadable<JobTrace?>(null).start()))
    }
    jobJob = host.session.launch {
      while (true) {
        val job = host.api { host.repository.getJob(it, projectId, jobId) }
        val trace = host.api { host.repository.getJobTrace(it, projectId, jobId) }
        host.state.update { s ->
          if (s.jobDetail.jobId != jobId) s
          else s.copy(jobDetail = s.jobDetail.copy(
            job = s.jobDetail.job.settle(job.map { it as GitLabJob? }),
            trace = s.jobDetail.trace.settle(trace.map { it as JobTrace? }),
          ))
        }
        if (job.getOrNull()?.status !in ACTIVE) break
        delay(POLL_MS)
      }
    }
  }

  fun retryLoadJob() {
    val d = host.state.value.jobDetail
    if (d.projectId > 0) loadJob(d.projectId, d.jobId)
  }

  fun retryJob() {
    val d = host.state.value.jobDetail
    val job = d.job.data ?: return
    if (d.busy) return
    host.state.update { it.copy(jobDetail = it.jobDetail.copy(busy = true)) }
    host.session.launch {
      host.api { host.repository.retryJob(it, d.projectId, job.id) }.fold(
        onSuccess = { retried ->
          host.state.update { it.copy(jobDetail = it.jobDetail.copy(busy = false)) }
          host.notify("Started job #${retried.id}")
          openJob(d.projectId, retried.id)
        },
        onFailure = { error ->
          host.state.update { it.copy(jobDetail = it.jobDetail.copy(busy = false)) }
          host.notifyError(error.userMessage())
        }
      )
    }
  }

  fun cancelPipeline() {
    val d = host.state.value.pipelineDetail
    val pipeline = d.pipeline.data ?: return
    if (d.busy) return
    host.state.update { it.copy(pipelineDetail = it.pipelineDetail.copy(busy = true)) }
    host.session.launch {
      host.api { host.repository.cancelPipeline(it, d.projectId, pipeline.id) }.fold(
        onSuccess = { canceled ->
          host.state.update { s ->
            s.copy(pipelineDetail = s.pipelineDetail.copy(busy = false, pipeline = s.pipelineDetail.pipeline.success(canceled.copy(stages = pipeline.stages))))
          }
          host.notify("Pipeline #${pipeline.id} canceled")
        },
        onFailure = { error ->
          host.state.update { it.copy(pipelineDetail = it.pipelineDetail.copy(busy = false)) }
          host.notifyError(error.userMessage())
        }
      )
    }
  }
}
