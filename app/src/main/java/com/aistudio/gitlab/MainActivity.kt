package com.aistudio.gitlab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.aistudio.gitlab.ui.MainScreen
import com.aistudio.gitlab.ui.theme.GitLabTheme
import com.aistudio.gitlab.ui.viewmodel.GitLabViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: GitLabViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      GitLabTheme {
        MainScreen(viewModel = viewModel, onExit = { finish() })
      }
    }
    // A recreated activity must not replay the redirect it already handled.
    if (savedInstanceState == null) handleOAuthIntent(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleOAuthIntent(intent)
  }

  private fun handleOAuthIntent(intent: Intent?) {
    val data = intent?.data ?: return
    if (data.scheme != "com.aistudio.gitlab.mclient" || data.host != "oauth") return
    viewModel.completeOAuthRedirect(data)
    setIntent(Intent(intent).apply { this.data = null })
  }
}
