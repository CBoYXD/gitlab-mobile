package com.cboyxd.gitlab.data.auth

import com.cboyxd.gitlab.data.model.GitLabInstance
import com.cboyxd.gitlab.data.storage.InstanceStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** UI and notification workers share one refresh lock: GitLab refresh tokens are single use. */
object GitLabSessionTokens {
  private val lock = Mutex()

  suspend fun fresh(store: InstanceStore, instanceId: String, force: Boolean = false, staleToken: String? = null): GitLabInstance? = lock.withLock {
    withContext(Dispatchers.IO + NonCancellable) {
      val latest = store.getInstances().firstOrNull { it.id == instanceId } ?: return@withContext null
      if (latest.authKind != GitLabAuth.KIND_OAUTH || latest.refreshToken.isBlank()) return@withContext latest
      if (staleToken != null && latest.token != staleToken) return@withContext latest
      if (!force && latest.tokenExpiresAt > System.currentTimeMillis() / 1000 + 60) return@withContext latest
      val clientId = latest.oauthClientId.ifBlank { store.getGitLabComClientId() }
      if (clientId.isBlank()) return@withContext latest
      val tokens = GitLabOAuth.refresh(latest.url, clientId, latest.refreshToken)
      val updated = latest.copy(
        token = tokens.accessToken,
        refreshToken = tokens.refreshToken.ifBlank { latest.refreshToken },
        tokenExpiresAt = tokens.expiresAtEpochSec,
        oauthClientId = clientId,
      )
      if (store.updateInstance(updated)) store.getInstances().firstOrNull { it.id == instanceId } else null
    }
  }
}
