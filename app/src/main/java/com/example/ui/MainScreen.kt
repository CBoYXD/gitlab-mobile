package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GitLabTanukiIcon
import com.example.ui.screens.FileViewerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.InstancesScreen
import com.example.ui.screens.IssueDetailScreen
import com.example.ui.screens.IssuesScreen
import com.example.ui.screens.MergeRequestDetailScreen
import com.example.ui.screens.MergeRequestsScreen
import com.example.ui.screens.PipelinesScreen
import com.example.ui.screens.ProjectDetailScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.screens.WorkItemsScreen
import com.example.ui.theme.GitLabOrange
import com.example.ui.theme.GitLabPurple
import com.example.ui.theme.GitLabSuccess
import com.example.ui.viewmodel.BottomTab
import com.example.ui.viewmodel.GitLabViewModel
import com.example.ui.viewmodel.ScreenDestination
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: GitLabViewModel) {
  val uiState by viewModel.uiState.collectAsState()
  var showInstanceSwitcherSheet by remember { mutableStateOf(false) }
  val sheetState = rememberModalBottomSheetState()
  val scope = rememberCoroutineScope()

  BackHandler(enabled = uiState.destination !is ScreenDestination.MainTabs) {
    viewModel.navigateBack()
  }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.statusBars),
    bottomBar = {
      if (uiState.destination is ScreenDestination.MainTabs) {
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surface,
          tonalElevation = 8.dp,
          modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("main_bottom_nav")
        ) {
          // 1. Home (1st tab)
          NavigationBarItem(
            selected = (uiState.currentTab == BottomTab.HOME),
            onClick = { viewModel.switchTab(BottomTab.HOME) },
            icon = {
              Icon(
                imageVector = if (uiState.currentTab == BottomTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                contentDescription = "Home"
              )
            },
            label = { Text("Home", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = GitLabOrange,
              selectedTextColor = GitLabOrange,
              indicatorColor = GitLabOrange.copy(alpha = 0.12f)
            ),
            modifier = Modifier.testTag("nav_item_home")
          )

          // 2. Inbox (2nd tab - requested by user)
          val unreadNotifs = uiState.notifications.count { it.isUnread }
          NavigationBarItem(
            selected = (uiState.currentTab == BottomTab.INBOX),
            onClick = { viewModel.switchTab(BottomTab.INBOX) },
            icon = {
              BadgedBox(
                badge = {
                  if (unreadNotifs > 0) {
                    Badge(
                      containerColor = GitLabOrange,
                      contentColor = Color.White
                    ) {
                      Text(unreadNotifs.toString(), fontSize = 10.sp)
                    }
                  }
                }
              ) {
                Icon(
                  imageVector = if (uiState.currentTab == BottomTab.INBOX) Icons.Filled.Inbox else Icons.Outlined.Inbox,
                  contentDescription = "Inbox"
                )
              }
            },
            label = { Text("Inbox", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = GitLabOrange,
              selectedTextColor = GitLabOrange,
              indicatorColor = GitLabOrange.copy(alpha = 0.12f)
            ),
            modifier = Modifier.testTag("nav_item_inbox")
          )

          // 3. Projects (3rd tab)
          NavigationBarItem(
            selected = (uiState.currentTab == BottomTab.PROJECTS),
            onClick = { viewModel.switchTab(BottomTab.PROJECTS) },
            icon = {
              Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = "Projects"
              )
            },
            label = { Text("Projects", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = GitLabOrange,
              selectedTextColor = GitLabOrange,
              indicatorColor = GitLabOrange.copy(alpha = 0.12f)
            ),
            modifier = Modifier.testTag("nav_item_projects")
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Screen Routing
      when (val dest = uiState.destination) {
        ScreenDestination.MainTabs -> {
          when (uiState.currentTab) {
            BottomTab.HOME -> HomeScreen(
              uiState = uiState,
              onNavigate = { viewModel.navigateTo(it) },
              onSwitchTab = { viewModel.switchTab(it) },
              onOpenInstanceSwitcher = { showInstanceSwitcherSheet = true },
              onOpenProfile = { viewModel.navigateTo(ScreenDestination.UserProfile) },
              onToggleStar = { viewModel.toggleProjectStar(it) },
              onRefresh = { viewModel.loadAllData() }
            )
            BottomTab.INBOX -> InboxScreen(
              notifications = uiState.notifications,
              currentFilter = uiState.inboxFilter,
              onFilterChange = { viewModel.setInboxFilter(it) },
              onToggleRead = { viewModel.toggleNotificationRead(it) },
              onMarkAllRead = { viewModel.markAllNotificationsAsRead() },
              onNavigateToWorkItems = { viewModel.navigateTo(ScreenDestination.IssuesList) },
              onNavigateToPipelines = { viewModel.navigateTo(ScreenDestination.PipelinesList) }
            )
            BottomTab.PROJECTS -> ProjectsScreen(
              projects = uiState.projects,
              searchQuery = uiState.searchQuery,
              onSearchChange = { viewModel.setSearchQuery(it) },
              onSelectProject = { viewModel.navigateTo(ScreenDestination.ProjectDetail(it)) },
              onToggleStar = { viewModel.toggleProjectStar(it) },
              onAddProject = { _, _, _ ->
                viewModel.setSearchQuery("")
              }
            )
          }
        }
        is ScreenDestination.IssuesList -> {
          IssuesScreen(
            issues = uiState.issues,
            projects = uiState.projects,
            onSelectIssue = { viewModel.navigateTo(ScreenDestination.IssueDetail(it)) },
            onCreateIssue = { projId, title, desc, labels ->
              viewModel.createNewIssue(projId, title, desc, labels)
            },
            onBack = { viewModel.navigateBack() }
          )
        }
        is ScreenDestination.MergeRequestsList -> {
          MergeRequestsScreen(
            mergeRequests = uiState.mergeRequests,
            onSelectMergeRequest = { viewModel.navigateTo(ScreenDestination.MergeRequestDetail(it)) },
            onBack = { viewModel.navigateBack() }
          )
        }
        is ScreenDestination.PipelinesList -> {
          PipelinesScreen(
            pipelines = uiState.projectPipelines,
            onRefresh = { viewModel.loadAllData() },
            onBack = { viewModel.navigateBack() }
          )
        }
        is ScreenDestination.UserProfile -> {
          UserProfileScreen(
            activeInstance = uiState.activeInstance,
            onBack = { viewModel.navigateBack() },
            onOpenInstanceSwitcher = { showInstanceSwitcherSheet = true }
          )
        }
        is ScreenDestination.Instances -> {
          InstancesScreen(
            activeInstance = uiState.activeInstance,
            savedInstances = uiState.savedInstances,
            connectionTestState = uiState.connectionTestState,
            onSwitchInstance = { viewModel.switchInstance(it) },
            onAddInstance = { name, url, token, makeActive ->
              viewModel.addCustomInstance(name, url, token, makeActive)
            },
            onDeleteInstance = { viewModel.deleteInstance(it) },
            onTestConnection = { url, token -> viewModel.testConnection(url, token) },
            onClearTestState = { viewModel.clearConnectionTestState() },
            onBack = { viewModel.navigateBack() }
          )
        }
        is ScreenDestination.ProjectDetail -> {
          ProjectDetailScreen(
            project = uiState.selectedProject,
            tree = uiState.projectTree,
            currentPath = uiState.currentTreePath,
            issues = uiState.issues,
            mergeRequests = uiState.mergeRequests,
            pipelines = uiState.projectPipelines,
            commits = uiState.projectCommits,
            onBack = { viewModel.navigateBack() },
            onToggleStar = { viewModel.toggleProjectStar(it) },
            onNavigateTree = { path ->
              uiState.selectedProject?.let { p ->
                viewModel.loadRepositoryTree(p.id, path)
              }
            },
            onOpenFile = { path ->
              uiState.selectedProject?.let { p ->
                viewModel.navigateTo(ScreenDestination.FileViewer(p.id, path))
              }
            },
            onSelectIssue = { viewModel.navigateTo(ScreenDestination.IssueDetail(it)) },
            onSelectMr = { viewModel.navigateTo(ScreenDestination.MergeRequestDetail(it)) }
          )
        }
        is ScreenDestination.FileViewer -> {
          FileViewerScreen(
            filePath = dest.filePath,
            content = uiState.selectedFileContent,
            isLoading = uiState.isFileLoading,
            onBack = { viewModel.navigateBack() }
          )
        }
        is ScreenDestination.MergeRequestDetail -> {
          MergeRequestDetailScreen(
            mr = dest.mr,
            diffs = uiState.selectedMrDiffs,
            isLoadingDiffs = uiState.isDiffLoading,
            onBack = { viewModel.navigateBack() }
          )
        }
        is ScreenDestination.IssueDetail -> {
          IssueDetailScreen(
            issue = dest.issue,
            onBack = { viewModel.navigateBack() }
          )
        }
      }

      // Notification Banner (when switching instance or creating issue)
      AnimatedVisibility(
        visible = uiState.bannerMessage != null,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
        modifier = Modifier.align(Alignment.TopCenter)
      ) {
        uiState.bannerMessage?.let { msg ->
          Surface(
            color = GitLabPurple,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
              .padding(16.dp)
              .fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                GitLabTanukiIcon(size = 18.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = msg,
                  color = Color.White,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
              IconButton(
                onClick = { viewModel.dismissBanner() },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Dismiss",
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }
      }
    }
  }

  // Quick Instance Switcher Bottom Sheet
  if (showInstanceSwitcherSheet) {
    ModalBottomSheet(
      onDismissRequest = { showInstanceSwitcherSheet = false },
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surface
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .padding(bottom = 32.dp)
      ) {
        Text(
          text = "Switch GitLab Instance",
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
          modifier = Modifier.padding(bottom = 12.dp)
        )

        uiState.savedInstances.forEach { instance ->
          val isSelected = (instance.id == uiState.activeInstance.id)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable {
                scope.launch {
                  viewModel.switchInstance(instance.id)
                  sheetState.hide()
                  showInstanceSwitcherSheet = false
                }
              }
              .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              GitLabTanukiIcon(size = 22.dp)
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = instance.name,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 14.sp
                )
                Text(
                  text = instance.url,
                  fontSize = 11.5.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            if (isSelected) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(GitLabOrange)
              )
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable {
              scope.launch {
                sheetState.hide()
                showInstanceSwitcherSheet = false
                viewModel.navigateTo(ScreenDestination.Instances)
              }
            }
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Dns,
            contentDescription = null,
            tint = GitLabOrange,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Manage & Add Custom Instances...",
            color = GitLabOrange,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp
          )
        }
      }
    }
  }
}
