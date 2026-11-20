package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.DoveAiService
import com.example.data.DoveRepository
import com.example.data.LocalSafetyStore
import com.example.viewmodel.DoveViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

private enum class DoveTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    PICKS("Picks", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    DISCOVER("Discover", Icons.Filled.Explore, Icons.Outlined.Explore),
    NEARBY("Nearby", Icons.Filled.LocationOn, Icons.Outlined.LocationOn),
    MESSAGES("Messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    ME("Me", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun DoveAppRoot() {
    val authFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { /* authStateFlow updates automatically */ })
        return
    }

    key(user.uid) {
        val context = LocalContext.current.applicationContext
        val viewModel: DoveViewModel = viewModel(
            key = user.uid,
            factory = viewModelFactory {
                initializer {
                    DoveViewModel(
                        repository = DoveRepository(context),
                        aiService = DoveAiService(),
                        localSafetyStore = LocalSafetyStore(context),
                        currentUserId = user.uid,
                        currentUserEmail = user.email.orEmpty()
                    )
                }
            }
        )
        DoveAuthenticatedShell(
            currentUserId = user.uid,
            viewModel = viewModel
        )
    }
}

@Composable
private fun DoveAuthenticatedShell(
    currentUserId: String,
    viewModel: DoveViewModel
) {
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()

    val under18BlockedUntil by viewModel.under18BlockedUntilMs.collectAsStateWithLifecycle()
    val accountState by viewModel.accountState.collectAsStateWithLifecycle()
    val ownProfileState by viewModel.ownProfileState.collectAsStateWithLifecycle()
    val privatePrefs by viewModel.privatePrefsState.collectAsStateWithLifecycle()
    val pins by viewModel.pinsState.collectAsStateWithLifecycle()
    val swipes by viewModel.swipesState.collectAsStateWithLifecycle()
    val blocks by viewModel.blocksState.collectAsStateWithLifecycle()
    val safeDates by viewModel.safeDatesState.collectAsStateWithLifecycle()
    val messageRequests by viewModel.messageRequestsState.collectAsStateWithLifecycle()
    val connections by viewModel.connectionsState.collectAsStateWithLifecycle()
    val reports by viewModel.myReportsState.collectAsStateWithLifecycle()
    val scoredCandidates by viewModel.scoredCandidates.collectAsStateWithLifecycle()
    val nearbyCandidates by viewModel.nearbyCandidates.collectAsStateWithLifecycle()
    val discoverFilters by viewModel.discoverFilters.collectAsStateWithLifecycle()
    val selectedConnectionId by viewModel.selectedConnectionId.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessagesState.collectAsStateWithLifecycle()

    val onboardingStep by viewModel.onboardingStep.collectAsStateWithLifecycle()
    val onboardingDraft by viewModel.onboardingDraft.collectAsStateWithLifecycle()
    val aiLoading by viewModel.aiBuilderLoading.collectAsStateWithLifecycle()
    val aiError by viewModel.aiBuilderError.collectAsStateWithLifecycle()
    val aiClarify by viewModel.aiClarifyHint.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val victimGuidance by viewModel.victimGuidanceText.collectAsStateWithLifecycle()
    val victimLoading by viewModel.victimGuidanceLoading.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    if (under18BlockedUntil > System.currentTimeMillis()) {
        Under18BlockedScreen()
        return
    }

    when (val accState = accountState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Loading your Dove profile...", style = MaterialTheme.typography.bodyMedium)
                }
            }
            return
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.widthIn(max = 460.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Could not connect to Dove", style = MaterialTheme.typography.titleLarge)
                        Text(accState.message, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { signOut(credentialManager, scope) }) {
                                Text("Sign Out")
                            }
                        }
                    }
                }
            }
            return
        }

        is UiState.Success -> {
            val account = accState.data
            if (account == null || !account.onboardingComplete) {
                Box(modifier = Modifier.fillMaxSize()) {
                    OnboardingFlowScreen(
                        step = onboardingStep,
                        draft = onboardingDraft,
                        aiLoading = aiLoading,
                        aiError = aiError,
                        aiClarifyHint = aiClarify,
                        onSubmitAge = viewModel::submitAge,
                        onAcceptTerms = viewModel::acceptTermsAndGuidelines,
                        onSubmitSafetyConsent = viewModel::submitSafetyConsent,
                        onRunAiExtract = viewModel::runAiProfileExtraction,
                        onUpdateDraft = viewModel::updateDraft,
                        onSelfieVerified = viewModel::completeSelfieVerificationInOnboarding,
                        onSavePrivatePrefs = viewModel::savePrivateCompatibility,
                        onNextStep = viewModel::setOnboardingStep,
                        onPublish = viewModel::publishProfileFromOnboarding,
                        onReportConcern = viewModel::reportConcern
                    )
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    )
                }
                return
            }

            val ownProfile = (ownProfileState as? UiState.Success)?.data
            val tier = viewModel.capabilityTier(account)
            val canSendRose = account.selfieVerified && account.rosesBalance > 0 && account.rosesSentWeek < 5

            val activeConnection = selectedConnectionId?.let { cid ->
                connections.firstOrNull { it.id == cid && it.status == "active" }
            }

            if (activeConnection != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ChatThreadScreen(
                        currentUserId = currentUserId,
                        account = account,
                        connection = activeConnection,
                        messages = activeMessages,
                        aiGuidance = victimGuidance,
                        aiGuidanceLoading = victimLoading,
                        onBack = { viewModel.selectConnection(null) },
                        onSendMessage = { text, rose ->
                            viewModel.sendThreadMessage(activeConnection, text, rose)
                        },
                        onDismissNudge = { viewModel.dismissThreadNudge(activeConnection.id) },
                        onAppealStrike = viewModel::appealStrike,
                        onBlockAndReport = { partnerUid, partnerName, cat, details, alsoReport, snippet ->
                            viewModel.blockUser(
                                targetUid = partnerUid,
                                targetName = partnerName,
                                category = cat?.code,
                                evidenceSnapshot = snippet,
                                connectionId = activeConnection.id,
                                alsoReport = alsoReport,
                                reportDetails = details
                            )
                        },
                        onSaveSafeDate = { place, meetAt, contact ->
                            val partnerName = if (activeConnection.userAId == currentUserId) {
                                activeConnection.userBName
                            } else {
                                activeConnection.userAName
                            }
                            viewModel.saveSafeDate(activeConnection.id, partnerName, place, meetAt, contact)
                        },
                        onRequestVictimGuidance = viewModel::loadVictimSupportGuidance
                    )
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    )
                }
                return
            }

            var selectedTabIndex by remember { mutableIntStateOf(0) }
            val pendingRequestsCount = messageRequests.count {
                it.receiverId == currentUserId && it.status == "pending"
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 600.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (!useRail) {
                            NavigationBar {
                                DoveTab.entries.forEachIndexed { idx, tab ->
                                    val selected = selectedTabIndex == idx
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { selectedTabIndex = idx },
                                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                                        icon = {
                                            if (tab == DoveTab.MESSAGES && pendingRequestsCount > 0) {
                                                BadgedBox(badge = { Badge { Text("$pendingRequestsCount") } }) {
                                                    Icon(
                                                        if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = tab.label
                                                    )
                                                }
                                            } else {
                                                Icon(
                                                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.label
                                                )
                                            }
                                        },
                                        label = { Text(tab.label) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useRail) {
                            NavigationRail {
                                Spacer(Modifier.height(12.dp))
                                DoveTab.entries.forEachIndexed { idx, tab ->
                                    val selected = selectedTabIndex == idx
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { selectedTabIndex = idx },
                                        modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}"),
                                        icon = {
                                            if (tab == DoveTab.MESSAGES && pendingRequestsCount > 0) {
                                                BadgedBox(badge = { Badge { Text("$pendingRequestsCount") } }) {
                                                    Icon(
                                                        if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = tab.label
                                                    )
                                                }
                                            } else {
                                                Icon(
                                                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.label
                                                )
                                            }
                                        },
                                        label = { Text(tab.label) }
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
                                when (DoveTab.entries[selectedTabIndex]) {
                                    DoveTab.PICKS -> PicksScreen(
                                        account = account,
                                        candidates = scoredCandidates,
                                        tier = tier,
                                        onSwipe = viewModel::swipeCandidate,
                                        onBlockAndReport = { target, cat, details, alsoReport ->
                                            viewModel.blockUser(
                                                targetUid = target.userId,
                                                targetName = target.firstName,
                                                category = cat?.code,
                                                evidenceSnapshot = target.bio,
                                                connectionId = null,
                                                alsoReport = alsoReport,
                                                reportDetails = details
                                            )
                                        }
                                    )

                                    DoveTab.DISCOVER -> DiscoverScreen(
                                        candidates = scoredCandidates,
                                        filters = discoverFilters,
                                        onUpdateFilters = viewModel::updateDiscoverFilters,
                                        onSwipe = viewModel::swipeCandidate,
                                        onSendRequest = viewModel::sendOneMessageRequest,
                                        onBlockAndReport = { target, cat, details, alsoReport ->
                                            viewModel.blockUser(
                                                targetUid = target.userId,
                                                targetName = target.firstName,
                                                category = cat?.code,
                                                evidenceSnapshot = target.bio,
                                                connectionId = null,
                                                alsoReport = alsoReport,
                                                reportDetails = details
                                            )
                                        },
                                        canSendRose = canSendRose,
                                        rosesUsedWeek = account.rosesSentWeek
                                    )

                                    DoveTab.NEARBY -> NearbyScreen(
                                        ownProfile = ownProfile,
                                        nearbyCandidates = nearbyCandidates,
                                        onToggleShowInNearby = { show ->
                                            viewModel.updateReachAndDiscoverySettings(showInNearby = show)
                                        },
                                        onSwipeLike = { s -> viewModel.swipeCandidate(s, "like", null, false) },
                                        onSendRequest = viewModel::sendOneMessageRequest,
                                        onBlockAndReport = { target, cat, details, alsoReport ->
                                            viewModel.blockUser(
                                                targetUid = target.userId,
                                                targetName = target.firstName,
                                                category = cat?.code,
                                                evidenceSnapshot = target.bio,
                                                connectionId = null,
                                                alsoReport = alsoReport,
                                                reportDetails = details
                                            )
                                        },
                                        canSendRose = canSendRose,
                                        rosesUsedWeek = account.rosesSentWeek
                                    )

                                    DoveTab.MESSAGES -> MessagesHubScreen(
                                        currentUserId = currentUserId,
                                        account = account,
                                        connections = connections,
                                        messageRequests = messageRequests,
                                        swipes = swipes,
                                        safeDates = safeDates,
                                        onOpenConnection = { conn -> viewModel.selectConnection(conn.id) },
                                        onReplyToRequest = viewModel::replyToRequestAndMatch,
                                        onIgnoreRequest = viewModel::ignoreRequest,
                                        onBulkIgnoreFiltered = viewModel::bulkIgnoreFiltered,
                                        onBulkReportSpam = viewModel::bulkReportSpam,
                                        onClimbWaitingLadder = viewModel::climbWaitingLadder,
                                        onCheckInSafeDate = viewModel::checkInSafeDate,
                                        onBlockAndReportUser = { uid, name, cat, details, alsoReport, snippet ->
                                            viewModel.blockUser(
                                                targetUid = uid,
                                                targetName = name,
                                                category = cat?.code,
                                                evidenceSnapshot = snippet,
                                                connectionId = null,
                                                alsoReport = alsoReport,
                                                reportDetails = details
                                            )
                                        }
                                    )

                                    DoveTab.ME -> MeScreen(
                                        account = account,
                                        ownProfile = ownProfile,
                                        privatePrefs = privatePrefs,
                                        pins = pins,
                                        blocks = blocks,
                                        reports = reports,
                                        safeDates = safeDates,
                                        tier = tier,
                                        onCompleteSelfieVerify = { viewModel.completeSelfieVerificationFromMe(ownProfile) },
                                        onEnableSafetyConsent = viewModel::toggleBrowseOnlyToFullSafety,
                                        onAddPin = viewModel::addProtectedPin,
                                        onPauseAllPins = viewModel::togglePauseAllPins,
                                        onDeletePin = viewModel::deletePin,
                                        onSavePrivatePrefs = viewModel::savePrivateCompatibility,
                                        onDeletePrivatePrefs = viewModel::deletePrivateCompatibility,
                                        onUpdateReachSettings = viewModel::updateReachAndDiscoverySettings,
                                        onReportAfterBlock = viewModel::reportAfterBlock,
                                        onCheckInSafeDate = viewModel::checkInSafeDate,
                                        onReportConcern = viewModel::reportConcern,
                                        onDeleteAccount = {
                                            viewModel.deleteAccountAndSignOut {
                                                signOut(credentialManager, scope)
                                            }
                                        },
                                        onSignOut = { signOut(credentialManager, scope) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
