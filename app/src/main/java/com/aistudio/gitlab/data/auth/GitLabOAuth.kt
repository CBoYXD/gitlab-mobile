package com.aistudio.gitlab.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.aistudio.gitlab.data.storage.AndroidKeyStoreSecretStore
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

data class PendingOAuth(
  val state: String,
  val verifier: String,
  val name: String,
  val clientId: String,
  val baseUrl: String = "https://gitlab.com"
)

object GitLabOAuth {
  private const val PREFS = "gitlab_oauth_pending"
  private const val KEY_STATE = "state"
  private const val KEY_VERIFIER = "verifier"
  private const val KEY_NAME = "name"
  private const val KEY_CLIENT = "client_id"
  private const val KEY_BASE = "base_url"

  fun begin(name: String, clientId: String, baseUrl: String = "https://gitlab.com"): PendingOAuth = PendingOAuth(
    state = GitLabAuth.randomVerifier(),
    verifier = GitLabAuth.randomVerifier(),
    name = name.ifBlank { GitLabAuth.rootUrl(baseUrl).removePrefix("https://").trimEnd('/') },
    clientId = clientId.trim(),
    baseUrl = GitLabAuth.rootUrl(baseUrl)
  )

  fun authorizationUrl(pending: PendingOAuth, provider: String = ""): String =
    GitLabAuth.authorizationUrl(pending.baseUrl, pending.clientId, pending.state, pending.verifier, provider)

  fun savePending(context: Context, pending: PendingOAuth) {
    val secrets = AndroidKeyStoreSecretStore(context, PREFS)
    secrets.put(KEY_STATE, pending.state)
    secrets.put(KEY_VERIFIER, pending.verifier)
    secrets.put(KEY_NAME, pending.name)
    secrets.put(KEY_CLIENT, pending.clientId)
    secrets.put(KEY_BASE, pending.baseUrl)
  }

  fun consumePending(context: Context): PendingOAuth? {
    val secrets = AndroidKeyStoreSecretStore(context, PREFS)
    val state = secrets.get(KEY_STATE)
    val verifier = secrets.get(KEY_VERIFIER)
    val clientId = secrets.get(KEY_CLIENT)
    if (state.isNullOrBlank() || verifier.isNullOrBlank() || clientId.isNullOrBlank()) return null
    val pending = PendingOAuth(
      state = state,
      verifier = verifier,
      name = secrets.get(KEY_NAME).orEmpty().ifBlank { "GitLab.com" },
      clientId = clientId,
      baseUrl = secrets.get(KEY_BASE).orEmpty().ifBlank { "https://gitlab.com" }
    )
    secrets.clear()
    return pending
  }

  fun openBrowser(context: Context, pending: PendingOAuth, provider: String = "") {
    val tabs = CustomTabsIntent.Builder().setShowTitle(true).build()
    tabs.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    tabs.launchUrl(context, Uri.parse(authorizationUrl(pending, provider)))
  }

  fun exchange(pending: PendingOAuth, code: String, http: OkHttpClient = tokenClient()): OAuthTokenSet {
    val body = FormBody.Builder()
      .add("grant_type", "authorization_code")
      .add("client_id", pending.clientId)
      .add("code", code)
      .add("redirect_uri", GitLabAuth.REDIRECT_URI)
      .add("code_verifier", pending.verifier)
      .build()
    return postToken(pending.baseUrl, body, http)
  }

  fun refresh(baseUrl: String, clientId: String, refreshToken: String, http: OkHttpClient = tokenClient()): OAuthTokenSet {
    val body = FormBody.Builder()
      .add("grant_type", "refresh_token")
      .add("client_id", clientId)
      .add("refresh_token", refreshToken)
      .add("redirect_uri", GitLabAuth.REDIRECT_URI)
      .build()
    return postToken(baseUrl, body, http)
  }

  private fun postToken(baseUrl: String, body: FormBody, http: OkHttpClient): OAuthTokenSet {
    val request = Request.Builder()
      .url(GitLabAuth.rootUrl(baseUrl) + "/oauth/token")
      .header("Accept", "application/json")
      .header("User-Agent", "GitLabMobile/1.0 (Android)")
      .post(body)
      .build()
    try {
      http.newCall(request).execute().use { response ->
        val raw = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          throw GitLabAuthException(GitLabAuth.accessError(raw) ?: "GitLab sign-in failed (${response.code}).")
        }
        return GitLabAuth.parseOAuthTokens(raw)
      }
    } catch (error: IOException) {
      throw GitLabAuthException("Couldn't reach GitLab.")
    }
  }

  private fun tokenClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()
}
