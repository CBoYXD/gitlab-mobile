package com.cboyxd.gitlab.data.auth

import com.cboyxd.gitlab.data.api.normalizeBaseUrl
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object GitLabAuth {
  const val KIND_PASSWORD = "password"
  const val KIND_OAUTH = "oauth"
  const val KIND_PAT = "pat"
  /** No credentials: public projects only, and nothing can be changed. */
  const val KIND_PUBLIC = "public"

  /**
   * What the app browses while no account is signed in: GitLab.com's public projects. It is never
   * saved and never listed as a server; signing in replaces it, removing the last account returns to it.
   */
  val GUEST = com.cboyxd.gitlab.data.model.GitLabInstance(
    id = "guest-gitlab-com",
    name = "GitLab.com",
    url = "https://gitlab.com",
    isActive = true,
    isCustom = false,
    statusOk = true,
    authKind = KIND_PUBLIC,
  )
  const val REDIRECT_URI = "com.cboyxd.gitlab://oauth"
  const val OAUTH_SCOPE = "api"
  const val PROVIDER_GOOGLE = "google_oauth2"
  const val PROVIDER_GITHUB = "github"

  fun isGitLabCom(url: String): Boolean = try {
    val host = normalizeBaseUrl(url).substringAfter("://").substringBefore('/').substringBefore(':')
    host.equals("gitlab.com", true) || host.equals("www.gitlab.com", true)
  } catch (_: Exception) {
    false
  }

  fun rootUrl(url: String): String = normalizeBaseUrl(url).trimEnd('/')

  fun codeChallenge(verifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
    return base64Url(digest)
  }

  fun randomVerifier(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return base64Url(bytes)
  }

  fun tokenExpiryDate(daysAhead: Int, nowMillis: Long = System.currentTimeMillis()): String {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.timeInMillis = nowMillis
    calendar.add(Calendar.DAY_OF_YEAR, daysAhead)
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(calendar.time)
  }

  fun csrfToken(html: String): String? {
    val patterns = listOf(
      Regex("""name=["']csrf-token["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
      Regex("""content=["']([^"']+)["']\s+name=["']csrf-token["']""", RegexOption.IGNORE_CASE),
      Regex("""name=["']authenticity_token["'][^>]*value=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
      Regex("""value=["']([^"']+)["'][^>]*name=["']authenticity_token["']""", RegexOption.IGNORE_CASE)
    )
    return patterns.firstNotNullOfOrNull { pattern ->
      pattern.find(html)?.groupValues?.get(1)?.let(::decodeHtml)?.takeIf { it.isNotBlank() }
    }
  }

  fun pageAsksForTwoFactor(html: String): Boolean =
    html.contains("user[otp_attempt]") || html.contains("otp_attempt")

  fun pageHasPasswordForm(html: String): Boolean =
    html.contains("user[password]") || html.contains("name=\"password\"") || html.contains("name='password'")

  fun pageRequiresCaptcha(html: String): Boolean {
    val lowered = html.lowercase(Locale.US)
    return lowered.contains("g-recaptcha") ||
      lowered.contains("h-captcha") ||
      lowered.contains("cf-turnstile") ||
      lowered.contains("arkose")
  }

  fun formActionContaining(html: String, field: String): String? {
    val forms = Regex("""<form\b[^>]*>.*?</form>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    return forms.findAll(html).firstNotNullOfOrNull { form ->
      if (!form.value.contains(field)) return@firstNotNullOfOrNull null
      Regex("""\baction=["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        .find(form.value)
        ?.groupValues
        ?.get(1)
    }
  }

  fun alertMessage(html: String): String? {
    val patterns = listOf(
      Regex("""class=["'][^"']*flash-alert[^"']*["'][^>]*>(.*?)</div>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
      Regex("""class=["'][^"']*gl-alert-body[^"']*["'][^>]*>(.*?)</""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
      Regex("""data-testid=["']alert-danger["'][^>]*>(.*?)</""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    )
    return patterns.firstNotNullOfOrNull { pattern ->
      val raw = pattern.find(html)?.groupValues?.get(1) ?: return@firstNotNullOfOrNull null
      raw.replace(Regex("<[^>]+>"), " ")
        .let(::decodeHtml)
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(180)
        .takeIf { it.isNotBlank() }
    }
  }

  fun createdAccessToken(body: String): String? {
    // GitLab 15+ answers {"new_token": "glpat-…"}; older versions used "token".
    val json = Regex(""""(?:new_)?token"\s*:\s*"([^"\\]+)"""").find(body)?.groupValues?.get(1)
    if (!json.isNullOrBlank()) return json
    return Regex("""value=["']((?:glpat|glptt|gldt)-[^"'\s]+)["']""")
      .find(body)
      ?.groupValues
      ?.get(1)
  }

  fun accessError(body: String): String? {
    val errors = Regex(""""errors"\s*:\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL).find(body)
    if (errors != null) {
      val parts = Regex(""""([^"\\]+)"""").findAll(errors.groupValues[1]).map { it.groupValues[1] }.toList()
      if (parts.isNotEmpty()) return parts.joinToString(". ")
    }
    return jsonString(body, "error_description") ?: jsonString(body, "message") ?: jsonString(body, "error")
  }

  fun expiryRejected(body: String): Boolean = body.lowercase(Locale.US).contains("expir")

  fun parseOAuthTokens(body: String, nowEpochSec: Long = System.currentTimeMillis() / 1000): OAuthTokenSet {
    val access = jsonString(body, "access_token")
      ?: throw GitLabAuthException(accessError(body) ?: "GitLab did not return an access token.")
    val refresh = jsonString(body, "refresh_token").orEmpty()
    val created = jsonLong(body, "created_at") ?: nowEpochSec
    val expiresIn = jsonLong(body, "expires_in") ?: 7200L
    return OAuthTokenSet(access, refresh, created + expiresIn)
  }

  fun authorizationUrl(baseUrl: String, clientId: String, state: String, verifier: String, provider: String = ""): String {
    val root = rootUrl(baseUrl)
    val query = listOf(
      "client_id" to clientId,
      "redirect_uri" to REDIRECT_URI,
      "response_type" to "code",
      "state" to state,
      "scope" to OAUTH_SCOPE,
      "code_challenge" to codeChallenge(verifier),
      "code_challenge_method" to "S256"
    ).joinToString("&") { (key, value) -> "$key=${encode(value)}" }
    val origin = encode("/oauth/authorize?$query")
    return if (provider.isBlank()) "$root/oauth/authorize?$query"
    else "$root/users/auth/$provider?origin=$origin"
  }

  fun resolveUrl(root: String, action: String?): String {
    if (action.isNullOrBlank()) return "$root/users/sign_in"
    val trimmed = action.trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    if (trimmed.startsWith("/")) return root + trimmed
    return "$root/$trimmed"
  }

  private fun encode(value: String): String =
    URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

  private fun jsonString(body: String, key: String): String? =
    Regex(""""${Regex.escape(key)}"\s*:\s*"([^"\\]*)"""").find(body)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }

  private fun jsonLong(body: String, key: String): Long? =
    Regex(""""${Regex.escape(key)}"\s*:\s*(-?\d+)""").find(body)?.groupValues?.get(1)?.toLongOrNull()

  private fun decodeHtml(value: String): String = value
    .replace("&amp;", "&")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&lt;", "<")
    .replace("&gt;", ">")

  private fun base64Url(bytes: ByteArray): String {
    val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    val out = StringBuilder()
    var index = 0
    while (index < bytes.size) {
      val b0 = bytes[index].toInt() and 0xFF
      val b1 = if (index + 1 < bytes.size) bytes[index + 1].toInt() and 0xFF else 0
      val b2 = if (index + 2 < bytes.size) bytes[index + 2].toInt() and 0xFF else 0
      out.append(table[b0 shr 2])
      out.append(table[((b0 and 0x03) shl 4) or (b1 shr 4)])
      if (index + 1 < bytes.size) out.append(table[((b1 and 0x0F) shl 2) or (b2 shr 6)])
      if (index + 2 < bytes.size) out.append(table[b2 and 0x3F])
      index += 3
    }
    return out.toString()
  }
}

data class OAuthTokenSet(
  val accessToken: String,
  val refreshToken: String,
  val expiresAtEpochSec: Long
)

class GitLabAuthException(message: String) : Exception(message)
