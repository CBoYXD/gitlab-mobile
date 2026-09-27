package com.cboyxd.gitlab.ui.viewmodel

import com.cboyxd.gitlab.data.api.GitLabApiException
import com.cboyxd.gitlab.data.model.GitLabInstance
import com.cboyxd.gitlab.data.repository.GitLabRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What a feature needs from the view model: the active server session and shared state.
 * [session] is replaced whenever the active server changes, so work launched in it can never
 * write a previous server's data into the new one's state.
 */
internal interface SessionHost {
  val repository: GitLabRepository
  val session: CoroutineScope
  val state: MutableStateFlow<GitLabUiState>

  /** Runs a request against the active server, refreshing an expired OAuth token once. */
  suspend fun <T> api(block: suspend (GitLabInstance) -> Result<T>): Result<T>

  fun notify(text: String)
  fun notifyError(text: String)
  fun navigateTo(destination: ScreenDestination)
  fun openExternal(url: String)
}

/**
 * Cancels [previous], applies [start] to the state, runs [request] and hands the result to
 * [finish]. State updates are pure: the request never runs inside `update {}`.
 */
internal fun <T> SessionHost.load(
  previous: Job?,
  start: (GitLabUiState) -> GitLabUiState,
  request: suspend (GitLabInstance) -> Result<T>,
  finish: (GitLabUiState, Result<T>) -> GitLabUiState,
): Job {
  previous?.cancel()
  state.update(start)
  return session.launch {
    val result = api(request)
    state.update { finish(it, result) }
  }
}

/** Applies a finished request to a [Loadable]. */
internal fun <T> Loadable<T>.settle(result: Result<T>): Loadable<T> =
  result.fold(onSuccess = { success(it) }, onFailure = { failure(it.userMessage()) })

internal fun Throwable?.isUnauthorized(): Boolean = this is GitLabApiException && code == 401

internal fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Try again."
