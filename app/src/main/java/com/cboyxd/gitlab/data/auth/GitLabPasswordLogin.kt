package com.cboyxd.gitlab.data.auth

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class PasswordLoginOutcome {
  data class TokenReady(val token: String) : PasswordLoginOutcome()
  data object NeedsTwoFactor : PasswordLoginOutcome()
  data class Failed(val message: String) : PasswordLoginOutcome()
}

class GitLabPasswordLogin(
  private val http: OkHttpClient = defaultClient()
) {
  private var root: String = ""
  private var pendingCsrf: String? = null
  private var pendingAction: String? = null

  fun signIn(serverUrl: String, username: String, password: String): PasswordLoginOutcome = guard {
    root = GitLabAuth.rootUrl(serverUrl)
    val page = get("$root/users/sign_in")
    if (!sameServer(page.url)) {
      return@guard PasswordLoginOutcome.Failed("This server sends sign-in to another site.")
    }
    if (GitLabAuth.pageRequiresCaptcha(page.body)) {
      return@guard PasswordLoginOutcome.Failed("This server asks for a captcha, so the app cannot sign in for you.")
    }
    if (!GitLabAuth.pageHasPasswordForm(page.body)) {
      return@guard PasswordLoginOutcome.Failed("This server has no username and password form.")
    }
    val csrf = GitLabAuth.csrfToken(page.body)
      ?: return@guard PasswordLoginOutcome.Failed("GitLab did not send a sign-in form.")
    val posted = postForm(
      "$root/users/sign_in",
      listOf(
        "authenticity_token" to csrf,
        "user[login]" to username,
        "user[password]" to password,
        "user[remember_me]" to "0"
      ),
      referer = "$root/users/sign_in"
    )
    continueAfterCredentials(posted)
  }

  fun submitOtp(code: String): PasswordLoginOutcome = guard {
    val csrf = pendingCsrf ?: return@guard PasswordLoginOutcome.Failed("Sign in again, then enter the code.")
    val action = GitLabAuth.resolveUrl(root, pendingAction)
    val posted = postForm(
      action,
      listOf(
        "authenticity_token" to csrf,
        "user[otp_attempt]" to code
      ),
      referer = action
    )
    continueAfterCredentials(posted)
  }

  private fun continueAfterCredentials(posted: Fetched): PasswordLoginOutcome {
    if (GitLabAuth.pageAsksForTwoFactor(posted.body)) {
      pendingCsrf = GitLabAuth.csrfToken(posted.body)
      pendingAction = GitLabAuth.formActionContaining(posted.body, "otp_attempt")
      if (pendingCsrf.isNullOrBlank()) {
        return PasswordLoginOutcome.Failed("GitLab asked for a second code, but the form could not be read.")
      }
      return PasswordLoginOutcome.NeedsTwoFactor
    }
    if (posted.url.contains("/users/password")) {
      return PasswordLoginOutcome.Failed("GitLab wants a new password. Change it in the browser, then sign in here again.")
    }
    if (posted.url.contains("/users/sign_in") && GitLabAuth.pageHasPasswordForm(posted.body)) {
      return PasswordLoginOutcome.Failed(GitLabAuth.alertMessage(posted.body) ?: "Invalid login or password.")
    }
    if (!sameServer(posted.url)) {
      return PasswordLoginOutcome.Failed("This server sends sign-in to another site.")
    }
    return createToken()
  }

  private fun createToken(): PasswordLoginOutcome {
    val paths = listOf(
      "$root/-/user_settings/personal_access_tokens",
      "$root/profile/personal_access_tokens"
    )
    var lastFailure = "GitLab did not create an access token."
    for (path in paths) {
      val page = get(path)
      if (page.code == 404) continue
      if (page.url.contains("/users/sign_in")) {
        return PasswordLoginOutcome.Failed("GitLab did not keep the sign-in session.")
      }
      val csrf = GitLabAuth.csrfToken(page.body)
        ?: return PasswordLoginOutcome.Failed("Signed in, but the token page could not be read.")
      val created = requestToken(path, csrf, GitLabAuth.tokenExpiryDate(364))
      created.token?.let { return PasswordLoginOutcome.TokenReady(it) }
      if (created.sessionLost) return PasswordLoginOutcome.Failed("GitLab did not keep the sign-in session.")
      if (created.expiryRejected) {
        val shorter = requestToken(path, csrf, GitLabAuth.tokenExpiryDate(30))
        shorter.token?.let { return PasswordLoginOutcome.TokenReady(it) }
        if (shorter.message != null) lastFailure = shorter.message
      } else if (created.message != null) {
        lastFailure = created.message
      }
      val nested = requestTokenForm(path, csrf, GitLabAuth.tokenExpiryDate(364))
      nested.token?.let { return PasswordLoginOutcome.TokenReady(it) }
      if (nested.message != null) lastFailure = nested.message
      return PasswordLoginOutcome.Failed(lastFailure)
    }
    return PasswordLoginOutcome.Failed(lastFailure)
  }

  private fun requestToken(url: String, csrf: String, expiry: String): TokenAttempt {
    // Rails reads params.require(:personal_access_token), so the fields go under that key.
    val json = """{"personal_access_token":{"name":"gitlab-mobile","description":"Signed in from gitlab-mobile","expires_at":"$expiry","scopes":["api"]}}"""
    val response = postJson(url, csrf, json)
    return readTokenAttempt(response)
  }

  private fun requestTokenForm(url: String, csrf: String, expiry: String): TokenAttempt {
    val response = postForm(
      url,
      listOf(
        "authenticity_token" to csrf,
        "personal_access_token[name]" to "gitlab-mobile",
        "personal_access_token[description]" to "Signed in from gitlab-mobile",
        "personal_access_token[expires_at]" to expiry,
        "personal_access_token[scopes][]" to "api"
      ),
      referer = url,
      csrf = csrf,
      acceptJson = true
    )
    return readTokenAttempt(response)
  }

  private fun readTokenAttempt(response: Fetched): TokenAttempt {
    GitLabAuth.createdAccessToken(response.body)?.let { return TokenAttempt(token = it) }
    if (response.code == 401 || response.url.contains("/users/sign_in")) {
      return TokenAttempt(sessionLost = true)
    }
    return TokenAttempt(
      message = GitLabAuth.accessError(response.body) ?: "GitLab did not create an access token (${response.code}).",
      expiryRejected = GitLabAuth.expiryRejected(response.body)
    )
  }

  private fun sameServer(url: String): Boolean {
    if (!url.startsWith("https://", ignoreCase = true)) return false
    val expected = root.substringAfter("://").substringBefore('/')
    val actual = url.substringAfter("://").substringBefore('/')
    return actual.equals(expected, true)
  }

  private fun get(url: String): Fetched {
    val request = Request.Builder()
      .url(url)
      .header("User-Agent", USER_AGENT)
      .header("Accept", "text/html,application/xhtml+xml")
      .get()
      .build()
    return execute(request)
  }

  private fun postForm(
    url: String,
    fields: List<Pair<String, String>>,
    referer: String,
    csrf: String? = null,
    acceptJson: Boolean = false
  ): Fetched {
    val body = FormBody.Builder().apply {
      fields.forEach { (key, value) -> add(key, value) }
    }.build()
    val builder = Request.Builder()
      .url(url)
      .header("User-Agent", USER_AGENT)
      .header("Accept", if (acceptJson) "application/json,text/html" else "text/html,application/xhtml+xml")
      .header("Referer", referer)
      .header("Origin", root)
      .post(body)
    if (!csrf.isNullOrBlank()) {
      builder.header("X-CSRF-Token", csrf)
      builder.header("X-Requested-With", "XMLHttpRequest")
    }
    return execute(builder.build())
  }

  private fun postJson(url: String, csrf: String, json: String): Fetched {
    val request = Request.Builder()
      .url(url)
      .header("User-Agent", USER_AGENT)
      .header("Accept", "application/json")
      .header("Referer", url)
      .header("Origin", root)
      .header("X-CSRF-Token", csrf)
      .header("X-Requested-With", "XMLHttpRequest")
      .post(json.toRequestBody(JSON))
      .build()
    return execute(request)
  }

  private fun execute(request: Request): Fetched {
    http.newCall(request).execute().use { response ->
      return Fetched(
        code = response.code,
        url = response.request.url.toString(),
        body = response.body?.string().orEmpty()
      )
    }
  }

  private inline fun guard(block: () -> PasswordLoginOutcome): PasswordLoginOutcome = try {
    block()
  } catch (error: IOException) {
    PasswordLoginOutcome.Failed("Unable to reach the server. Check the address and your connection.")
  } catch (error: IllegalArgumentException) {
    PasswordLoginOutcome.Failed(error.message ?: "Enter a GitLab server URL.")
  }

  private data class Fetched(val code: Int, val url: String, val body: String)

  private data class TokenAttempt(
    val token: String? = null,
    val message: String? = null,
    val sessionLost: Boolean = false,
    val expiryRejected: Boolean = false
  )

  companion object {
    private const val USER_AGENT = "GitLabMobile/1.0 (Android)"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
      .cookieJar(MemoryCookieJar())
      .followRedirects(true)
      .followSslRedirects(true)
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .build()
  }
}

private class MemoryCookieJar : CookieJar {
  private val cookies = mutableListOf<Cookie>()

  override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
    cookies.forEach { cookie ->
      this.cookies.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
      this.cookies += cookie
    }
  }

  override fun loadForRequest(url: HttpUrl): List<Cookie> = cookies.filter { it.matches(url) }
}
