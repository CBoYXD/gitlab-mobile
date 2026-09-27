package com.aistudio.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistudio.gitlab.data.auth.GitLabAuth
import com.aistudio.gitlab.data.model.GitLabInstance
import com.aistudio.gitlab.ui.components.EmptyState
import com.aistudio.gitlab.ui.components.GitLabTanukiIcon
import com.aistudio.gitlab.ui.components.GlBadge
import com.aistudio.gitlab.ui.components.GlButton
import com.aistudio.gitlab.ui.components.GlButtonKind
import com.aistudio.gitlab.ui.components.GlCard
import com.aistudio.gitlab.ui.components.GlChip
import com.aistudio.gitlab.ui.components.GlDivider
import com.aistudio.gitlab.ui.components.GlIconAction
import com.aistudio.gitlab.ui.components.GlRow
import com.aistudio.gitlab.ui.components.GlSectionHeader
import com.aistudio.gitlab.ui.components.GlTopBar
import com.aistudio.gitlab.ui.components.InlineError
import com.aistudio.gitlab.ui.components.MonoPill
import com.aistudio.gitlab.ui.components.Tone
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlSpace
import com.aistudio.gitlab.ui.viewmodel.LoginState
import kotlinx.coroutines.launch

@Composable
fun InstancesScreen(
  activeInstance: GitLabInstance?,
  savedInstances: List<GitLabInstance>,
  loginState: LoginState,
  savedClientId: String,
  openAddOnStart: Boolean,
  onAddOpened: () -> Unit,
  onSwitchInstance: (String) -> Unit,
  onDeleteInstance: (String) -> Unit,
  onSignInWithPassword: (name: String, url: String, username: String, password: String) -> Unit,
  onSubmitTwoFactor: (String) -> Unit,
  onSignInWithOAuth: (name: String, url: String, clientId: String) -> Unit,
  onSignInWithToken: (name: String, url: String, token: String) -> Unit,
  onConnectPublic: (name: String, url: String) -> Unit,
  onClearLogin: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showAdd by rememberSaveable { mutableStateOf(false) }
  var pendingRemoval by remember { mutableStateOf<GitLabInstance?>(null) }

  LaunchedEffect(openAddOnStart) {
    if (openAddOnStart) {
      onClearLogin()
      showAdd = true
      onAddOpened()
    }
  }

  val openAdd = {
    onClearLogin()
    showAdd = true
  }

  Column(modifier = modifier.fillMaxSize().background(Gl.colors.bg)) {
    GlTopBar(title = "Servers", onBack = onBack) {
      GlIconAction(icon = Icons.Outlined.Add, contentDescription = "Add server", onClick = openAdd, modifier = Modifier.testTag("add_instance_button"))
    }
    if (savedInstances.isEmpty()) {
      EmptyState(
        icon = Icons.Outlined.Dns,
        title = "No servers yet",
        body = "Add GitLab.com or your company's GitLab server to get started.",
        actionLabel = "Add server",
        onAction = openAdd,
      )
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = GlSpace.group),
      ) {
        if (activeInstance != null) {
          item(key = "current") {
            GlSectionHeader(title = "Current server", modifier = Modifier.padding(top = 0.dp))
            CurrentServerCard(activeInstance, Modifier.padding(horizontal = GlSpace.margin))
          }
        }
        item(key = "saved") {
          GlSectionHeader(title = "Saved servers")
          GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin)) {
            savedInstances.forEachIndexed { index, instance ->
              if (index > 0) GlDivider(inset = 52.dp)
              val active = instance.id == activeInstance?.id
              GlRow(
                title = instance.name,
                subtitle = listOfNotNull(
                  instance.username.takeIf { it.isNotBlank() }?.let { "@$it" },
                  hostOf(instance.url),
                ).joinToString(" · "),
                leading = { GitLabTanukiIcon(size = 24.dp) },
                trailing = {
                  if (active) {
                    Icon(Icons.Outlined.Check, contentDescription = "Current server", tint = Gl.colors.textPrimary)
                  }
                  GlIconAction(
                    icon = Icons.Outlined.DeleteOutline,
                    contentDescription = "Remove ${instance.name}",
                    onClick = { pendingRemoval = instance },
                    tint = Gl.colors.textSecondary,
                  )
                },
                onClick = if (active) null else ({ onSwitchInstance(instance.id) }),
                onClickLabel = "Switch to ${instance.name}",
                modifier = Modifier.testTag("instance_row_${instance.id}"),
              )
            }
          }
        }
        item(key = "help") {
          GlSectionHeader(title = "Ways to sign in")
          GlCard(modifier = Modifier.padding(horizontal = GlSpace.margin), contentPadding = PaddingValues(GlSpace.margin)) {
            HelpLine("Access token", "Works on every server. Create one in GitLab with the api scope and paste it here.")
            Spacer(Modifier.height(12.dp))
            HelpLine("OAuth", "Signs in through your browser, including SSO. The server needs an OAuth application for this app.")
            Spacer(Modifier.height(12.dp))
            HelpLine("Password", "For self-hosted servers that allow it. The app creates an access token for you.")
          }
        }
      }
    }
  }

  pendingRemoval?.let { instance ->
    AlertDialog(
      onDismissRequest = { pendingRemoval = null },
      containerColor = Gl.colors.surface,
      title = { Text("Remove ${instance.name}?", style = Gl.type.sectionTitle, color = Gl.colors.textPrimary) },
      text = {
        Text(
          "The app signs out of this server and forgets its saved access token. This doesn't delete anything on the server.",
          style = Gl.type.bodySmall,
          color = Gl.colors.textSecondary,
        )
      },
      confirmButton = {
        GlButton(
          text = "Remove server",
          kind = GlButtonKind.Danger,
          onClick = {
            pendingRemoval = null
            onDeleteInstance(instance.id)
          },
        )
      },
      dismissButton = {
        GlButton(text = "Cancel", kind = GlButtonKind.Ghost, onClick = { pendingRemoval = null })
      },
    )
  }

  if (showAdd) {
    AddServerSheet(
      loginState = loginState,
      savedClientId = savedClientId,
      onDismiss = {
        showAdd = false
        onClearLogin()
      },
      onSignInWithPassword = onSignInWithPassword,
      onSubmitTwoFactor = onSubmitTwoFactor,
      onSignInWithOAuth = onSignInWithOAuth,
      onSignInWithToken = onSignInWithToken,
      onConnectPublic = onConnectPublic,
    )
  }
}

@Composable
private fun CurrentServerCard(instance: GitLabInstance, modifier: Modifier = Modifier) {
  val method = when {
    instance.authKind == GitLabAuth.KIND_OAUTH -> "OAuth"
    instance.authKind == GitLabAuth.KIND_PASSWORD -> "Password"
    instance.token.isNotBlank() -> "Access token"
    else -> "No sign-in"
  }
  val (statusText, tone) = when {
    instance.token.isBlank() -> "Not signed in" to Tone.Neutral
    instance.statusOk -> "Connected" to Tone.Success
    else -> "Unreachable" to Tone.Danger
  }
  GlCard(modifier = modifier.testTag("active_instance_card"), contentPadding = PaddingValues(GlSpace.margin)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      GitLabTanukiIcon(size = 32.dp)
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(instance.name, style = Gl.type.rowTitle, color = Gl.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(instance.url, style = Gl.type.meta, color = Gl.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Spacer(Modifier.width(8.dp))
      GlBadge(text = statusText, tone = tone)
    }
    Spacer(Modifier.height(12.dp))
    Text(
      text = listOfNotNull(
        instance.username.takeIf { it.isNotBlank() }?.let { "@$it" },
        instance.version.takeIf { it.isNotBlank() }?.let { "GitLab $it" },
        method,
      ).joinToString(" · "),
      style = Gl.type.meta,
      color = Gl.colors.textSecondary,
    )
  }
}

@Composable
private fun HelpLine(title: String, body: String) {
  Column {
    Text(title, style = Gl.type.label, color = Gl.colors.textPrimary)
    Spacer(Modifier.height(2.dp))
    Text(body, style = Gl.type.meta, color = Gl.colors.textSecondary)
  }
}

private enum class SignInMethod(val label: String) { TOKEN("Access token"), OAUTH("OAuth"), PASSWORD("Password"), PUBLIC("No sign-in") }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddServerSheet(
  loginState: LoginState,
  savedClientId: String,
  onDismiss: () -> Unit,
  onSignInWithPassword: (name: String, url: String, username: String, password: String) -> Unit,
  onSubmitTwoFactor: (String) -> Unit,
  onSignInWithOAuth: (name: String, url: String, clientId: String) -> Unit,
  onSignInWithToken: (name: String, url: String, token: String) -> Unit,
  onConnectPublic: (name: String, url: String) -> Unit,
) {
  val busy = loginState.inProgress
  val busyState = rememberUpdatedState(busy)
  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true,
    confirmValueChange = { !busyState.value },
  )
  val scope = rememberCoroutineScope()
  val clipboard = LocalClipboardManager.current

  var url by rememberSaveable { mutableStateOf("https://gitlab.com") }
  var name by rememberSaveable { mutableStateOf("") }
  var method by rememberSaveable { mutableStateOf(SignInMethod.TOKEN) }
  var token by rememberSaveable { mutableStateOf("") }
  var showToken by rememberSaveable { mutableStateOf(false) }
  var clientId by rememberSaveable { mutableStateOf(savedClientId) }
  var username by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
  var showPassword by rememberSaveable { mutableStateOf(false) }
  var otp by rememberSaveable { mutableStateOf("") }

  var urlError by remember { mutableStateOf<String?>(null) }
  var fieldError by remember { mutableStateOf<String?>(null) }
  val urlFocus = remember { FocusRequester() }
  val fieldFocus = remember { FocusRequester() }

  LaunchedEffect(loginState.finished) {
    if (loginState.finished) {
      scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
  }

  fun validateUrl(): Boolean {
    val ok = url.trim().startsWith("https://", ignoreCase = true) && url.trim().length > "https://".length
    urlError = if (ok) null else "Enter an address that starts with https://"
    if (!ok) urlFocus.requestFocus()
    return ok
  }

  fun requireField(value: String, message: String): Boolean {
    val ok = value.isNotBlank()
    fieldError = if (ok) null else message
    if (!ok) fieldFocus.requestFocus()
    return ok
  }

  fun submit() {
    fieldError = null
    if (!validateUrl()) return
    when (method) {
      SignInMethod.TOKEN -> if (requireField(token, "Paste an access token with the api scope.")) {
        onSignInWithToken(name.trim(), url.trim(), token.trim())
      }
      SignInMethod.OAUTH -> if (requireField(clientId, "Enter the application ID from your GitLab server.")) {
        onSignInWithOAuth(name.trim(), url.trim(), clientId.trim())
      }
      SignInMethod.PUBLIC -> onConnectPublic(name.trim(), url.trim())
      SignInMethod.PASSWORD -> if (loginState.needsTwoFactor) {
        if (requireField(otp, "Enter the code from your authenticator app.")) onSubmitTwoFactor(otp.trim())
      } else {
        if (username.isBlank()) {
          requireField(username, "Enter your username or email.")
        } else if (password.isEmpty()) {
          fieldError = "Enter your password."
        } else {
          onSignInWithPassword(name.trim(), url.trim(), username.trim(), password)
        }
      }
    }
  }

  val fieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Gl.colors.textPrimary,
    unfocusedBorderColor = Gl.colors.borderStrong,
    focusedLabelColor = Gl.colors.textPrimary,
    unfocusedLabelColor = Gl.colors.textSecondary,
    cursorColor = Gl.colors.textPrimary,
    errorBorderColor = Gl.colors.danger,
    errorLabelColor = Gl.colors.danger,
    errorSupportingTextColor = Gl.colors.danger,
    focusedSupportingTextColor = Gl.colors.textSecondary,
    unfocusedSupportingTextColor = Gl.colors.textSecondary,
  )

  ModalBottomSheet(
    onDismissRequest = { if (!busy) onDismiss() },
    sheetState = sheetState,
    containerColor = Gl.colors.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .imePadding()
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = GlSpace.margin)
        .padding(bottom = GlSpace.group),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text("Add server", style = Gl.type.title, color = Gl.colors.textPrimary)

      OutlinedTextField(
        value = url,
        onValueChange = { url = it; urlError = null },
        label = { Text("Server address") },
        placeholder = { Text("https://gitlab.example.com") },
        isError = urlError != null,
        supportingText = supporting(urlError),
        singleLine = true,
        enabled = !busy && !loginState.needsTwoFactor,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        colors = fieldColors,
        modifier = Modifier.fillMaxWidth().focusRequester(urlFocus).testTag("instance_url_input"),
      )
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Display name (optional)") },
        placeholder = { Text(if (GitLabAuth.isGitLabCom(url)) "GitLab.com" else "Company GitLab") },
        supportingText = { Text("Shown in the server list") },
        singleLine = true,
        enabled = !busy,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        colors = fieldColors,
        modifier = Modifier.fillMaxWidth().testTag("instance_name_input"),
      )

      Text("Sign-in method", style = Gl.type.label, color = Gl.colors.textPrimary)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(GlSpace.tight)) {
        SignInMethod.entries.forEach { option ->
          GlChip(
            text = option.label,
            selected = method == option,
            onClick = {
              if (!busy && !loginState.needsTwoFactor) {
                method = option
                fieldError = null
              }
            },
          )
        }
      }

      when (method) {
        SignInMethod.TOKEN -> {
          Explanation("Create a personal access token with the api scope in GitLab under Preferences → Access tokens.")
          OutlinedTextField(
            value = token,
            onValueChange = { token = it; fieldError = null },
            label = { Text("Access token") },
            placeholder = { Text("glpat-…") },
            isError = fieldError != null,
            supportingText = supporting(fieldError),
            singleLine = true,
            enabled = !busy,
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(onClick = { showToken = !showToken }) {
                Icon(
                  if (showToken) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                  contentDescription = if (showToken) "Hide token" else "Show token",
                  tint = Gl.colors.textSecondary,
                )
              }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus).testTag("instance_token_input"),
          )
        }
        SignInMethod.OAUTH -> {
          Explanation("On the server, register an application for this app: turn off Confidential, select the api scope, and use this redirect URI.")
          Row(verticalAlignment = Alignment.CenterVertically) {
            MonoPill(GitLabAuth.REDIRECT_URI, Modifier.weight(1f, fill = false))
            GlIconAction(
              icon = Icons.Outlined.ContentCopy,
              contentDescription = "Copy redirect URI",
              onClick = { clipboard.setText(AnnotatedString(GitLabAuth.REDIRECT_URI)) },
              tint = Gl.colors.textSecondary,
            )
          }
          OutlinedTextField(
            value = clientId,
            onValueChange = { clientId = it; fieldError = null },
            label = { Text("Application ID") },
            isError = fieldError != null,
            supportingText = supporting(fieldError),
            singleLine = true,
            enabled = !busy,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus).testTag("oauth_client_id_input"),
          )
          if (loginState.awaitingBrowser) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Gl.colors.textSecondary)
              Spacer(Modifier.width(8.dp))
              Text("Finish signing in in your browser, then come back.", style = Gl.type.meta, color = Gl.colors.textSecondary)
            }
          }
        }
        SignInMethod.PUBLIC -> {
          Explanation("Browse public projects, their code, merge requests and pipelines without an account. Nothing can be changed in this mode.")
        }
        SignInMethod.PASSWORD -> {
          Explanation("Works when the server allows password sign-in. If your account uses SSO, choose OAuth instead.")
          OutlinedTextField(
            value = username,
            onValueChange = { username = it; fieldError = null },
            label = { Text("Username or email") },
            isError = fieldError != null && username.isBlank(),
            supportingText = supporting(fieldError.takeIf { username.isBlank() }),
            singleLine = true,
            enabled = !busy && !loginState.needsTwoFactor,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            colors = fieldColors,
            modifier = Modifier
              .fillMaxWidth()
              .then(if (!loginState.needsTwoFactor) Modifier.focusRequester(fieldFocus) else Modifier)
              .testTag("instance_username_input"),
          )
          OutlinedTextField(
            value = password,
            onValueChange = { password = it; fieldError = null },
            label = { Text("Password") },
            isError = fieldError != null && username.isNotBlank() && password.isEmpty(),
            supportingText = supporting(fieldError.takeIf { username.isNotBlank() && password.isEmpty() }),
            singleLine = true,
            enabled = !busy && !loginState.needsTwoFactor,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                  if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                  contentDescription = if (showPassword) "Hide password" else "Show password",
                  tint = Gl.colors.textSecondary,
                )
              }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth().testTag("instance_password_input"),
          )
          if (loginState.needsTwoFactor) {
            OutlinedTextField(
              value = otp,
              onValueChange = { otp = it; fieldError = null },
              label = { Text("Authentication code") },
              isError = fieldError != null,
              supportingText = supporting(fieldError ?: "From your authenticator app"),
              singleLine = true,
              enabled = !busy,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
              colors = fieldColors,
              modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus).testTag("two_factor_input"),
            )
          }
        }
      }

      if (loginState.isError && loginState.message != null) {
        InlineError(message = loginState.message)
      }

      val buttonLabel = when (method) {
        SignInMethod.TOKEN -> "Connect"
        SignInMethod.OAUTH -> "Continue in browser"
        SignInMethod.PUBLIC -> "Browse without signing in"
        SignInMethod.PASSWORD -> if (loginState.needsTwoFactor) "Verify code" else "Sign in"
      }
      GlButton(
        text = buttonLabel,
        onClick = { submit() },
        loading = busy,
        modifier = Modifier.fillMaxWidth().testTag("sign_in_button"),
      )
      GlButton(
        text = "Cancel",
        kind = GlButtonKind.Ghost,
        enabled = !busy,
        onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun Explanation(text: String) {
  Text(text, style = Gl.type.meta, color = Gl.colors.textSecondary)
}

private fun hostOf(url: String): String =
  url.removePrefix("https://").removePrefix("http://").trimEnd('/')

private fun supporting(text: String?): (@Composable () -> Unit)? =
  text?.let { value -> @Composable { Text(value) } }
