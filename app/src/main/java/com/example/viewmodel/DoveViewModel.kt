package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BlockRecord
import com.example.data.CapabilityTier
import com.example.data.ChatMessageRecord
import com.example.data.CompanionCandidate
import com.example.data.ConnectionRecord
import com.example.data.DoveAiService
import com.example.data.DoveRepository
import com.example.data.DoveSeedCatalog
import com.example.data.LocalSafetyStore
import com.example.data.MessageRequestRecord
import com.example.data.PrivatePrefs
import com.example.data.ProfileDraftResult
import com.example.data.ProtectedPin
import com.example.data.PublicProfile
import com.example.data.SafeDateRecord
import com.example.data.SafetyReportRecord
import com.example.data.ScoredCandidate
import com.example.data.SwipeRecord
import com.example.data.UserAccount
import com.example.domain.CompatibilityEngine
import com.example.domain.SafetyEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class DiscoverFilters(
    val minAge: Int = 18,
    val maxAge: Int = 60,
    val maxDistKm: Int = 50,
    val lookingFor: String? = null,
    val verifiedOnly: Boolean = false
)

enum class OnboardingStep {
    AGE_SCREEN,
    TERMS_GATE,
    SAFETY_CONSENT,
    PROFILE_BUILDER,
    PHOTOS_AND_VERIFY,
    PRIVATE_COMPATIBILITY,
    PREFERENCES
}

data class OnboardingDraft(
    val dobYear: Int = 1998,
    val dobMonth: Int = 6,
    val dobDay: Int = 15,
    val computedAge: Int = 28,
    val termsAccepted: Boolean = false,
    val safetyConsent: Boolean = false,
    val browseOnly: Boolean = false,
    val phoneE164: String = "",
    val firstName: String = "",
    val gender: String = "woman",
    val interestedIn: List<String> = listOf("men", "women"),
    val areaName: String = "Kilimani, Nairobi",
    val lookingFor: String = "long_term",
    val bio: String = "",
    val promptQuestion: String = "My ideal Sunday",
    val promptAnswer: String = "",
    val interests: List<String> = listOf("Hiking", "Coffee"),
    val work: String = "",
    val selfieVerified: Boolean = false,
    val photoUris: List<String> = emptyList(),
    val prefAgeMin: Int = 21,
    val prefAgeMax: Int = 45,
    val distMaxKm: Int = 25,
    val verifiedOnly: Boolean = false
)

class DoveViewModel(
    private val repository: DoveRepository,
    private val aiService: DoveAiService,
    private val localSafetyStore: LocalSafetyStore,
    private val currentUserId: String,
    private val currentUserEmail: String
) : ViewModel() {

    companion object {
        private const val TAG = "DoveViewModel"
        private const val STOP_TIMEOUT_MS = 5000L
    }

    private fun <T> Flow<T>.toUiStateFlow(label: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                val rootError = fsErr ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }

    val under18BlockedUntilMs: StateFlow<Long> = localSafetyStore.under18BlockedUntilMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0L)

    val accountState: StateFlow<UiState<UserAccount?>> = repository.observeUserAccount()
        .toUiStateFlow("account")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val ownProfileState: StateFlow<UiState<PublicProfile?>> = repository.observeOwnPublicProfile()
        .toUiStateFlow("profile")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val privatePrefsState: StateFlow<PrivatePrefs?> = repository.observePrivatePrefs()
        .catch { if (it is CancellationException) throw it else Log.w(TAG, "privatePrefs err", it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val pinsState: StateFlow<List<ProtectedPin>> = repository.observePins()
        .catch { if (it is CancellationException) throw it else Log.w(TAG, "pins err", it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val swipesState: StateFlow<List<SwipeRecord>> = repository.observeSwipes()
        .catch { if (it is CancellationException) throw it else Log.w(TAG, "swipes err", it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val blocksState: StateFlow<List<BlockRecord>> = repository.observeBlocks()
        .catch { if (it is CancellationException) throw it else Log.w(TAG, "blocks err", it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val safeDatesState: StateFlow<List<SafeDateRecord>> = repository.observeSafeDates()
        .catch { if (it is CancellationException) throw it else Log.w(TAG, "safeDates err", it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val messageRequestsState: StateFlow<List<MessageRequestRecord>> = repository.observeMessageRequests()
        .catch { if (it is CancellationException) throw it else Log.w(TAG, "requests err", it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val connectionsState: StateFlow<List<ConnectionRecord>> = repository.observeConnections()
        .catch { if (it is CancellationException) throw it else Log.w(TAG, "connections err", it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val myReportsState: StateFlow<List<SafetyReportRecord>> = repository.observeMyReports()
        .catch { if (it is CancellationException) throw it else Log.w(TAG, "reports err", it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val remoteProfilesFlow: Flow<List<PublicProfile>> = repository.observePublicProfiles()
        .catch { if (it is CancellationException) throw it else emit(emptyList()) }

    private val _discoverFilters = MutableStateFlow(DiscoverFilters())
    val discoverFilters: StateFlow<DiscoverFilters> = _discoverFilters.asStateFlow()

    val scoredCandidates: StateFlow<List<ScoredCandidate>> = combine(
        ownProfileState,
        privatePrefsState,
        pinsState,
        blocksState,
        swipesState,
        remoteProfilesFlow
    ) { arr ->
        val ownState = arr[0] as UiState<PublicProfile?>
        val own = (ownState as? UiState.Success)?.data
            ?: return@combine emptyList<ScoredCandidate>()
        val priv = arr[1] as? PrivatePrefs
        @Suppress("UNCHECKED_CAST")
        val pins = arr[2] as List<ProtectedPin>
        @Suppress("UNCHECKED_CAST")
        val blocks = arr[3] as List<BlockRecord>
        @Suppress("UNCHECKED_CAST")
        val swipes = arr[4] as List<SwipeRecord>
        @Suppress("UNCHECKED_CAST")
        val remotes = arr[5] as List<PublicProfile>

        val blockedIds = blocks.map { it.blockedUid }.toSet()
        val swipedIds = swipes.map { it.targetId }.toSet()

        val combinedPool = buildList {
            addAll(DoveSeedCatalog.seedCompanions())
            remotes.filter { it.userId != currentUserId }.forEach { remote ->
                add(CompanionCandidate(profile = remote, photoRes = null, daysSinceActive = 0.5))
            }
        }

        combinedPool.filter { wrap ->
            CompatibilityEngine.passesBothWaysFilters(
                viewer = own,
                candidate = wrap.profile,
                viewerPins = pins,
                blockedIds = blockedIds,
                swipedIds = swipedIds,
                viewerPrivate = priv,
                candidatePrivate = wrap.privateAlignmentStub,
                forNearby = false
            )
        }.map { wrap ->
            CompatibilityEngine.scoreCandidate(own, priv, wrap)
        }.sortedByDescending { it.matchPercent }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val nearbyCandidates: StateFlow<List<ScoredCandidate>> = combine(
        ownProfileState,
        privatePrefsState,
        pinsState,
        blocksState,
        connectionsState,
        remoteProfilesFlow
    ) { arr ->
        val ownState = arr[0] as UiState<PublicProfile?>
        val own = (ownState as? UiState.Success)?.data
            ?: return@combine emptyList<ScoredCandidate>()
        val priv = arr[1] as? PrivatePrefs
        @Suppress("UNCHECKED_CAST")
        val pins = arr[2] as List<ProtectedPin>
        @Suppress("UNCHECKED_CAST")
        val blocks = arr[3] as List<BlockRecord>
        @Suppress("UNCHECKED_CAST")
        val conns = arr[4] as List<ConnectionRecord>
        @Suppress("UNCHECKED_CAST")
        val remotes = arr[5] as List<PublicProfile>

        val blockedIds = blocks.map { it.blockedUid }.toSet()
        val matchedIds = conns.flatMap { it.memberUids }.toSet()

        val combinedPool = buildList {
            addAll(DoveSeedCatalog.seedCompanions())
            remotes.filter { it.userId != currentUserId }.forEach { remote ->
                add(CompanionCandidate(profile = remote, photoRes = null, daysSinceActive = 0.5))
            }
        }

        combinedPool.filter { wrap ->
            wrap.profile.userId !in matchedIds &&
                CompatibilityEngine.passesBothWaysFilters(
                    viewer = own,
                    candidate = wrap.profile,
                    viewerPins = pins,
                    blockedIds = blockedIds,
                    swipedIds = emptySet(),
                    viewerPrivate = priv,
                    candidatePrivate = wrap.privateAlignmentStub,
                    forNearby = true
                )
        }.map { wrap ->
            CompatibilityEngine.scoreCandidate(own, priv, wrap)
        }.sortedBy {
            CompatibilityEngine.haversineKm(own.lat, own.lng, it.candidate.profile.lat, it.candidate.profile.lng)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    // Active chat thread messages
    private val _selectedConnectionId = MutableStateFlow<String?>(null)
    val selectedConnectionId: StateFlow<String?> = _selectedConnectionId.asStateFlow()

    val activeMessagesState: StateFlow<List<ChatMessageRecord>> = _selectedConnectionId
        .flatMapLatest { connId ->
            if (connId.isNullOrBlank()) flowOf(emptyList())
            else repository.observeMessages(connId).catch {
                if (it is CancellationException) throw it else emit(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    // Onboarding state
    private val _onboardingStep = MutableStateFlow(OnboardingStep.AGE_SCREEN)
    val onboardingStep: StateFlow<OnboardingStep> = _onboardingStep.asStateFlow()

    private val _onboardingDraft = MutableStateFlow(OnboardingDraft())
    val onboardingDraft: StateFlow<OnboardingDraft> = _onboardingDraft.asStateFlow()

    private val _aiBuilderLoading = MutableStateFlow(false)
    val aiBuilderLoading: StateFlow<Boolean> = _aiBuilderLoading.asStateFlow()

    private val _aiBuilderError = MutableStateFlow<String?>(null)
    val aiBuilderError: StateFlow<String?> = _aiBuilderError.asStateFlow()

    private val _aiClarifyHint = MutableStateFlow<String?>(null)
    val aiClarifyHint: StateFlow<String?> = _aiClarifyHint.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _victimGuidanceText = MutableStateFlow<String?>(null)
    val victimGuidanceText: StateFlow<String?> = _victimGuidanceText.asStateFlow()

    private val _victimGuidanceLoading = MutableStateFlow(false)
    val victimGuidanceLoading: StateFlow<Boolean> = _victimGuidanceLoading.asStateFlow()

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun showBanner(msg: String) {
        _statusBanner.value = msg
    }

    fun setOnboardingStep(step: OnboardingStep) {
        _onboardingStep.value = step
    }

    fun updateDraft(transform: (OnboardingDraft) -> OnboardingDraft) {
        _onboardingDraft.value = transform(_onboardingDraft.value)
    }

    fun submitAge(year: Int, month: Int, day: Int) {
        val currentYear = 2026
        val currentMonth = 11
        val currentDay = 20
        var age = currentYear - year
        if (month > currentMonth || (month == currentMonth && day > currentDay)) {
            age -= 1
        }
        if (age < 18) {
            viewModelScope.launch {
                localSafetyStore.recordUnder18Block(30L)
                _statusBanner.value = "Dove is strictly for adults 18 and older."
            }
        } else {
            _onboardingDraft.value = _onboardingDraft.value.copy(
                dobYear = year,
                dobMonth = month,
                dobDay = day,
                computedAge = age
            )
            _onboardingStep.value = OnboardingStep.TERMS_GATE
        }
    }

    fun acceptTermsAndGuidelines() {
        _onboardingDraft.value = _onboardingDraft.value.copy(termsAccepted = true)
        _onboardingStep.value = OnboardingStep.SAFETY_CONSENT
    }

    fun submitSafetyConsent(consentAccepted: Boolean, phoneE164: String) {
        val draft = _onboardingDraft.value.copy(
            safetyConsent = consentAccepted,
            browseOnly = !consentAccepted,
            phoneE164 = phoneE164.trim()
        )
        _onboardingDraft.value = draft
        viewModelScope.launch {
            val res = repository.createOrUpdateAccountInitial(
                email = currentUserEmail,
                age = draft.computedAge,
                dobYear = draft.dobYear,
                dobMonth = draft.dobMonth,
                dobDay = draft.dobDay,
                termsAccepted = true,
                safetyConsent = consentAccepted,
                browseOnly = !consentAccepted,
                phoneE164 = phoneE164.trim().takeIf { it.isNotBlank() }
            )
            if (res.isSuccess) {
                _onboardingStep.value = OnboardingStep.PROFILE_BUILDER
            } else {
                _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save consent"
            }
        }
    }

    fun runAiProfileExtraction(naturalText: String) {
        if (naturalText.isBlank()) {
            _aiBuilderError.value = "Describe yourself in a sentence or two first."
            return
        }
        viewModelScope.launch {
            _aiBuilderLoading.value = true
            _aiBuilderError.value = null
            _aiClarifyHint.value = null
            val res = aiService.extractProfileDraft(naturalText)
            _aiBuilderLoading.value = false
            res.onSuccess { draftResult: ProfileDraftResult ->
                val matchedArea = DoveSeedCatalog.areas.firstOrNull { area ->
                    draftResult.area != null && area.name.contains(draftResult.area, ignoreCase = true)
                }?.name ?: _onboardingDraft.value.areaName

                _onboardingDraft.value = _onboardingDraft.value.copy(
                    firstName = draftResult.firstName.ifBlank { _onboardingDraft.value.firstName },
                    gender = draftResult.gender ?: _onboardingDraft.value.gender,
                    interestedIn = draftResult.interestedIn.ifEmpty { _onboardingDraft.value.interestedIn },
                    areaName = matchedArea,
                    lookingFor = draftResult.lookingFor ?: _onboardingDraft.value.lookingFor,
                    bio = draftResult.bio.ifBlank { naturalText.take(300) },
                    interests = draftResult.interests.ifEmpty { _onboardingDraft.value.interests },
                    work = draftResult.work.ifBlank { _onboardingDraft.value.work }
                )
                _aiClarifyHint.value = draftResult.clarifyQuestion
            }.onFailure { err ->
                _aiBuilderError.value = err.localizedMessage ?: "AI extraction failed."
            }
        }
    }

    fun completeSelfieVerificationInOnboarding() {
        _onboardingDraft.value = _onboardingDraft.value.copy(selfieVerified = true)
        viewModelScope.launch {
            repository.updateUserFields(mapOf("selfieVerified" to true))
            _statusBanner.value = "Selfie verified! Established tier unlocked."
        }
    }

    fun completeSelfieVerificationFromMe(ownProfile: PublicProfile?) {
        viewModelScope.launch {
            repository.updateUserFields(mapOf("selfieVerified" to true, "suspensionUntilMs" to 0L))
            if (ownProfile != null) {
                repository.savePublicProfile(
                    ownProfile.copy(selfieVerified = true, trustScore = maxOf(ownProfile.trustScore, 88))
                )
            }
            _statusBanner.value = "Selfie check passed. Your Verified badge is active."
        }
    }

    fun savePrivateCompatibility(
        consented: Boolean,
        pace: String,
        paceDealbreaker: Boolean,
        exclusivity: String,
        exclusivityDealbreaker: Boolean,
        chemWeight: String,
        discretion: String,
        flirtLevel: String,
        place: String
    ) {
        viewModelScope.launch {
            val prefs = PrivatePrefs(
                userId = currentUserId,
                consented = consented,
                consentVersion = 1,
                pace = pace,
                paceDealbreaker = paceDealbreaker,
                exclusivity = exclusivity,
                exclusivityDealbreaker = exclusivityDealbreaker,
                chemWeight = chemWeight,
                discretion = discretion,
                flirtLevel = flirtLevel,
                place = place
            )
            val res = repository.savePrivatePrefs(prefs)
            if (res.isSuccess) {
                _statusBanner.value = "Private compatibility saved. Never visible to others."
            } else {
                _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Could not save private preferences"
            }
        }
    }

    fun deletePrivateCompatibility() {
        viewModelScope.launch {
            repository.deletePrivatePrefs()
            _statusBanner.value = "Private compatibility cleared."
        }
    }

    fun publishProfileFromOnboarding() {
        val d = _onboardingDraft.value
        val bioModeration = SafetyEngine.validatePublicText("${d.firstName} ${d.bio} ${d.promptAnswer}")
        if (bioModeration != null) {
            _statusBanner.value = bioModeration
            return
        }
        val area = DoveSeedCatalog.areas.firstOrNull { it.name == d.areaName } ?: DoveSeedCatalog.areas.first()
        val (snappedLat, snappedLng) = CompatibilityEngine.snapTo500mGrid(area.lat, area.lng)
        viewModelScope.launch {
            val profile = PublicProfile(
                userId = currentUserId,
                firstName = d.firstName.ifBlank { "Member" }.take(30),
                age = d.computedAge,
                gender = d.gender,
                interestedIn = d.interestedIn,
                area = area.name,
                lat = snappedLat,
                lng = snappedLng,
                lookingFor = d.lookingFor,
                bio = d.bio.ifBlank { "New to Dove and looking forward to real conversations." },
                promptQuestion = d.promptQuestion,
                promptAnswer = d.promptAnswer,
                interests = d.interests.take(12),
                languages = listOf("en"),
                work = d.work,
                selfieVerified = d.selfieVerified,
                trustScore = if (d.selfieVerified) 88 else 65,
                prefAgeMin = d.prefAgeMin,
                prefAgeMax = d.prefAgeMax,
                distMaxKm = d.distMaxKm,
                verifiedOnly = d.verifiedOnly,
                showInNearby = true,
                paused = false,
                incognito = false,
                status = "active"
            )
            val res1 = repository.savePublicProfile(profile)
            val res2 = repository.updateUserFields(mapOf("onboardingComplete" to true))
            if (res1.isSuccess && res2.isSuccess) {
                repository.seedSampleIncomingRequestsIfEmpty(profile.firstName)
                _statusBanner.value = "Welcome to Dove! Your profile is live."
            } else {
                _statusBanner.value = res1.exceptionOrNull()?.localizedMessage
                    ?: res2.exceptionOrNull()?.localizedMessage
                    ?: "Could not publish profile."
            }
        }
    }

    fun updateDiscoverFilters(filters: DiscoverFilters) {
        _discoverFilters.value = filters
    }

    fun swipeCandidate(
        scored: ScoredCandidate,
        action: String,
        note: String? = null,
        attachRose: Boolean = false
    ) {
        val account = (accountState.value as? UiState.Success)?.data ?: return
        val own = (ownProfileState.value as? UiState.Success)?.data ?: return
        val target = scored.candidate.profile

        if (!note.isNullOrBlank()) {
            val scan = SafetyEngine.scanMessage(note)
            if (scan.outcome == SafetyEngine.ScanOutcome.BLOCK_CONTACT_LEAK ||
                scan.outcome == SafetyEngine.ScanOutcome.HOLD_SCAM ||
                scan.outcome == SafetyEngine.ScanOutcome.REJECT_POLICY ||
                scan.outcome == SafetyEngine.ScanOutcome.ESCALATE_CHILD_SAFETY
            ) {
                _statusBanner.value = "Note blocked: ${scan.userCategory}"
                return
            }
        }

        if (attachRose && (!account.selfieVerified || account.rosesBalance <= 0 || account.rosesSentWeek >= 5)) {
            _statusBanner.value = "Roses require Selfie Verification and max 5 per rolling 7 days."
            return
        }

        viewModelScope.launch {
            repository.recordSwipe(
                targetId = target.userId,
                targetName = target.firstName,
                targetArea = target.area,
                action = action,
                note = note,
                roseAttached = attachRose
            )
            if (attachRose) {
                repository.updateUserFields(
                    mapOf(
                        "rosesBalance" to (account.rosesBalance - 1).coerceAtLeast(0),
                        "rosesSentWeek" to (account.rosesSentWeek + 1).coerceAtMost(5)
                    )
                )
            }

            if ((action == "like" || action == "like_note") && scored.candidate.likesUserBack) {
                val preview = note?.takeIf { it.isNotBlank() } ?: "Matched with ${target.firstName}!"
                val connRes = repository.createConnection(
                    partnerId = target.userId,
                    partnerName = target.firstName,
                    partnerArea = target.area,
                    partnerVerified = target.selfieVerified,
                    myName = own.firstName,
                    initialPreview = preview
                )
                if (connRes.isSuccess) {
                    _statusBanner.value = "It's a match with ${target.firstName}! Chat opened."
                }
            } else if (action == "like" || action == "like_note") {
                _statusBanner.value = "Added to Waiting. ${target.firstName} won't see 'seen' pressure."
            }
        }
    }

    fun sendOneMessageRequest(
        target: PublicProfile,
        body: String,
        attachRose: Boolean
    ) {
        val account = (accountState.value as? UiState.Success)?.data ?: return
        val own = (ownProfileState.value as? UiState.Success)?.data ?: return
        if (account.browseOnly) {
            _statusBanner.value = "Safety consent required before messaging. Enable in Me > Safety."
            return
        }
        val trimmed = body.trim()
        if (trimmed.isEmpty() || trimmed.length > 200) {
            _statusBanner.value = "Message request must be 1–200 characters."
            return
        }

        val scan = SafetyEngine.scanMessage(trimmed)
        viewModelScope.launch {
            when (scan.outcome) {
                SafetyEngine.ScanOutcome.ESCALATE_CHILD_SAFETY -> {
                    _statusBanner.value = "Blocked: Child safety policy violation."
                    return@launch
                }
                SafetyEngine.ScanOutcome.BLOCK_CONTACT_LEAK -> {
                    applyStrike(account, scan.strikeIncrement, "Contact details can't be shared in requests.")
                    repository.sendMessageRequest(
                        senderName = own.firstName,
                        senderArea = own.area,
                        senderVerified = account.selfieVerified,
                        receiverId = target.userId,
                        receiverName = target.firstName,
                        body = trimmed,
                        status = "blocked",
                        roseAttached = false,
                        sensitive = false,
                        effortReasons = emptyList(),
                        riskCategory = scan.userCategory
                    )
                    return@launch
                }
                SafetyEngine.ScanOutcome.HOLD_SCAM, SafetyEngine.ScanOutcome.REJECT_POLICY -> {
                    repository.sendMessageRequest(
                        senderName = own.firstName,
                        senderArea = own.area,
                        senderVerified = account.selfieVerified,
                        receiverId = target.userId,
                        receiverName = target.firstName,
                        body = trimmed,
                        status = "held",
                        roseAttached = false,
                        sensitive = false,
                        effortReasons = emptyList(),
                        riskCategory = scan.userCategory
                    )
                    _statusBanner.value = "Request held for safety review (${scan.userCategory})."
                    return@launch
                }
                else -> Unit
            }

            if (attachRose && (!account.selfieVerified || account.rosesBalance <= 0 || account.rosesSentWeek >= 5)) {
                _statusBanner.value = "Roses require Selfie Verification and max 5/week."
                return@launch
            }

            val reasons = buildList {
                if (account.selfieVerified) add("verified")
                add("note_on_prompt")
                if (attachRose) add("rose_sent")
            }
            val res = repository.sendMessageRequest(
                senderName = own.firstName,
                senderArea = own.area,
                senderVerified = account.selfieVerified,
                receiverId = target.userId,
                receiverName = target.firstName,
                body = trimmed,
                status = if (account.selfieVerified) "pending" else "filtered",
                roseAttached = attachRose,
                sensitive = scan.isSensitiveExplicit,
                effortReasons = reasons,
                riskCategory = null
            )
            if (res.isSuccess) {
                if (attachRose) {
                    repository.updateUserFields(
                        mapOf(
                            "rosesBalance" to (account.rosesBalance - 1).coerceAtLeast(0),
                            "rosesSentWeek" to (account.rosesSentWeek + 1).coerceAtMost(5)
                        )
                    )
                }
                _statusBanner.value = "One message request sent to ${target.firstName}."
            } else {
                _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to send request."
            }
        }
    }

    fun replyToRequestAndMatch(request: MessageRequestRecord, replyBody: String) {
        val own = (ownProfileState.value as? UiState.Success)?.data ?: return
        val scan = SafetyEngine.scanMessage(replyBody)
        if (scan.outcome == SafetyEngine.ScanOutcome.BLOCK_CONTACT_LEAK ||
            scan.outcome == SafetyEngine.ScanOutcome.HOLD_SCAM ||
            scan.outcome == SafetyEngine.ScanOutcome.REJECT_POLICY ||
            scan.outcome == SafetyEngine.ScanOutcome.ESCALATE_CHILD_SAFETY
        ) {
            _statusBanner.value = "Reply blocked: ${scan.userCategory}"
            return
        }
        viewModelScope.launch {
            val res = repository.acceptRequestAndOpenThread(request, replyBody.trim(), own.firstName)
            res.onSuccess { connId ->
                _selectedConnectionId.value = connId
                _statusBanner.value = "Matched with ${request.senderName}! Thread opened."
            }.onFailure {
                _statusBanner.value = it.localizedMessage ?: "Could not accept request."
            }
        }
    }

    fun ignoreRequest(requestId: String) {
        viewModelScope.launch {
            repository.ignoreMessageRequest(requestId)
            _statusBanner.value = "Request ignored silently."
        }
    }

    fun bulkIgnoreFiltered(requests: List<MessageRequestRecord>) {
        viewModelScope.launch {
            requests.take(20).forEach { repository.ignoreMessageRequest(it.id) }
            _statusBanner.value = "Cleared ${minOf(requests.size, 20)} filtered requests."
        }
    }

    fun bulkReportSpam(requests: List<MessageRequestRecord>) {
        viewModelScope.launch {
            requests.take(20).forEach { req ->
                repository.fileReport(
                    targetUid = req.senderId,
                    targetName = req.senderName,
                    category = "spam",
                    details = "Bulk spam report from Filtered Requests",
                    filedAfterBlock = false,
                    bulk = true,
                    evidenceSnippet = req.body
                )
                repository.ignoreMessageRequest(req.id)
            }
            _statusBanner.value = "Reported ${minOf(requests.size, 20)} requests as spam."
        }
    }

    fun climbWaitingLadder(swipe: SwipeRecord, rung: String, note: String? = null) {
        val account = (accountState.value as? UiState.Success)?.data ?: return
        if (rung in swipe.ladderUsed) {
            _statusBanner.value = "You already used this Interest Ladder step for ${swipe.targetName}."
            return
        }
        viewModelScope.launch {
            when (rung) {
                "rose" -> {
                    if (!account.selfieVerified || account.rosesBalance <= 0 || account.rosesSentWeek >= 5) {
                        _statusBanner.value = "Roses require Selfie Verification and max 5/week."
                        return@launch
                    }
                    repository.updateUserFields(
                        mapOf(
                            "rosesBalance" to (account.rosesBalance - 1).coerceAtLeast(0),
                            "rosesSentWeek" to (account.rosesSentWeek + 1).coerceAtMost(5)
                        )
                    )
                    repository.updateSwipeLadder(
                        targetId = swipe.targetId,
                        roseAttached = true,
                        ladderUsed = swipe.ladderUsed + "rose"
                    )
                    _statusBanner.value = "Rose attached for ${swipe.targetName}."
                }
                "note" -> {
                    val trimmed = note?.trim().orEmpty()
                    if (trimmed.isBlank()) return@launch
                    val scan = SafetyEngine.scanMessage(trimmed)
                    if (scan.outcome != SafetyEngine.ScanOutcome.ALLOW) {
                        _statusBanner.value = "Note blocked: ${scan.userCategory}"
                        return@launch
                    }
                    repository.updateSwipeLadder(
                        targetId = swipe.targetId,
                        note = trimmed,
                        ladderUsed = swipe.ladderUsed + "note"
                    )
                    _statusBanner.value = "Prompt note added for ${swipe.targetName}."
                }
                "reintroduce" -> {
                    repository.updateSwipeLadder(
                        targetId = swipe.targetId,
                        reintroduced = true,
                        ladderUsed = swipe.ladderUsed + "reintroduce"
                    )
                    _statusBanner.value = "Reintroduce queued once for ${swipe.targetName}."
                }
            }
        }
    }

    fun selectConnection(connectionId: String?) {
        _selectedConnectionId.value = connectionId
    }

    fun sendThreadMessage(connection: ConnectionRecord, rawText: String, attachRose: Boolean = false) {
        val account = (accountState.value as? UiState.Success)?.data ?: return
        if (account.browseOnly) {
            _statusBanner.value = "Enable Safety Consent in Me > Safety to send messages."
            return
        }
        if (account.suspensionUntilMs > System.currentTimeMillis()) {
            _statusBanner.value = "Chat is suspended: ${account.suspensionReason.ifBlank { "Safety policy violation" }}"
            return
        }
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return

        val currentMessages = activeMessagesState.value
        val scan = SafetyEngine.scanMessage(trimmed, currentMessages.size + 1)

        viewModelScope.launch {
            when (scan.outcome) {
                SafetyEngine.ScanOutcome.ESCALATE_CHILD_SAFETY -> {
                    applyStrike(account, 5, scan.userCategory)
                    repository.sendChatMessage(
                        connection = connection,
                        body = "[Message removed — child safety policy]",
                        status = "blocked",
                        reasonCode = scan.userCategory,
                        nudgeCategory = "child_safety",
                        nudgeBannerToSet = null,
                        roseAttached = false
                    )
                }
                SafetyEngine.ScanOutcome.BLOCK_CONTACT_LEAK -> {
                    applyStrike(account, scan.strikeIncrement, scan.userCategory)
                    repository.sendChatMessage(
                        connection = connection,
                        body = trimmed,
                        status = "blocked",
                        reasonCode = "To keep everyone safe, contact details can't be shared in chat.",
                        nudgeCategory = "contact_leak",
                        nudgeBannerToSet = scan.nudgeBanner,
                        roseAttached = false
                    )
                }
                SafetyEngine.ScanOutcome.HOLD_SCAM, SafetyEngine.ScanOutcome.REJECT_POLICY -> {
                    applyStrike(account, scan.strikeIncrement, scan.userCategory)
                    // Trigger Thread-Stage LLM (§D7.1b) only on trigger!
                    val recentBodies = currentMessages.takeLast(6).map { it.body } + trimmed
                    val stageRes = aiService.classifyThreadStage(recentBodies)
                    val stageNote = stageRes.getOrNull()?.second
                    val banner = stageNote ?: scan.nudgeBanner
                    repository.sendChatMessage(
                        connection = connection,
                        body = trimmed,
                        status = "held",
                        reasonCode = scan.userCategory,
                        nudgeCategory = "scam",
                        nudgeBannerToSet = banner,
                        roseAttached = false
                    )
                }
                SafetyEngine.ScanOutcome.SENSITIVE_BLUR -> {
                    repository.sendChatMessage(
                        connection = connection,
                        body = trimmed,
                        status = "sensitive",
                        reasonCode = "Sensitive message",
                        nudgeCategory = null,
                        nudgeBannerToSet = null,
                        roseAttached = attachRose
                    )
                }
                SafetyEngine.ScanOutcome.ALLOW -> {
                    if (attachRose && (!account.selfieVerified || account.rosesBalance <= 0 || account.rosesSentWeek >= 5)) {
                        _statusBanner.value = "Roses require Selfie Verification and max 5/week."
                        return@launch
                    }
                    if (attachRose) {
                        repository.updateUserFields(
                            mapOf(
                                "rosesBalance" to (account.rosesBalance - 1).coerceAtLeast(0),
                                "rosesSentWeek" to (account.rosesSentWeek + 1).coerceAtMost(5)
                            )
                        )
                    }
                    repository.sendChatMessage(
                        connection = connection,
                        body = trimmed,
                        status = "sent",
                        reasonCode = null,
                        nudgeCategory = null,
                        nudgeBannerToSet = null,
                        roseAttached = attachRose
                    )
                }
            }
        }
    }

    private suspend fun applyStrike(account: UserAccount, increment: Int, reason: String) {
        val nextStrikes = (account.strikes + increment).coerceAtMost(10)
        val suspDurationMs = when (nextStrikes) {
            2 -> 24L * 3_600_000L
            3 -> 72L * 3_600_000L
            4 -> 7L * 86_400_000L
            in 5..10 -> 30L * 86_400_000L
            else -> 0L
        }
        val suspUntil = if (suspDurationMs > 0L) System.currentTimeMillis() + suspDurationMs else 0L
        repository.updateUserFields(
            mapOf(
                "strikes" to nextStrikes,
                "suspensionUntilMs" to suspUntil,
                "suspensionReason" to reason.take(120)
            )
        )
        _statusBanner.value = if (suspUntil > 0L) {
            "Strike $nextStrikes: Chat paused ($reason)."
        } else {
            "Message blocked ($reason). Strike $nextStrikes recorded."
        }
    }

    fun appealStrike() {
        val account = (accountState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val next = (account.strikes - 1).coerceAtLeast(0)
            repository.updateUserFields(
                mapOf(
                    "strikes" to next,
                    "suspensionUntilMs" to 0L,
                    "suspensionReason" to ""
                )
            )
            _statusBanner.value = "Appeal logged & reviewed: strike restored."
        }
    }

    fun dismissThreadNudge(connectionId: String) {
        viewModelScope.launch {
            repository.dismissConnectionNudge(connectionId)
        }
    }

    fun addProtectedPin(label: String, areaName: String, radiusM: Int) {
        if (pinsState.value.size >= 5) {
            _statusBanner.value = "Maximum 5 Protected Places allowed."
            return
        }
        val area = DoveSeedCatalog.areas.firstOrNull { it.name == areaName } ?: DoveSeedCatalog.areas.first()
        val (lat, lng) = CompatibilityEngine.snapTo500mGrid(area.lat, area.lng)
        viewModelScope.launch {
            val res = repository.addPin(label, area.name, lat, lng, radiusM)
            if (res.isSuccess) {
                _statusBanner.value = "Protected place '$label' saved (${radiusM / 1000} km)."
            } else {
                _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save pin."
            }
        }
    }

    fun togglePauseAllPins(hours: Long) {
        viewModelScope.launch {
            repository.pauseAllPins(pinsState.value, hours)
            _statusBanner.value = if (hours > 0L) "Protected pins paused for ${hours}h." else "Protected pins resumed."
        }
    }

    fun deletePin(pinId: String) {
        viewModelScope.launch {
            repository.deletePin(pinId)
        }
    }

    fun blockUser(
        targetUid: String,
        targetName: String,
        category: String?,
        evidenceSnapshot: String?,
        connectionId: String?,
        alsoReport: Boolean,
        reportDetails: String = ""
    ) {
        viewModelScope.launch {
            repository.blockAndPreserveEvidence(targetUid, targetName, category, evidenceSnapshot, connectionId)
            if (alsoReport && !category.isNullOrBlank()) {
                repository.fileReport(
                    targetUid = targetUid,
                    targetName = targetName,
                    category = category,
                    details = reportDetails.ifBlank { "Reported during block flow" },
                    filedAfterBlock = false,
                    bulk = false,
                    evidenceSnippet = evidenceSnapshot
                )
            }
            if (connectionId != null && _selectedConnectionId.value == connectionId) {
                _selectedConnectionId.value = null
            }
            _statusBanner.value = if (alsoReport) {
                "$targetName blocked and reported. Evidence retained 90 days."
            } else {
                "$targetName blocked. Evidence retained 90 days."
            }
        }
    }

    fun reportAfterBlock(block: BlockRecord, category: String, details: String) {
        viewModelScope.launch {
            val res = repository.fileReport(
                targetUid = block.blockedUid,
                targetName = block.blockedName,
                category = category,
                details = details,
                filedAfterBlock = true,
                bulk = false,
                evidenceSnippet = block.evidenceSnapshot
            )
            if (res.isSuccess) {
                _statusBanner.value = "Thanks, we're reviewing your report on ${block.blockedName}."
            }
        }
    }

    fun reportConcern(category: String, details: String) {
        viewModelScope.launch {
            val res = repository.fileReport(
                targetUid = "platform_concern",
                targetName = "In-app safety concern",
                category = category,
                details = details,
                filedAfterBlock = false,
                bulk = false,
                evidenceSnippet = null
            )
            if (res.isSuccess) {
                _statusBanner.value = "Concern submitted to Dove Trust & Safety."
            }
        }
    }

    fun saveSafeDate(connectionId: String, partnerName: String, place: String, meetAtLabel: String, trustedContact: String) {
        viewModelScope.launch {
            val res = repository.saveSafeDate(connectionId, partnerName, place, meetAtLabel, trustedContact)
            if (res.isSuccess) {
                _statusBanner.value = "Safe Date check-in saved for $place."
            } else {
                _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save Safe Date."
            }
        }
    }

    fun checkInSafeDate(dateId: String, status: String) {
        viewModelScope.launch {
            repository.updateSafeDateStatus(dateId, status)
            _statusBanner.value = if (status == "panic") {
                "Panic alert logged. Use emergency contacts immediately if in danger."
            } else {
                "Checked in safe!"
            }
        }
    }

    fun loadVictimSupportGuidance(category: String) {
        viewModelScope.launch {
            _victimGuidanceLoading.value = true
            val res = aiService.generateVictimSupportSteps(category)
            _victimGuidanceLoading.value = false
            _victimGuidanceText.value = res.getOrElse {
                "• Stop replying and do not send any money or codes.\n• Take screenshots and keep this thread intact (Dove retains evidence for 90 days).\n• Tap Block & Report below immediately.\n• Contact verified local emergency or cybercrime support if threatened."
            }
        }
    }

    fun updateReachAndDiscoverySettings(
        whoCanMessageFirst: String? = null,
        showInNearby: Boolean? = null,
        paused: Boolean? = null,
        incognito: Boolean? = null,
        allowRoses: Boolean? = null,
        standoutsMuted: Boolean? = null,
        liteMode: Boolean? = null,
        verifiedOnly: Boolean? = null,
        distMaxKm: Int? = null
    ) {
        val own = (ownProfileState.value as? UiState.Success)?.data
        viewModelScope.launch {
            val userUpdates = mutableMapOf<String, Any>()
            if (whoCanMessageFirst != null) userUpdates["whoCanMessageFirst"] = whoCanMessageFirst
            if (allowRoses != null) userUpdates["allowRoses"] = allowRoses
            if (standoutsMuted != null) userUpdates["standoutsMuted"] = standoutsMuted
            if (liteMode != null) userUpdates["liteMode"] = liteMode
            if (userUpdates.isNotEmpty()) repository.updateUserFields(userUpdates)

            if (own != null) {
                val updatedProfile = own.copy(
                    showInNearby = showInNearby ?: own.showInNearby,
                    paused = paused ?: own.paused,
                    incognito = incognito ?: own.incognito,
                    verifiedOnly = verifiedOnly ?: own.verifiedOnly,
                    distMaxKm = distMaxKm ?: own.distMaxKm,
                    status = if (paused ?: own.paused) "paused" else "active"
                )
                repository.savePublicProfile(updatedProfile)
            }
        }
    }

    fun toggleBrowseOnlyToFullSafety() {
        viewModelScope.launch {
            repository.updateUserFields(mapOf("safetyConsent" to true, "browseOnly" to false))
            _statusBanner.value = "Safety consent accepted. Chat unlocked."
        }
    }

    fun deleteAccountAndSignOut(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteAccountData()
            if (res.isSuccess) {
                onDeleted()
            } else {
                _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Account deletion failed."
            }
        }
    }

    fun capabilityTier(account: UserAccount?): CapabilityTier =
        if (account?.selfieVerified == true) CapabilityTier.SELFIE_VERIFIED else CapabilityTier.UNVERIFIED
}
