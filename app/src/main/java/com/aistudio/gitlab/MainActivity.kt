package com.aistudio.gitlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.aistudio.gitlab.ui.MainScreen
import com.aistudio.gitlab.ui.theme.MyApplicationTheme
import com.aistudio.gitlab.ui.viewmodel.GitLabViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: GitLabViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainScreen(viewModel = viewModel)
      }
    }
  }
}

