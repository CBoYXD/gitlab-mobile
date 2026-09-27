package com.cboyxd.gitlab

import com.cboyxd.gitlab.data.auth.GitLabAuth
import com.cboyxd.gitlab.data.auth.GitLabOAuth
import com.cboyxd.gitlab.data.auth.GitLabPasswordLogin
import com.cboyxd.gitlab.data.auth.PasswordLoginOutcome
import com.cboyxd.gitlab.data.auth.PendingOAuth
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder
import java.net.URLEncoder

class GitLabAuthTest {

  @Test
  fun `pkce challenge matches the gitlab sample`() {
    val verifier = "ks02i3jdikdo2k0dkfodf3m39rjfjsdk0wk349rj3jrhf"
    assertEquals("2i0WFA-0AerkjQm4X4oDEhqA17QIAKNjXpagHBXmO_U", GitLabAuth.codeChallenge(verifier))
  }

  @Test
  fun `gitlab com is recognized and a custom host is not`() {
    assertTrue(GitLabAuth.isGitLabCom("https://gitlab.com"))
    assertTrue(GitLabAuth.isGitLabCom("gitlab.com/explore"))
    assertTrue(!GitLabAuth.isGitLabCom("https://gitlab.company.internal"))
    assertTrue(!GitLabAuth.isGitLabCom("https://"))
  }

  @Test
  fun `google sign-in url returns through the oauth origin`() {
    val pending = PendingOAuth(
      state = "state-1",
      verifier = "ks02i3jdikdo2k0dkfodf3m39rjfjsdk0wk349rj3jrhf",
      name = "GitLab.com",
      clientId = "app-id"
    )
    val url = GitLabOAuth.authorizationUrl(pending, GitLabAuth.PROVIDER_GOOGLE)
    assertTrue(url.startsWith("https://gitlab.com/users/auth/google_oauth2?origin="))
    val origin = URLDecoder.decode(url.substringAfter("origin="), Charsets.UTF_8.name())
    assertTrue(origin.startsWith("/oauth/authorize?"))
    assertTrue(origin.contains("client_id=app-id"))
    assertTrue(origin.contains("code_challenge=2i0WFA-0AerkjQm4X4oDEhqA17QIAKNjXpagHBXmO_U"))
    val redirect = URLDecoder.decode(origin.substringAfter("redirect_uri=").substringBefore("&"), Charsets.UTF_8.name())
    assertEquals(GitLabAuth.REDIRECT_URI, redirect)
    assertTrue(origin.contains("code_challenge_method=S256"))
  }

  @Test
  fun `oauth supports arbitrary gitlab instances without a provider`() {
    val pending = GitLabOAuth.begin("Company GitLab", "client", "https://gitlab.example.com/gitlab")
    val url = GitLabOAuth.authorizationUrl(pending)
    assertTrue(url.startsWith("https://gitlab.example.com/gitlab/oauth/authorize?"))
    assertTrue(url.contains("client_id=client"))
    assertTrue(url.contains("redirect_uri=" + URLEncoder.encode(GitLabAuth.REDIRECT_URI, Charsets.UTF_8.name())))
  }

  @Test
  fun `oauth token json is parsed`() {
    val tokens = GitLabAuth.parseOAuthTokens(
      """{"access_token":"access","token_type":"bearer","expires_in":7200,"refresh_token":"refresh","created_at":100}"""
    )
    assertEquals("access", tokens.accessToken)
    assertEquals("refresh", tokens.refreshToken)
    assertEquals(7300L, tokens.expiresAtEpochSec)
  }

  @Test
  fun `password sign-in creates a token from the session`() {
    val (server, client) = secureServer()
    try {
      server.enqueue(signInPage("csrf-1"))
      server.enqueue(
        MockResponse()
          .setResponseCode(302)
          .addHeader("Location", server.url("/").toString())
          .addHeader("Set-Cookie", "_gitlab_session=signed-in; Path=/")
      )
      server.enqueue(MockResponse().setBody("<html><body>Home</body></html>"))
      server.enqueue(tokenPage("csrf-pat"))
      server.enqueue(MockResponse().setBody("""{"token":"glpat-created"}""").addHeader("Content-Type", "application/json"))

      val outcome = GitLabPasswordLogin(client).signIn(server.url("/").toString(), "dev", "secret")

      assertTrue(outcome is PasswordLoginOutcome.TokenReady)
      assertEquals("glpat-created", (outcome as PasswordLoginOutcome.TokenReady).token)
      val tokenRequest = server.takeRequest().let { server.takeRequest(); server.takeRequest(); server.takeRequest(); server.takeRequest() }
      assertTrue(tokenRequest.path!!.contains("personal_access_tokens"))
      assertEquals("csrf-pat", tokenRequest.getHeader("X-CSRF-Token"))
      assertTrue(tokenRequest.body.readUtf8().contains("gitlab-mobile"))
      assertTrue(tokenRequest.getHeader("Cookie")!!.contains("_gitlab_session=signed-in"))
    } finally {
      server.shutdown()
    }
  }

  @Test
  fun `wrong password stays on the sign-in form`() {
    val (server, client) = secureServer()
    try {
      server.enqueue(signInPage("csrf-1"))
      server.enqueue(
        MockResponse().setBody(
          """
          <html><head><meta name="csrf-token" content="csrf-2" /></head>
          <body><div class="flash-alert">Invalid login or password.</div>
          <form action="/users/sign_in"><input name="user[password]" /></form></body></html>
          """.trimIndent()
        )
      )
      val outcome = GitLabPasswordLogin(client).signIn(server.url("/").toString(), "dev", "nope")
      assertTrue(outcome is PasswordLoginOutcome.Failed)
      assertEquals("Invalid login or password.", (outcome as PasswordLoginOutcome.Failed).message)
    } finally {
      server.shutdown()
    }
  }

  @Test
  fun `two factor code continues the same session`() {
    val (server, client) = secureServer()
    try {
      server.enqueue(signInPage("csrf-1"))
      server.enqueue(
        MockResponse().setBody(
          """
          <html><head><meta name="csrf-token" content="csrf-otp" /></head>
          <body><form action="/users/sign_in" method="post">
          <input type="hidden" name="authenticity_token" value="csrf-otp" />
          <input name="user[otp_attempt]" />
          </form></body></html>
          """.trimIndent()
        )
      )
      val login = GitLabPasswordLogin(client)
      val first = login.signIn(server.url("/").toString(), "dev", "secret")
      assertTrue(first is PasswordLoginOutcome.NeedsTwoFactor)

      server.enqueue(
        MockResponse()
          .setResponseCode(302)
          .addHeader("Location", server.url("/").toString())
          .addHeader("Set-Cookie", "_gitlab_session=after-otp; Path=/")
      )
      server.enqueue(MockResponse().setBody("<html><body>Home</body></html>"))
      server.enqueue(tokenPage("csrf-pat"))
      server.enqueue(MockResponse().setBody("""{"token":"glpat-otp"}"""))

      val second = login.submitOtp("123456")
      assertTrue(second is PasswordLoginOutcome.TokenReady)
      assertEquals("glpat-otp", (second as PasswordLoginOutcome.TokenReady).token)
    } finally {
      server.shutdown()
    }
  }

  private fun signInPage(csrf: String) = MockResponse().setBody(
    """
    <html><head><meta name="csrf-token" content="$csrf" /></head>
    <body><form action="/users/sign_in" method="post">
    <input type="hidden" name="authenticity_token" value="$csrf" />
    <input name="user[login]" />
    <input type="password" name="user[password]" />
    </form></body></html>
    """.trimIndent()
  )

  private fun tokenPage(csrf: String) = MockResponse().setBody(
    """<html><head><meta name="csrf-token" content="$csrf" /></head><body>Tokens</body></html>"""
  )

  private fun secureServer(): Pair<MockWebServer, OkHttpClient> {
    val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
    val certificates = HandshakeCertificates.Builder()
      .heldCertificate(certificate)
      .addTrustedCertificate(certificate.certificate)
      .build()
    val server = MockWebServer()
    server.useHttps(certificates.sslSocketFactory(), false)
    server.start()
    val client = OkHttpClient.Builder()
      .sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager)
      .cookieJar(object : CookieJar {
        private val cookies = mutableListOf<Cookie>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
          this.cookies.removeAll { old -> cookies.any { it.name == old.name } }
          this.cookies.addAll(cookies)
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> = cookies.filter { it.matches(url) }
      })
      .build()
    return server to client
  }
}
