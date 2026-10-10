package com.cboyxd.gitlab.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import coil.network.HttpException
import com.cboyxd.gitlab.GitLabApplication
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Retry only failed requests, preserving successful images across navigation. */
@Composable
fun AvatarImage(url: String, modifier: Modifier) {
  val model = rememberImageModel(url, avatar = true)
  val auth = LocalImageAuth.current
  val application = LocalContext.current.applicationContext as? GitLabApplication
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  var attempt by remember(url, auth) { mutableIntStateOf(0) }
  var failure by remember(url, auth) { mutableStateOf<Throwable?>(null) }

  LaunchedEffect(url, auth, failure, attempt, lifecycle) {
    val error = failure ?: return@LaunchedEffect
    val retryable = when (error) {
      is HttpException -> error.response.code in listOf(408, 425, 429) || error.response.code >= 500
      is IOException -> true
      else -> false
    }
    if (!retryable) return@LaunchedEffect
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      val generation = application?.networkChanges?.value
      val backoff = (2_000L shl attempt.coerceAtMost(5)).coerceAtMost(60_000L)
      withTimeoutOrNull(backoff) {
        if (application != null) application.networkChanges.first { it != generation && application.isOnline }
        else kotlinx.coroutines.delay(backoff)
      }
      if (application?.isOnline != false) {
        failure = null
        attempt += 1
      } else {
        application.networkChanges.first { application.isOnline }
        failure = null
        attempt += 1
      }
    }
  }

  // Coil 2 has no painter.restart(): recreating only the failed painter keeps cache keys stable.
  key(url, auth, attempt) {
    AsyncImage(
      model = model,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = modifier,
      onSuccess = { failure = null },
      onError = { failure = it.result.throwable },
    )
  }
}
