package com.cboyxd.gitlab.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import okhttp3.HttpUrl.Companion.toHttpUrl
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cboyxd.gitlab.data.api.normalizeBaseUrl
import com.cboyxd.gitlab.data.auth.GitLabAuth
import com.cboyxd.gitlab.ui.components.ErrorState
import com.cboyxd.gitlab.ui.components.ImageAuth
import com.cboyxd.gitlab.ui.components.LocalImageAuth
import com.cboyxd.gitlab.ui.components.FullScreenState
import com.cboyxd.gitlab.ui.components.GitLabTanukiIcon
import com.cboyxd.gitlab.ui.components.GlDivider
import com.cboyxd.gitlab.ui.components.GlRow
import com.cboyxd.gitlab.ui.components.GlSectionHeader
import com.cboyxd.gitlab.ui.screens.FileViewerScreen
import com.cboyxd.gitlab.ui.viewmodel.GitLabUiState
import com.cboyxd.gitlab.ui.viewmodel.ProjectSection
import com.cboyxd.gitlab.data.model.GitLabLabel
import com.cboyxd.gitlab.ui.components.MarkdownContext
import com.cboyxd.gitlab.ui.screens.UserDetailScreen
import com.cboyxd.gitlab.ui.screens.SearchScreen
import com.cboyxd.gitlab.ui.screens.ReleaseDetailScreen
import com.cboyxd.gitlab.ui.screens.ProjectSectionScreen
import com.cboyxd.gitlab.ui.screens.ProjectCodeScreen
import com.cboyxd.gitlab.ui.screens.PipelineDetailScreen
import com.cboyxd.gitlab.ui.screens.JobDetailScreen
import com.cboyxd.gitlab.ui.screens.GroupDetailScreen
import com.cboyxd.gitlab.ui.screens.CommitDetailScreen
import com.cboyxd.gitlab.ui.screens.GroupsScreen
import com.cboyxd.gitlab.ui.screens.HomeScreen
import com.cboyxd.gitlab.ui.screens.InboxScreen
import com.cboyxd.gitlab.ui.screens.InstancesScreen
import com.cboyxd.gitlab.ui.screens.IssueDetailScreen
import com.cboyxd.gitlab.ui.screens.IssuesScreen
import com.cboyxd.gitlab.ui.screens.MergeRequestDetailScreen
import com.cboyxd.gitlab.ui.screens.MergeRequestsScreen
import com.cboyxd.gitlab.ui.screens.PipelinesScreen
import com.cboyxd.gitlab.ui.screens.ProjectDetailScreen
import com.cboyxd.gitlab.ui.screens.ProjectsScreen
import com.cboyxd.gitlab.ui.screens.UserProfileScreen
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlMotion
import com.cboyxd.gitlab.ui.theme.glTween
import com.cboyxd.gitlab.ui.viewmodel.BottomTab
import com.cboyxd.gitlab.ui.viewmodel.GitLabViewModel
import com.cboyxd.gitlab.ui.viewmodel.ScreenDestination
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(viewModel: GitLabViewModel, onExit: () -> Unit) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  var showSwitcher by rememberSaveable { mutableStateOf(false) }
  var openAddServer by rememberSaveable { mutableStateOf(false) }
  val c = Gl.colors
  var showOpening by remember { mutableStateOf(false) }
  LaunchedEffect(uiState.pendingDestination) {
    showOpening = false
    if (uiState.pendingDestination != null) {
      delay(400)
      showOpening = true
    }
  }

  LaunchedEffect(viewModel) {
    launch { viewModel.externalLinks.collect { openUrl(context, it) } }
    viewModel.messages.collect { message ->
      val result = snackbarHostState.showSnackbar(
        message = message.text,
        actionLabel = message.actionLabel,
        withDismissAction = message.isError,
        duration = if (message.isError || message.action != null) SnackbarDuration.Indefinite else SnackbarDuration.Short,
      )
      if (result == SnackbarResult.ActionPerformed) message.action?.invoke()
    }
  }

  BackHandler(enabled = uiState.pendingDestination != null || uiState.destination !is ScreenDestination.MainTabs || uiState.currentTab != BottomTab.HOME) {
    if (!viewModel.navigateBack()) {
      if (uiState.currentTab != BottomTab.HOME) viewModel.switchTab(BottomTab.HOME) else onExit()
    }
  }

  val openServers: (Boolean) -> Unit = { add ->
    openAddServer = add
    viewModel.navigateTo(ScreenDestination.Instances)
  }

  val imageAuth = remember(uiState.activeInstance) {
    uiState.activeInstance?.let { instance ->
      val baseUrl = normalizeBaseUrl(instance.url).toHttpUrl().toString()
      val token = instance.token.trim()
      if (instance.authKind == GitLabAuth.KIND_OAUTH || token.startsWith("Bearer ", ignoreCase = true)) {
        val value = if (token.startsWith("Bearer ", ignoreCase = true)) token.substringAfter(' ').trim() else token
        ImageAuth(baseUrl, instance.id, "Authorization", "Bearer $value")
      } else {
        ImageAuth(baseUrl, instance.id, "PRIVATE-TOKEN", token)
      }
    }
  }

  CompositionLocalProvider(LocalImageAuth provides imageAuth) {
  Scaffold(
    // Exposes test tags as resource ids so UI Automator (and screenshot redaction) can find them.
    modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
    containerColor = c.bg,
    snackbarHost = {
      SnackbarHost(snackbarHostState) { data ->
        Snackbar(
          snackbarData = data,
          containerColor = c.textPrimary,
          contentColor = c.bg,
          actionColor = c.bg,
          dismissActionContentColor = c.bg,
          shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        )
      }
    },
    bottomBar = {
      if (uiState.destination is ScreenDestination.MainTabs && uiState.bootstrapped && uiState.storageError == null) {
        Column {
          GlDivider()
          NavigationBar(containerColor = c.surface, tonalElevation = 0.dp, modifier = Modifier.testTag("main_bottom_nav")) {
            val itemColors = NavigationBarItemDefaults.colors(
              selectedIconColor = c.accent,
              selectedTextColor = c.textPrimary,
              unselectedIconColor = c.textSecondary,
              unselectedTextColor = c.textPrimary,
              indicatorColor = c.accentBg,
            )
            TabItem(BottomTab.HOME, uiState.currentTab, Icons.Filled.Home, Icons.Outlined.Home, itemColors, viewModel::switchTab)
            TabItem(BottomTab.PROJECTS, uiState.currentTab, Icons.Filled.Folder, Icons.Outlined.Folder, itemColors, viewModel::switchTab)
            NavigationBarItem(
              selected = uiState.currentTab == BottomTab.INBOX,
              onClick = { viewModel.switchTab(BottomTab.INBOX) },
              icon = {
                val unread = uiState.unreadCount
                BadgedBox(badge = {
                  if (unread > 0) {
                    Badge(containerColor = c.info, contentColor = c.surface) {
                      Text(if (unread > 99) "99+" else unread.toString(), style = Gl.type.badge)
                    }
                  }
                }) {
                  Icon(if (uiState.currentTab == BottomTab.INBOX) Icons.Filled.Inbox else Icons.Outlined.Inbox, contentDescription = null)
                }
              },
              label = { Text(BottomTab.INBOX.title, style = Gl.type.badge) },
              colors = itemColors,
              modifier = Modifier
                .testTag("nav_item_inbox")
                .semantics { contentDescription = if (uiState.unreadCount > 0) "Inbox, ${uiState.unreadCount} to-dos" else "Inbox" },
            )
          }
        }
      }
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      when {
        !uiState.bootstrapped -> Unit
        uiState.storageError != null -> FullScreenState {
          ErrorState(message = uiState.storageError ?: "", onRetry = viewModel::retryBootstrap)
        }
        else -> {
          val reduced = Gl.reducedMotion
          // Keep the visible screen stable while another destination prepares its data.
          val presented = remember { arrayOf(uiState) }
          if (uiState.pendingDestination == null) presented[0] = uiState
          AnimatedContent(
            targetState = presented[0],
            contentKey = { it.destination to it.currentTab },
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
              if (reduced) {
                fadeIn(glTween(GlMotion.MEDIUM, true)) togetherWith fadeOut(glTween(GlMotion.MEDIUM, true))
              } else {
                val direction = when {
                  targetState.backStack.size > initialState.backStack.size -> 1
                  targetState.backStack.size < initialState.backStack.size -> -1
                  else -> if (targetState.currentTab.ordinal >= initialState.currentTab.ordinal) 1 else -1
                }
                ((slideInHorizontally(tween(340, easing = GlMotion.Standard)) { direction * it / 7 } +
                  fadeIn(tween(280, delayMillis = 50))) togetherWith
                  (slideOutHorizontally(tween(340, easing = GlMotion.Standard)) { -direction * it / 12 } +
                    fadeOut(tween(240)))).using(SizeTransform(clip = false))
              }
            },
            label = "screen",
          ) { scene ->
            Destination(
              uiState = scene,
              destination = scene.destination,
              tab = scene.currentTab,
              viewModel = viewModel,
              context = context,
              openSwitcher = { showSwitcher = true },
              openServers = openServers,
              openAddServer = openAddServer,
              onAddServerConsumed = { openAddServer = false },
            )
          }
        }
      }
      if (showOpening && uiState.pendingDestination != null) {
        Surface(
          modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
          shape = RoundedCornerShape(16.dp),
          color = c.surface,
          tonalElevation = 4.dp,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 4.dp)) {
            // Polite live region: screen readers hear that the next screen is still loading.
            Text(
              "Opening…",
              style = Gl.type.meta,
              color = c.textSecondary,
              modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            TextButton(onClick = { viewModel.navigateBack() }) {
              Text("Cancel", style = Gl.type.label, color = c.accent)
            }
          }
        }
      }
    }
  }

  }

  if (showSwitcher) {
    InstanceSwitcherSheet(
      viewModel = viewModel,
      onDismiss = { showSwitcher = false },
      onManage = { add ->
        showSwitcher = false
        openServers(add)
      },
    )
  }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(
  tab: BottomTab,
  current: BottomTab,
  selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  colors: androidx.compose.material3.NavigationBarItemColors,
  onSelect: (BottomTab) -> Unit,
) {
  NavigationBarItem(
    selected = current == tab,
    onClick = { onSelect(tab) },
    icon = { Icon(if (current == tab) selectedIcon else icon, contentDescription = null) },
    label = { Text(tab.title, style = Gl.type.badge) },
    colors = colors,
    modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}"),
  )
}

@Composable
private fun Destination(
  uiState: GitLabUiState,
  destination: ScreenDestination,
  tab: BottomTab,
  viewModel: GitLabViewModel,
  context: Context,
  openSwitcher: () -> Unit,
  openServers: (add: Boolean) -> Unit,
  openAddServer: Boolean,
  onAddServerConsumed: () -> Unit,
) {
  val back: () -> Unit = { viewModel.navigateBack() }
  val browser: (String) -> Unit = { openUrl(context, it) }
  val onLink: (String) -> Boolean = viewModel.explore::handleLink
  val project = uiState.project
  // Relative links and images in project documents resolve against the project and current ref.
  val projectMarkdown = remember(project.webUrl, project.effectiveRef) {
    MarkdownContext(
      webBase = project.webUrl,
      rawBase = project.webUrl?.let { web -> project.effectiveRef?.let { "$web/-/raw/$it/" } },
      onLink = onLink,
    )
  }
  val labelsFor: (Long) -> Map<String, GitLabLabel> = { uiState.labels[it].orEmpty() }
  when (destination) {
    ScreenDestination.MainTabs -> when (tab) {
      BottomTab.HOME -> HomeScreen(
        state = uiState,
        onRefresh = { viewModel.refreshDashboard(userInitiated = true) },
        onOpenProfile = { if (uiState.isSignedIn) viewModel.navigateTo(ScreenDestination.UserProfile) else openServers(true) },
        onOpenSearch = { viewModel.navigateTo(ScreenDestination.Search) },
        onAddServer = { openServers(true) },
        onSignInAgain = { openServers(true) },
        onNavigate = viewModel::navigateTo,
        onSwitchTab = viewModel::switchTab,
        onToggleStar = viewModel::toggleProjectStar,
      )
      BottomTab.INBOX -> InboxScreen(
        state = uiState,
        onRefresh = { viewModel.refreshDashboard(userInitiated = true) },
        onFilterChange = viewModel::setInboxFilter,
        onOpen = { notification -> viewModel.openNotification(notification)?.let(browser) },
        onMarkDone = viewModel::markNotificationDone,
        onMarkAllDone = viewModel::markAllNotificationsDone,
        onAddServer = { openServers(true) },
      )
      BottomTab.PROJECTS -> ProjectsScreen(
        title = "Projects",
        projects = uiState.projects,
        searchQuery = uiState.searchQuery,
        searchResults = uiState.searchResults,
        pendingStars = uiState.pendingStars,
        canStar = uiState.isSignedIn,
        canCreate = uiState.isSignedIn,
        hasServer = uiState.activeInstance != null,
        onSearchChange = viewModel::setSearchQuery,
        onRefresh = { viewModel.refreshDashboard(userInitiated = true) },
        refreshing = uiState.refreshing,
        onSelectProject = { viewModel.navigateTo(ScreenDestination.ProjectDetail(it)) },
        onToggleStar = viewModel::toggleProjectStar,
        onCreateProject = viewModel::createProject,
        onAddServer = { openServers(true) },
        onBack = null,
      )
    }
    ScreenDestination.IssuesList -> IssuesScreen(
      issues = uiState.issues,
      projects = uiState.projects.data,
      onSelectIssue = viewModel::openIssue,
      onCreateIssue = viewModel::createIssue,
      onRetry = { viewModel.refreshDashboard(userInitiated = true) },
      onBack = back,
    )
    ScreenDestination.MergeRequestsList -> MergeRequestsScreen(
      mergeRequests = uiState.mergeRequests,
      projects = uiState.projects.data,
      onSelectMergeRequest = viewModel::openMergeRequest,
      onCreateMergeRequest = viewModel.review::create,
      onLoadBranches = viewModel.review::branches,
      onRetry = { viewModel.refreshDashboard(userInitiated = true) },
      onBack = back,
    )
    ScreenDestination.PipelinesList -> PipelinesScreen(
      pipelines = uiState.pipelines,
      refreshing = uiState.refreshing,
      onRefresh = { viewModel.refreshDashboard(userInitiated = true) },
      onRetryPipeline = viewModel::retryPipeline,
      onOpenPipeline = { viewModel.ci.openPipeline(it.projectId, it.id) },
      onBack = back,
    )
    ScreenDestination.GroupsList -> GroupsScreen(
      groups = uiState.groups,
      onSelect = { viewModel.navigateTo(ScreenDestination.GroupProjects(it)) },
      onRetry = { viewModel.refreshDashboard(userInitiated = true) },
      onBack = back,
    )
    is ScreenDestination.GroupProjects -> GroupDetailScreen(
      state = uiState.group,
      onBack = back,
      onRetry = viewModel.explore::retryGroup,
      onOpenGroup = { viewModel.navigateTo(ScreenDestination.GroupProjects(it)) },
      onOpenProject = { viewModel.navigateTo(ScreenDestination.ProjectDetail(it)) },
      onOpenInBrowser = browser,
    )
    ScreenDestination.Search -> SearchScreen(
      state = uiState.search,
      isSignedIn = uiState.isSignedIn,
      onBack = back,
      onQueryChange = viewModel.explore::setQuery,
      onScopeChange = viewModel.explore::setScope,
      onOpenProject = { viewModel.navigateTo(ScreenDestination.ProjectDetail(it)) },
      onOpenIssue = viewModel::openIssue,
      onOpenMergeRequest = viewModel::openMergeRequest,
      onOpenUser = viewModel.explore::openUser,
    )
    ScreenDestination.UserProfile -> UserProfileScreen(
      activeInstance = uiState.activeInstance,
      user = uiState.currentUser,
      status = uiState.userStatus,
      profileReadme = uiState.profileReadme,
      onBack = back,
      onOpenSwitcher = openSwitcher,
      onManageServers = { openServers(false) },
      onSetStatus = viewModel::setUserStatus,
      onOpenInBrowser = browser,
      onRetry = { viewModel.refreshDashboard(userInitiated = true) },
    )
    is ScreenDestination.UserDetail -> UserDetailScreen(
      state = uiState.userDetail,
      onBack = back,
      onRetry = viewModel.explore::retryUser,
      onOpenProject = { viewModel.navigateTo(ScreenDestination.ProjectDetail(it)) },
      onNavigate = viewModel::navigateTo,
      onOpenInBrowser = browser,
      markdown = MarkdownContext(onLink = onLink),
    )
    ScreenDestination.Instances -> InstancesScreen(
      activeInstance = uiState.activeInstance,
      savedInstances = uiState.savedInstances,
      loginState = uiState.loginState,
      savedClientId = uiState.gitLabComClientId,
      openAddOnStart = openAddServer,
      onAddOpened = onAddServerConsumed,
      onSwitchInstance = viewModel::switchInstance,
      onDeleteInstance = viewModel::deleteInstance,
      onSignInWithPassword = viewModel::signInWithPassword,
      onSubmitTwoFactor = viewModel::submitTwoFactor,
      onSignInWithOAuth = viewModel::startOAuthSignIn,
      onSignInWithToken = viewModel::signInWithToken,
      onClearLogin = viewModel::clearLoginState,
      onBack = back,
    )
    is ScreenDestination.ProjectDetail -> ProjectDetailScreen(
      state = project,
      pendingStar = project.projectId in uiState.pendingStars,
      canWrite = uiState.isSignedIn,
      markdown = projectMarkdown,
      onBack = back,
      onRetry = viewModel.code::retry,
      onToggleStar = viewModel::toggleProjectStar,
      onOpenSection = viewModel.code::openSection,
      onOpenCode = viewModel.code::openCode,
      onOpenRelease = viewModel.code::openRelease,
      onOpenInBrowser = browser,
      onChangeRef = viewModel.code::setRef,
      onLoadRefs = viewModel.code::loadRefs,
      onFilterRefs = viewModel.code::filterRefs,
      onRefresh = viewModel.code::refresh,
    )
    is ScreenDestination.ProjectCode -> ProjectCodeScreen(
      state = project,
      onBack = back,
      onOpenPath = viewModel.code::openPath,
      onOpenFile = viewModel.code::openFile,
      onChangeRef = viewModel.code::setRef,
      onLoadRefs = viewModel.code::loadRefs,
      onFilterRefs = viewModel.code::filterRefs,
      onOpenCommits = { viewModel.code.openSection(ProjectSection.COMMITS) },
      onOpenInBrowser = browser,
    )
    is ScreenDestination.ProjectSectionList -> ProjectSectionScreen(
      section = destination.section,
      state = project,
      labels = labelsFor(project.projectId),
      canWrite = uiState.isSignedIn,
      onBack = back,
      onRetry = viewModel.code::retrySection,
      onOpenIssue = viewModel::openIssue,
      onOpenMergeRequest = viewModel::openMergeRequest,
      onCreateMergeRequest = viewModel.review::create,
      onLoadBranches = viewModel.review::branches,
      onOpenPipeline = { viewModel.ci.openPipeline(it.projectId, it.id) },
      onRetryPipeline = viewModel::retryPipeline,
      onOpenCommit = { viewModel.code.openCommit(project.projectId, it) },
      onBrowseRef = { ref ->
        viewModel.code.setRef(ref)
        viewModel.code.openCode()
      },
      onOpenRelease = viewModel.code::openRelease,
      onNavigate = viewModel::navigateTo,
      onOpenInBrowser = browser,
    )
    is ScreenDestination.ReleaseDetail -> ReleaseDetailScreen(
      state = project,
      tagName = destination.tagName,
      markdown = projectMarkdown,
      onBack = back,
      onRetry = { viewModel.code.retrySection(ProjectSection.RELEASES) },
      onOpenCommit = { viewModel.code.openCommit(project.projectId, it) },
      onOpenInBrowser = browser,
    )
    is ScreenDestination.FileViewer -> FileViewerScreen(
      state = uiState.file,
      webUrl = project.webUrl?.takeIf { project.projectId == uiState.file.projectId },
      markdown = projectMarkdown,
      onRetry = viewModel.code::retryFile,
      onOpenInBrowser = browser,
      onBack = back,
    )
    is ScreenDestination.CommitDetail -> CommitDetailScreen(
      state = uiState.commitDetail,
      onRetry = viewModel.code::retryCommit,
      onOpenPipeline = { viewModel.ci.openPipeline(it.projectId.takeIf { id -> id > 0 } ?: uiState.commitDetail.projectId, it.id) },
      onOpenInBrowser = browser,
      onBack = back,
    )
    is ScreenDestination.IssueDetail -> IssueDetailScreen(
      state = uiState.issueDetail,
      labels = labelsFor(uiState.issueDetail.projectId) + uiState.issueDetail.issue.data?.labelDetails.orEmpty().associateBy { it.name },
      canWrite = uiState.isSignedIn,
      onLink = onLink,
      onRetry = viewModel::retryIssue,
      onComment = viewModel::postIssueNote,
      onSetClosed = viewModel::setIssueClosed,
      onEdit = viewModel::updateIssue,
      onOpenUser = viewModel.explore::openUser,
      onOpenInBrowser = browser,
      onBack = back,
    )
    is ScreenDestination.MergeRequestDetail -> MergeRequestDetailScreen(
      state = uiState.mergeRequestDetail,
      labels = labelsFor(uiState.mergeRequestDetail.projectId) + uiState.mergeRequestDetail.mergeRequest.data?.labelDetails.orEmpty().associateBy { it.name },
      canWrite = uiState.isSignedIn,
      onLink = onLink,
      onRetry = viewModel.review::retry,
      onMerge = viewModel.review::merge,
      onSetClosed = viewModel.review::setClosed,
      onComment = viewModel.review::postNote,
      onOpenPipeline = { viewModel.ci.openPipeline(it.projectId.takeIf { id -> id > 0 } ?: uiState.mergeRequestDetail.projectId, it.id) },
      onOpenCommit = { viewModel.code.openCommit(uiState.mergeRequestDetail.projectId, it) },
      onOpenUser = viewModel.explore::openUser,
      onOpenInBrowser = browser,
      onBack = back,
    )
    is ScreenDestination.PipelineDetail -> PipelineDetailScreen(
      state = uiState.pipelineDetail,
      canWrite = uiState.isSignedIn,
      onBack = back,
      onRetryLoad = viewModel.ci::retryLoadPipeline,
      onRetryPipeline = viewModel::retryPipeline,
      onCancel = viewModel.ci::cancelPipeline,
      onOpenJob = { viewModel.ci.openJob(uiState.pipelineDetail.projectId, it.id) },
      onOpenCommit = { viewModel.code.openCommit(uiState.pipelineDetail.projectId, it) },
      onOpenInBrowser = browser,
    )
    is ScreenDestination.JobDetail -> JobDetailScreen(
      state = uiState.jobDetail,
      canWrite = uiState.isSignedIn,
      onBack = back,
      onRetryLoad = viewModel.ci::retryLoadJob,
      onRetryJob = viewModel.ci::retryJob,
      onOpenInBrowser = browser,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstanceSwitcherSheet(
  viewModel: GitLabViewModel,
  onDismiss: () -> Unit,
  onManage: (add: Boolean) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()
  val c = Gl.colors
  fun close(then: () -> Unit) {
    scope.launch { sheetState.hide() }.invokeOnCompletion { then() }
  }
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.surface) {
    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp)) {
      GlSectionHeader(title = "Accounts", modifier = Modifier.padding(top = 0.dp))
      uiState.savedInstances.forEach { instance ->
        val selected = instance.id == uiState.activeInstance?.id
        GlRow(
          title = instance.name,
          subtitle = listOfNotNull(
            instance.username.takeIf { it.isNotBlank() }?.let { "@$it" },
            instance.url.removePrefix("https://"),
          ).joinToString(" · "),
          leading = { GitLabTanukiIcon(size = 24.dp) },
          trailing = {
            if (selected) Icon(Icons.Outlined.Check, contentDescription = "Current account", tint = c.textPrimary)
          },
          onClick = {
            close {
              onDismiss()
              viewModel.switchInstance(instance.id)
            }
          },
          onClickLabel = if (selected) null else "Switch to ${instance.name}",
        )
      }
      if (uiState.savedInstances.isNotEmpty()) GlDivider(inset = 16.dp)
      GlRow(
        title = "Add account",
        leading = { Icon(Icons.Outlined.Add, contentDescription = null, tint = c.textPrimary, modifier = Modifier.size(24.dp)) },
        onClick = { close { onManage(true) } },
      )
      GlRow(
        title = "Manage accounts",
        leading = { Icon(Icons.Outlined.Settings, contentDescription = null, tint = c.textPrimary, modifier = Modifier.size(24.dp)) },
        onClick = { close { onManage(false) } },
      )
    }
  }
}

internal fun openUrl(context: Context, url: String) {
  if (url.isBlank()) return
  try {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  } catch (_: ActivityNotFoundException) {
    // No browser installed; nothing sensible to do.
  }
}
